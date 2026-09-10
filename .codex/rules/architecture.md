# Backend architecture

This repository does not yet have an implemented backend stack. Do not create
framework, API, database, or migration files until the relevant design is
approved.

## Future structure proposal

When implementation starts, prefer a structure that keeps domain behavior close
to its tests and keeps infrastructure replaceable:

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

The exact language, framework, migration tool, and test runner remain future
decisions. Once chosen, update this rule to match the real stack.

## Architectural rules

1. Domain rules live outside HTTP handlers and database migration files.
2. API handlers translate transport concerns and call domain/application logic.
3. Database access is explicit and testable; avoid hidden global state.
4. Cross-domain state changes, especially booking/payment/refund flows, need
   transactional boundaries documented before implementation.
5. New public API contracts require an approved plan before code is written.
6. Tests should sit close to the behavior they validate, with broader
   integration tests for cross-domain flows.
