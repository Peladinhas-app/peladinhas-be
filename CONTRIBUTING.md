# Contributing

This repository contains the Peladinhas backend, database, and technical
documentation. Frontend changes belong in frontend repositories.

## Source of truth

The V1 product source of truth is `../Documentation/REQUIREMENTS_V1.md`.
Do not duplicate its business rules in governance or code unless needed to
prevent a technical mistake.

## Language

Use English for technical names, database schema, backend code, API names,
logs, migrations, commits, and technical documentation.

User-facing Peladinhas-controlled content must support Portuguese and English
where appropriate. Proper names and user-generated content remain as entered.

## Branches and worktrees

`main` is protected. After the initial governance bootstrap, implementation
work must happen in a linked worktree on a task branch:

```text
<type>/<lowercase-task>
```

Supported task types are `feature`, `fix`, `hotfix`, `chore`, `docs`,
`refactor`, and `test`.

## Commit messages

Use Conventional Commit titles:

```text
<type>(<scope>): <description>

Details:
<short explanation>
```

The title must be under 72 characters. The details section must stay concise
and explain the change in professional, impersonal English.

## Pull request messages

Use a Conventional Commit title under 72 characters and this body:

```text
## Summary
<brief outcome>

## Details
<reason, risk, and validation>
```

## Checks

Before considering work complete:

1. Fetch and integrate `origin/main` when it exists.
2. Run the complete standard test suite for the stack in use.
3. Run applicable lint, format, type, migration, and governance checks.
4. Confirm there are no unintended files or secrets staged.

The backend stack is not implemented yet, so the standard test suite must be
defined before backend or database implementation begins.

## Database and backend work

Database design must be approved before creating schema, migrations, seed data,
backend models, or data-writing code.

Open V1 decisions from the requirements must not be hard-coded. Designs for
payments, refunds, bookings, cancellations, and price recalculation must be
auditable and safe under concurrent requests.

## Proposed future structure

The following structure is a proposal for future work, not an implemented
stack:

```text
docs/
  database-design.md
  api-design.md
src/
  peladinhas/
    config/
    domains/
      users/
      pitches/
      groups/
      matches/
      bookings/
      payments/
      chat/
      notifications/
    infrastructure/
      database/
      external_services/
    interfaces/
      http/
tests/
  integration/
  contract/
migrations/
```

Update this guide once the backend language, framework, migration tool, test
runner, and local runtime are chosen.

## Pushes

Never push without explicit user confirmation. The approval phrase is exactly:

```text
APPROVE PUSH
```

The local pre-push hook also requires `PELADINHAS_PUSH_APPROVAL` to equal that
phrase for the push attempt.
