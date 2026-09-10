# Task worktrees

Every implementation task uses one dedicated linked Git worktree and one task branch.

Reading, investigation, planning, and user questions may happen before the final task branch is established.

Bootstrap governance work may happen in the main checkout only while the
repository has no initial commit and a linked worktree cannot yet be created.
After the initial governance commit exists, implementation work must use a
linked worktree.

A tool may initially create a temporary `codex/...` or `worktree-...` branch.
Before the first repository modification:

1. Determine the appropriate task type and concise task name.
2. Rename the temporary branch to `<type>/<lowercase-task>`.
3. Continue implementation only after the branch satisfies governance.

Supported task types are:
`feature`, `fix`, `hotfix`, `chore`, `docs`, `refactor`, `test`.

Never create a second worktree merely to replace an already-created task
worktree.
