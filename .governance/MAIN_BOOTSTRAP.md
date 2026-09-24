# Empty Remote Main Bootstrap

This document defines the only governance-approved path for creating
`origin/main` when the remote repository is genuinely empty.

## Approval

From a clean local `main` checkout, run:

```sh
python .governance/approval.py issue-main-bootstrap "APPROVE MAIN BOOTSTRAP"
```

The approval is valid for ten minutes and is bound to:

- this repository;
- local `main`;
- the exact current `main` commit;
- the configured remote;
- the remote push URL fingerprint.

The approval is stored in Git-private state and is consumed immediately by the
pre-push hook after authorization.

## Push

After approval, create remote `main` with:

```sh
git push --set-upstream origin main
```

The pre-push hook only allows this when all conditions remain true:

- exactly one ref update is pushed;
- the update creates `refs/heads/main` from the zero SHA;
- local `main` is still clean and still at the approved commit;
- the live remote still has no branch heads;
- the remote push URL fingerprint still matches the approval;
- the current tip commit message satisfies the current commit-message rules;
- secret scanning and non-message governance checks pass.

Earlier legacy commits are grandfathered during the first remote-main creation
so existing local history can be preserved without rewriting it. The bootstrap
tip is not grandfathered.

## Permanent Rule

After remote `main` exists, direct pushes to `main` remain forbidden. Normal
work continues on feature branches through the protected-branch workflow.
