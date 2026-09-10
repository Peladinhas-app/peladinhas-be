# peladinhas-be - mandatory rules

This repository is for the Peladinhas backend, database, and technical
documentation. Do not modify frontend repositories from this project.

## Rule loading

Before making changes, read and follow all applicable rules in:

- `.codex/rules/output-format.md`
- `.codex/rules/planning.md`
- `.codex/rules/worktree.md`
- `.codex/rules/architecture.md`
- `.codex/rules/database.md`
- `.codex/rules/git-workflow.md`
- `.codex/rules/coding-standards.md`

The V1 product source of truth is:

- `../Documentation/REQUIREMENTS_V1.md`

Governance explains process and safety rules. It must not duplicate product
requirements unless doing so prevents implementation mistakes.

## 1. Repository boundary

Work only inside `peladinhas-be` unless the user explicitly asks otherwise.
Frontend work belongs in frontend repositories.

## 2. Language rules

Technical names, database schema, backend code, API names, logs, migrations,
commit messages, and technical documentation use English.

User-facing content controlled by Peladinhas must support Portuguese and
English where appropriate. Proper names and user-generated content must not be
translated unless the user explicitly provides translations.

## 3. Contradictions

When instructions conflict in a way that changes what gets built, stop and ask.
State both readings and what each would produce. Do not silently choose one.

## 4. Doubts

Material uncertainty about behavior, data, architecture, security, persisted
state, or scope must be resolved before implementation. Do everything that does
not depend on the answer first, then ask once.

## 5. Planning before material changes

Before implementation, evaluate correctness, maintainability, scalability,
architecture, duplication, performance, and security where they apply.

Database schema, migrations, persisted data behavior, public API contracts,
new dependencies, new top-level architecture, and cross-cutting refactors need
an approved plan before implementation.

## 6. Database and backend safety

Database design must be approved before schema, migration, or backend code is
implemented. Open V1 business decisions from the requirements file must not be
hard-coded. Model them as configurable, documented, or intentionally pending.

Payments, refunds, booking confirmation, slot competition, and cancellation
flows must be auditable and concurrency-safe. Secrets must stay out of Git and
must be supplied through environment variables or a future secret manager.

## 7. Git and worktrees

Implementation work uses a linked Git worktree and a dedicated task branch.
The `main` branch is protected. Do not push without the exact approval phrase
`APPROVE PUSH`.

Bootstrap governance work is allowed in the main checkout only while the repo
has no initial commit and a linked worktree cannot yet be created. Future
implementation work must use a linked worktree.

## 8. Output format

All substantial responses must follow `.codex/rules/output-format.md`.
When repository work occurred, include branch, commits, push status, checks
run, and any known limitations.
