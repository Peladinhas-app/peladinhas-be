# Database and persisted data

The V1 product source of truth is `../Documentation/REQUIREMENTS_V1.md`.
Do not duplicate its business rules here unless the repetition prevents a
technical mistake.

## Approval before implementation

Database design requires user approval before implementation. This includes
schema files, migrations, seed data, backend models, repository code, and any
change that writes or transforms persisted data.

Design documents may be drafted before approval. Implementation starts only
after the user approves the design.

## Migrations

Migrations must use English names and comments. Each migration must describe
the intended data change and rollback expectation in its review context.

Do not create a migration until the migration tool is chosen and documented.
Do not mix schema creation, data backfill, and unrelated cleanup in one
migration unless the coupling is required for correctness.

## Technical naming

Database tables, columns, indexes, constraints, internal enum values, API
fields, logs, and technical documentation use English. Do not create translated
technical field names.

## Open V1 decisions

Open requirements must not be hard-coded. This currently includes deadlines,
minimum-player confirmation formula, service-fee structure, automatic vacancy
timing, owner cancellation policy, payment provider, authentication provider,
notification implementation, and localization storage strategy.

If a design must represent an open decision, make the value configurable,
explicitly pending, or modeled as a future extension point.

## Localization-aware persisted data

Peladinhas-controlled user-facing data stored in the database must support
Portuguese and English where translation is appropriate. Proper names and
user-generated content must remain exactly as entered unless explicit
translations are supplied by the user.

Stable identifiers remain English. Localized labels or messages must be tied
to those stable identifiers rather than replacing them.

## Auditability

Payment, refund, cancellation, booking rejection, price recalculation, and
spot-status changes must be auditable. Designs must preserve who or what
caused the change, when it happened, the previous relevant state, and the new
state or financial amount.

Sensitive payment data must not be stored unless a future payment provider and
security review explicitly require and permit it.

## Booking and concurrency safety

Pitch-slot competition, provisional bookings, confirmations, cancellations,
and time-slot losses must be designed for concurrent requests. Designs must
state which database constraints, transactions, locks, idempotency keys, or
retries prevent double booking and duplicate financial effects.

## Secrets and environment

Secrets must not be committed. Use environment variables, ignored local files,
or a future secret manager. Template files may document required variables but
must contain placeholder values only.
