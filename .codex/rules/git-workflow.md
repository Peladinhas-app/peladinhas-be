# Git workflow

Each coherent implementation step is staged and committed on the dedicated task
branch. A coherent step is independently understandable and testable; avoid
both one giant final commit and noisy commits for incomplete edits.

Each commit has exactly one concern. Never mix functional behavior with
refactoring, renaming, formatting, dependency maintenance, or unrelated cleanup.
Make behavior-preserving refactors and cleanup separate commits before or after
the functional change. Tests and documentation that directly prove or explain
that one concern belong with it; unrelated test or documentation cleanup does
not. Do not hide drive-by changes inside an otherwise valid commit.

Recent local commits are working history, not a declaration that the task is
final. Inspect them at the start of every turn and amend or follow them with an
improvement commit when new evidence requires it.

At task start, fetch `origin/main` and integrate it before implementation when
a remote `main` exists. At completion, fetch and integrate `origin/main` again,
then run the complete standard suite on a clean HEAD. Fix every failure,
including failures inherited from `main`. Push approval does not bypass these
checks.

Never push without user confirmation. Only the exact response `APPROVE PUSH`
authorizes one push attempt for the current repository, worktree, branch, and
HEAD.

Local hooks enforce worktree, branch, secret-scan, commit-message, and
push-approval checks. They remain an advisory boundary; remote branch
protection and required continuous integration checks are still required.

Commit and pull-request text is professional, impersonal, plain English. Never
address the reader directly, use emoji, or mention an assistant, model,
generator, or automated authorship. Branch names follow the same neutral rule
and use `<type>/<lowercase-task>` without tool or assistant prefixes. Both
message types use two levels of detail:

- Commit: `<type>(<scope>): <description>` under 72 characters, then a blank
  line, `Details:`, and no more than 100 words of necessary context.
- Pull request: a Conventional Commit title under 72 characters, then
  `## Summary` with no more than 60 words and `## Details` with no more than
  500 words covering relevant behavior, reason, risk, and validation.

Allowed types are `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `perf`,
`ci`, `build`, and `style`. Hooks enforce the mechanical structure. Review
enforces clarity and relevance. The pre-push hook rechecks the destination
branch and every new commit being transmitted.

Never push directly to `main`, force-push without `--force-with-lease`, or
delete the protected branch.
