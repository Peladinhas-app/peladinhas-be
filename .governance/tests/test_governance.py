"""Regression tests for repository governance hooks."""

from __future__ import annotations

import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GITHOOKS = ROOT / ".githooks"
GOVERNANCE = ROOT / ".governance"
MESSAGE_STRUCTURE_PATH = GITHOOKS / "message_structure.py"


def git(root: Path, *args: str, check: bool = True, env: dict[str, str] | None = None) -> subprocess.CompletedProcess[str]:
    """Run Git in a test repository."""
    result = subprocess.run(
        ["git", *args],
        cwd=root,
        capture_output=True,
        text=True,
        check=False,
        env=env,
    )
    if check and result.returncode:
        raise AssertionError(result.stderr or result.stdout)
    return result


def message_check(root: Path, *args: str) -> subprocess.CompletedProcess[str]:
    """Run the real message checker in a test repository."""
    return subprocess.run(
        [sys.executable, str(MESSAGE_STRUCTURE_PATH), *args],
        cwd=root,
        capture_output=True,
        text=True,
        check=False,
    )


def write_file(root: Path, relative: str, text: str) -> None:
    """Write a file inside a test repository."""
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def valid_message(subject: str) -> str:
    """Build a commit message accepted by the governance rules."""
    return f"{subject}\n\nDetails:\nAdd tested governance behavior."


def commit_file(root: Path, relative: str, text: str, message: str) -> str:
    """Create one commit and return its SHA."""
    write_file(root, relative, text)
    git(root, "add", relative)
    git(root, "-c", "core.hooksPath=", "commit", "-m", message)
    return git(root, "rev-parse", "HEAD").stdout.strip()


def init_repository(root: Path) -> None:
    """Create a standalone repository for hook tests."""
    git(root, "init", "-b", "main")
    git(root, "config", "user.email", "governance@example.local")
    git(root, "config", "user.name", "Governance Test")


def install_governance(root: Path) -> None:
    """Copy the real hooks and governance scripts into a test repository."""
    shutil.copytree(GITHOOKS, root / ".githooks", ignore=shutil.ignore_patterns("__pycache__"))
    shutil.copytree(GOVERNANCE, root / ".governance", ignore=shutil.ignore_patterns("__pycache__"))
    tests = root / ".governance" / "tests" / "test_governance.py"
    tests.write_text('print("stub governance tests pass")\n', encoding="utf-8")
    exclude = root / ".git" / "info" / "exclude"
    exclude.write_text(exclude.read_text(encoding="utf-8") + "\n.githooks/\n.governance/\n", encoding="utf-8")
    os.chmod(root / ".githooks" / "pre-push", 0o755)
    git(root, "config", "core.hooksPath", ".githooks")


def approve_main_bootstrap(root: Path) -> subprocess.CompletedProcess[str]:
    """Issue the empty-remote main bootstrap approval."""
    return subprocess.run(
        [
            sys.executable,
            str(root / ".governance" / "approval.py"),
            "issue-main-bootstrap",
            "APPROVE MAIN BOOTSTRAP",
        ],
        cwd=root,
        capture_output=True,
        text=True,
        check=False,
    )


def write_failing_bootstrap_check(root: Path) -> None:
    """Replace the copied governance test fixture with a failing check."""
    tests = root / ".governance" / "tests" / "test_governance.py"
    tests.write_text(
        "import sys\n"
        "print('forced bootstrap check failure')\n"
        "sys.exit(1)\n",
        encoding="utf-8",
    )


class MessageStructureBootstrapTests(unittest.TestCase):
    """Check bootstrap tip validation independently from push history checks."""

    def test_bootstrap_tip_checks_only_current_tip(self) -> None:
        """Legacy history can be grandfathered while the tip remains current."""
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            init_repository(root)
            commit_file(root, "legacy.txt", "legacy", "bad legacy")
            tip = commit_file(root, "tip.txt", "tip", valid_message("chore: prepare bootstrap"))

            self.assertEqual(message_check(root, "bootstrap-tip", tip).returncode, 0)
            push_check = message_check(root, "push-commits", tip, "0" * 40)
            self.assertNotEqual(push_check.returncode, 0)
            self.assertIn("title must match", push_check.stderr)

    def test_bootstrap_tip_rejects_invalid_current_tip(self) -> None:
        """The current bootstrap tip is never grandfathered."""
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            init_repository(root)
            tip = commit_file(root, "tip.txt", "tip", "bad current tip")

            tip_check = message_check(root, "bootstrap-tip", tip)

            self.assertNotEqual(tip_check.returncode, 0)
            self.assertIn("title must match", tip_check.stderr)


class MainBootstrapHookIntegrationTests(unittest.TestCase):
    """Exercise the real pre-push hook with a temporary bare remote."""

    def test_first_real_main_push_succeeds_and_second_is_rejected(self) -> None:
        """The approved first push creates main and later direct main pushes fail."""
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            work = base / "work"
            remote = base / "remote.git"
            work.mkdir()
            init_repository(work)
            legacy = commit_file(work, "legacy.txt", "legacy", "bad legacy")
            first = commit_file(work, "tip.txt", "tip", valid_message("chore: prepare bootstrap"))
            install_governance(work)
            remote.mkdir()
            git(remote, "init", "--bare")
            git(work, "remote", "add", "origin", str(remote))

            approval = approve_main_bootstrap(work)
            self.assertEqual(approval.returncode, 0, approval.stderr)
            push = git(work, "push", "--set-upstream", "origin", "main", check=False)

            self.assertEqual(push.returncode, 0, push.stderr)
            self.assertEqual(git(remote, "rev-parse", "refs/heads/main").stdout.strip(), first)
            self.assertIn(legacy, git(remote, "rev-list", "refs/heads/main").stdout)

            commit_file(work, "later.txt", "later", valid_message("chore: test later rejection"))
            second = git(work, "push", "origin", "main", check=False)

            self.assertNotEqual(second.returncode, 0)
            self.assertIn("direct pushes to main are forbidden", second.stderr)

    def test_pre_push_reruns_bootstrap_checks_before_authorizing(self) -> None:
        """A check failure after approval blocks the real hook push path."""
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            work = base / "work"
            remote = base / "remote.git"
            work.mkdir()
            init_repository(work)
            commit_file(work, "tip.txt", "tip", valid_message("chore: prepare bootstrap"))
            install_governance(work)
            remote.mkdir()
            git(remote, "init", "--bare")
            git(work, "remote", "add", "origin", str(remote))

            approval = approve_main_bootstrap(work)
            self.assertEqual(approval.returncode, 0, approval.stderr)
            write_failing_bootstrap_check(work)
            push = git(work, "push", "--set-upstream", "origin", "main", check=False)

            self.assertNotEqual(push.returncode, 0)
            self.assertIn("direct pushes to main are forbidden", push.stderr)
            self.assertEqual(git(remote, "show-ref", check=False).stdout.strip(), "")

    def test_invalid_current_tip_consumes_approval_before_failure(self) -> None:
        """A failed approved attempt consumes approval and cannot be retried."""
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            work = base / "work"
            remote = base / "remote.git"
            work.mkdir()
            init_repository(work)
            commit_file(work, "legacy.txt", "legacy", "bad legacy")
            commit_file(work, "tip.txt", "tip", "bad current tip")
            install_governance(work)
            remote.mkdir()
            git(remote, "init", "--bare")
            git(work, "remote", "add", "origin", str(remote))

            approval = approve_main_bootstrap(work)
            self.assertEqual(approval.returncode, 0, approval.stderr)
            push = git(work, "push", "--set-upstream", "origin", "main", check=False)
            retry = git(work, "push", "--set-upstream", "origin", "main", check=False)

            self.assertNotEqual(push.returncode, 0)
            self.assertIn("message-structure:", push.stderr)
            self.assertNotEqual(retry.returncode, 0)
            self.assertIn("direct pushes to main are forbidden", retry.stderr)
            self.assertEqual(git(remote, "show-ref", check=False).stdout.strip(), "")

    def test_tracked_modification_blocks_bootstrap_approval(self) -> None:
        """A tracked local change keeps main from being approved for bootstrap."""
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            work = base / "work"
            remote = base / "remote.git"
            work.mkdir()
            init_repository(work)
            commit_file(work, "tip.txt", "tip", valid_message("chore: prepare bootstrap"))
            install_governance(work)
            remote.mkdir()
            git(remote, "init", "--bare")
            git(work, "remote", "add", "origin", str(remote))
            write_file(work, "tip.txt", "dirty tracked change")

            approval = approve_main_bootstrap(work)

            self.assertNotEqual(approval.returncode, 0)
            self.assertIn("clean worktree", approval.stderr)

    def test_untracked_file_blocks_bootstrap_even_when_git_config_hides_it(self) -> None:
        """Explicit status flags reject untracked files regardless of user config."""
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            work = base / "work"
            remote = base / "remote.git"
            work.mkdir()
            init_repository(work)
            commit_file(work, "tip.txt", "tip", valid_message("chore: prepare bootstrap"))
            install_governance(work)
            remote.mkdir()
            git(remote, "init", "--bare")
            git(work, "remote", "add", "origin", str(remote))
            git(work, "config", "status.showUntrackedFiles", "no")
            write_file(work, "surprise.txt", "untracked")

            approval = approve_main_bootstrap(work)

            self.assertNotEqual(approval.returncode, 0)
            self.assertIn("clean worktree", approval.stderr)

    def test_ordinary_non_main_push_still_checks_every_commit_message(self) -> None:
        """The main bootstrap exception does not weaken normal branch checks."""
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            work = base / "work"
            remote = base / "remote.git"
            work.mkdir()
            init_repository(work)
            commit_file(work, "legacy.txt", "legacy", "bad legacy")
            commit_file(work, "tip.txt", "tip", valid_message("chore: prepare feature push"))
            install_governance(work)
            remote.mkdir()
            git(remote, "init", "--bare")
            git(work, "remote", "add", "origin", str(remote))
            git(work, "switch", "-c", "feature/test-push")
            env = os.environ.copy()
            env["PELADINHAS_PUSH_APPROVAL"] = "APPROVE PUSH"

            push = git(work, "push", "--set-upstream", "origin", "feature/test-push", check=False, env=env)

            self.assertNotEqual(push.returncode, 0)
            self.assertIn("message-structure:", push.stderr)


if __name__ == "__main__":
    unittest.main()
