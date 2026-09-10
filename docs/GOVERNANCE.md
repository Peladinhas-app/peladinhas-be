# Governance

Governance protects the Peladinhas backend and database work from accidental
schema changes, unsafe branch work, leaked secrets, and unapproved pushes.

## Active instruction files

- `AGENTS.md` is the entry point.
- `.codex/rules/output-format.md` defines response format.
- `.codex/rules/planning.md` defines planning, contradiction, and doubt rules.
- `.codex/rules/worktree.md` defines task worktree rules.
- `.codex/rules/architecture.md` defines backend architecture expectations.
- `.codex/rules/database.md` defines database and persisted-data safety rules.
- `.codex/rules/git-workflow.md` defines branch, commit, and push rules.
- `.codex/rules/coding-standards.md` defines coding standards once code exists.

The V1 product source of truth is `../Documentation/REQUIREMENTS_V1.md`.

## Hook status

Hooks live in `.githooks`, but `core.hooksPath` must not be enabled until the
scripts have been tested in this repository.

The intended hook behavior is:

- `pre-commit`: reject task commits on `main`, require linked worktrees after
  the initial commit, validate branch names, and run the staged secret scan.
- `commit-msg`: validate Conventional Commit message structure.
- `pre-push`: reject direct pushes to `main`, validate pushed branches and
  commit messages, and require an explicit approval environment value.
- `post-checkout` and `post-merge`: reserved no-op hooks.
- `post-commit`: print the latest local commit for quick verification.

## Protected branch

`main` is the protected branch. The previous copied assumptions about
`staging`, `master`, Git LFS, Trigger.dev, and external hook folders do not
apply to this repository.

## Bootstrap exception

This repository currently has no initial commit. A linked worktree cannot be
created until the repository has a real commit. Governance bootstrap may happen
in the main checkout only for that initial setup. Future implementation work
must use a linked worktree and task branch.

## Approval before database implementation

Database design must be approved before schema, migration, seed, backend model,
or data-writing implementation begins.

Design reviews must explicitly cover:

- English technical naming.
- Localization-aware persisted data.
- Open V1 decisions that must remain configurable or pending.
- Payment and refund auditability.
- Booking and pitch-slot concurrency safety.
- Secrets and environment handling.

## Future standard checks

The backend stack is not selected yet. Before implementation begins, define the
standard suite for the chosen stack, including tests, linting, formatting, type
checks when applicable, migration checks, and any container/database health
checks needed for reliable local development.
