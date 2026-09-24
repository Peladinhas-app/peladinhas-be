"""Issue and verify tightly scoped approvals for sensitive Git operations."""

from __future__ import annotations

import hashlib
import json
import secrets
import subprocess
import sys
import time
from pathlib import Path

BOOTSTRAP_APPROVAL_PHRASE = "APPROVE MAIN BOOTSTRAP"
BOOTSTRAP_STATE_GIT_PATH = "governance/main-bootstrap-approval.json"
TOKEN_TTL_SECONDS = 600
ZERO_SHA = "0" * 40
PROTECTED_BRANCH = "main"
BOOTSTRAP_CHECKS = (
    (".githooks/secret_scan.py",),
    (".governance/tests/test_governance.py",),
)


def _git(*args: str, cwd: Path | None = None, check: bool = True) -> subprocess.CompletedProcess[str]:
    """Run Git and return text output for governance checks."""
    result = subprocess.run(
        ["git", *args],
        cwd=cwd,
        capture_output=True,
        text=True,
        check=False,
    )
    if check and result.returncode:
        detail = (result.stderr or result.stdout).strip()
        raise RuntimeError(detail or f"git {' '.join(args)} failed")
    return result


def repo_root() -> Path:
    """Return the current repository root."""
    return Path(_git("rev-parse", "--show-toplevel").stdout.strip())


def bootstrap_state_path(root: Path | None = None) -> Path:
    """Return the private Git path used for the one-time bootstrap approval."""
    repository = root or repo_root()
    return Path(_git("rev-parse", "--git-path", BOOTSTRAP_STATE_GIT_PATH, cwd=repository).stdout.strip())


def current_branch(root: Path) -> str:
    """Return the checked-out branch name."""
    return _git("branch", "--show-current", cwd=root).stdout.strip()


def current_head(root: Path) -> str:
    """Return the current commit SHA."""
    return _git("rev-parse", "HEAD", cwd=root).stdout.strip()


def local_main_head(root: Path) -> str:
    """Return the local main branch commit SHA."""
    return _git("rev-parse", PROTECTED_BRANCH, cwd=root).stdout.strip()


def clean_worktree(root: Path) -> bool:
    """Check whether tracked and untracked worktree state is clean."""
    return _git("status", "--porcelain=v1", "--untracked-files=all", cwd=root).stdout.strip() == ""


def remote_push_url(root: Path, remote: str) -> str:
    """Return the configured push URL or fetch URL for a remote."""
    push = _git("remote", "get-url", "--push", remote, cwd=root, check=False)
    if push.returncode == 0 and push.stdout.strip():
        return push.stdout.strip()
    fetch = _git("remote", "get-url", remote, cwd=root, check=True)
    return fetch.stdout.strip()


def remote_push_url_fingerprint(root: Path, remote: str) -> str:
    """Hash the push URL without exposing credentials."""
    return hashlib.sha256(remote_push_url(root, remote).encode("utf-8")).hexdigest()


def remote_branch_heads(root: Path, remote: str) -> list[str]:
    """Return live branch heads currently present on the remote."""
    result = _git("ls-remote", "--heads", remote, cwd=root, check=False)
    if result.returncode:
        raise RuntimeError("remote branch heads could not be inspected")
    return [line for line in result.stdout.splitlines() if line.strip()]


def remote_has_no_branch_heads(root: Path, remote: str) -> bool:
    """Check whether the remote has no branch heads at authorization time."""
    return not remote_branch_heads(root, remote)


def run_bootstrap_checks(root: Path) -> None:
    """Run deterministic non-message checks before issuing bootstrap approval."""
    for check in BOOTSTRAP_CHECKS:
        script = root / check[0]
        command = [sys.executable, str(script), *check[1:]]
        result = subprocess.run(command, cwd=root, text=True, check=False)
        if result.returncode:
            raise RuntimeError(f"{check[0]} failed")


def _read_state(path: Path) -> dict[str, object]:
    """Read an approval token from the private Git state path."""
    return json.loads(path.read_text(encoding="utf-8"))


def _write_state(path: Path, state: dict[str, object]) -> None:
    """Write an approval token to the private Git state path."""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(state, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def _remove_state(path: Path) -> None:
    """Consume a one-time approval token if it exists."""
    try:
        path.unlink()
    except FileNotFoundError:
        pass


def _not_expired(state: dict[str, object]) -> bool:
    """Check token lifetime."""
    issued_at = float(state.get("issued_at", 0))
    return time.time() - issued_at <= TOKEN_TTL_SECONDS


def _parse_ref_lines(refs_file: Path) -> list[tuple[str, str, str, str]]:
    """Parse pre-push stdin copied into a temporary file."""
    refs: list[tuple[str, str, str, str]] = []
    for line in refs_file.read_text(encoding="utf-8", errors="replace").splitlines():
        parts = line.split()
        if len(parts) == 4:
            refs.append((parts[0], parts[1], parts[2], parts[3]))
    return refs


def issue_main_bootstrap(phrase: str, remote: str = "origin") -> None:
    """Authorize exactly one first push that creates remote main."""
    if phrase != BOOTSTRAP_APPROVAL_PHRASE:
        raise RuntimeError("main bootstrap approval phrase did not match")
    root = repo_root()
    if current_branch(root) != PROTECTED_BRANCH:
        raise RuntimeError("main bootstrap approval must be issued from main")
    if current_head(root) != local_main_head(root):
        raise RuntimeError("main bootstrap approval must target local main HEAD")
    if not clean_worktree(root):
        raise RuntimeError("main bootstrap approval requires a clean worktree")
    if not remote_has_no_branch_heads(root, remote):
        raise RuntimeError("main bootstrap approval requires a genuinely empty remote")
    run_bootstrap_checks(root)
    state = {
        "action": "main-bootstrap",
        "branch": PROTECTED_BRANCH,
        "head": current_head(root),
        "issued_at": time.time(),
        "nonce": secrets.token_hex(16),
        "remote": remote,
        "remote_push_url_sha256": remote_push_url_fingerprint(root, remote),
        "repository_git_common_dir": _git("rev-parse", "--git-common-dir", cwd=root).stdout.strip(),
        "repository_toplevel": str(root),
    }
    _write_state(bootstrap_state_path(root), state)
    print(f"main bootstrap approved for {state['head']} for {TOKEN_TTL_SECONDS} seconds")


def check_main_bootstrap_refs(refs_file: Path, remote: str = "origin") -> None:
    """Validate a pre-push main creation against an issued bootstrap approval."""
    root = repo_root()
    state_path = bootstrap_state_path(root)
    if not state_path.exists():
        raise RuntimeError("main bootstrap approval was not found")
    state = _read_state(state_path)
    refs = _parse_ref_lines(refs_file)
    main_refs = [ref for ref in refs if ref[2] == "refs/heads/main"]
    if len(refs) != 1 or len(main_refs) != 1:
        raise RuntimeError("main bootstrap must push exactly one refs/heads/main update")
    local_ref, local_sha, remote_ref, remote_sha = main_refs[0]
    if local_ref != "refs/heads/main" or remote_ref != "refs/heads/main":
        raise RuntimeError("main bootstrap must create refs/heads/main")
    if remote_sha != ZERO_SHA:
        raise RuntimeError("main bootstrap only supports zero-SHA remote creation")
    if state.get("action") != "main-bootstrap":
        raise RuntimeError("approval token has the wrong action")
    if state.get("remote") != remote:
        raise RuntimeError("approval token remote does not match")
    if state.get("head") != local_sha:
        raise RuntimeError("approval token does not match the pushed commit")
    if state.get("branch") != PROTECTED_BRANCH or current_branch(root) != PROTECTED_BRANCH:
        raise RuntimeError("main bootstrap must run from local main")
    if state.get("head") != current_head(root) or state.get("head") != local_main_head(root):
        raise RuntimeError("local main HEAD changed after approval")
    if not _not_expired(state):
        raise RuntimeError("main bootstrap approval expired")
    if not clean_worktree(root):
        raise RuntimeError("main bootstrap requires a clean worktree")
    if state.get("remote_push_url_sha256") != remote_push_url_fingerprint(root, remote):
        raise RuntimeError("remote push URL changed after approval")
    if not remote_has_no_branch_heads(root, remote):
        raise RuntimeError("remote is no longer empty")
    run_bootstrap_checks(root)


def main() -> int:
    """Expose approval operations to Git hooks and maintainers."""
    command = sys.argv[1] if len(sys.argv) > 1 else ""
    try:
        if command == "issue-main-bootstrap" and len(sys.argv) in {3, 4}:
            remote = sys.argv[3] if len(sys.argv) == 4 else "origin"
            issue_main_bootstrap(sys.argv[2], remote)
        elif command == "check-main-bootstrap-refs" and len(sys.argv) in {3, 4}:
            remote = sys.argv[3] if len(sys.argv) == 4 else "origin"
            check_main_bootstrap_refs(Path(sys.argv[2]), remote)
        elif command == "consume-main-bootstrap" and len(sys.argv) == 2:
            _remove_state(bootstrap_state_path())
        else:
            print("approval: invalid command", file=sys.stderr)
            return 2
    except Exception as exc:  # noqa: BLE001 - hook output must stay concise.
        print(f"approval: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
