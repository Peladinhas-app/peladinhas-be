# Proposed Backend and Database Structure

This is a planning structure only. Do not create source, schema, migration, or
runtime files until the backend stack and database design are approved.

## Documentation first

```text
docs/
  GOVERNANCE.md
  PROPOSED_STRUCTURE.md
  database-design.md
  api-design.md
```

`database-design.md` should be drafted and approved before migrations or
database access code exist.

## Future implementation layout

```text
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

## Design notes

- Domain modules should own business behavior and state transitions.
- Infrastructure should contain database and external-service adapters.
- HTTP interfaces should translate requests and responses, not own business
  rules.
- Payment, refund, booking, and cancellation flows need audit records and
  concurrency protection by design.
- Localized Peladinhas-controlled content should use stable English
  identifiers with Portuguese and English display values where appropriate.
- Open V1 decisions from `../Documentation/REQUIREMENTS_V1.md` must remain
  configurable, pending, or explicitly documented until decided.
