# Backend Implementation Plan v0.1

This document proposes the implementation order for the Peladinhas V1 backend.
It is planning documentation only. It does not create Java source files,
`pom.xml`, Docker files, SQL migrations, application configuration, API code,
or backend implementation.

Approved stack:

- Java
- Spring Boot
- Maven
- PostgreSQL
- Flyway
- Docker Compose
- Spring Data JPA / Hibernate
- Spring Validation
- Testcontainers
- Spring Security later
- Stripe Java SDK later, using Stripe test mode during development

Sources:

- `../Documentation/REQUIREMENTS_V1.md`
- `docs/database/DATABASE_DESIGN_V0.1.md`
- `docs/database/STATUS_TRANSITIONS_V0.1.md`

## Proposed package and folder structure

When implementation begins, use a package structure that keeps domain behavior
separate from HTTP and infrastructure concerns:

```text
src/main/java/com/peladinhas/backend/
  PeladinhasApplication.java
  config/
  domains/
    users/
      domain/
      application/
      persistence/
      web/
    pitches/
      domain/
      application/
      persistence/
      web/
    groups/
      domain/
      application/
      persistence/
      web/
    matches/
      domain/
      application/
      persistence/
      web/
    bookings/
      domain/
      application/
      persistence/
      web/
    payments/
      domain/
      application/
      persistence/
      web/
    chats/
      domain/
      application/
      persistence/
      web/
    notifications/
      domain/
      application/
      persistence/
      web/
  shared/
    domain/
    persistence/
    web/
    validation/
    time/
src/main/resources/
  db/migration/
src/test/java/com/peladinhas/backend/
  integration/
  support/
```

Structure notes:

- `domain` contains business concepts, status transitions, and domain rules.
- `application` contains use-case services and transaction boundaries.
- `persistence` contains JPA entities, repositories, and database-specific
  query code.
- `web` contains REST controllers, request objects, and response objects.
- `shared` contains cross-cutting code that is genuinely reusable.
- Keep booking/payment/refund workflows explicit; do not hide them behind
  generic CRUD services.
- Git does not track empty directories, so this structure should appear
  naturally as real Java code and resources are added. Do not create
  meaningless placeholder files only to force folders into Git.

## Phase 1: Bootstrap Spring Boot project

Create:

- Maven project files.
- Spring Boot application entry point.
- Basic test setup.
- Project metadata and standard commands.

Depends on:

- Approved stack document.

Test before moving forward:

- Project compiles.
- Empty Spring context test passes.
- Maven test command is documented and repeatable.

## Phase 2: Configure project structure

Create:

- Package structure under `com.peladinhas.backend`.
- Empty domain folders for the V1 areas.
- Shared test support location.
- Documentation updates if the actual structure differs from this plan.

Depends on:

- Phase 1 project bootstrap.

Test before moving forward:

- Build still passes.
- No unused generated examples or placeholder endpoints remain.

## Phase 3: Add Docker Compose PostgreSQL

Create:

- Local PostgreSQL service for development.
- Named local volume or documented reset workflow.
- Environment variable placeholders only, with no secrets committed.

Depends on:

- Phase 1 project baseline.

Test before moving forward:

- PostgreSQL starts locally.
- Connection details are documented.
- No real credentials are committed.

## Phase 4: Configure application/database connection

Create:

- Spring datasource configuration using environment variables.
- Local development profile.
- PostgreSQL driver dependency.

Depends on:

- Phase 3 local PostgreSQL.

Test before moving forward:

- Application can connect to local PostgreSQL.
- Application fails clearly when required environment variables are missing.
- No secrets are committed.

## Phase 5: Configure Flyway

Create:

- Flyway dependency and configuration.
- Migration folder under `src/main/resources/db/migration/`.
- Migration naming convention.

Depends on:

- Phase 4 database connectivity.

Test before moving forward:

- Flyway runs on application startup or test startup.
- Empty or baseline migration behavior is understood before real migrations.
- Migration command/failure behavior is documented.

## Phase 6: Configure Testcontainers PostgreSQL testing baseline

Create:

- Shared Testcontainers PostgreSQL test setup.
- Base integration-test configuration.
- A minimal database connectivity test using the same PostgreSQL engine family
  as local development.
- Documentation for running integration tests locally.

Depends on:

- Phase 4 database connectivity.
- Phase 5 Flyway setup.

Test before moving forward:

- Testcontainers can start PostgreSQL reliably.
- The test application context can connect to the containerized database.
- Flyway can run in the test context before any real schema migrations are
  added.
- Test failures are clear when Docker or the container runtime is unavailable.

## Phase 7: Create initial database migrations

Create:

- Initial schema migrations from `DATABASE_DESIGN_V0.1.md`.
- Enum-like values as stable English values.
- Tables, primary keys, foreign keys, required columns, and nullable columns.

Depends on:

- Phase 5 Flyway setup.
- Phase 6 Testcontainers baseline.
- Approved database design.
- Approved status-transition model.

Test before moving forward:

- Migrations apply from an empty database.
- Migrations can be rerun in a clean Testcontainers database.
- Schema matches the approved design.
- No open business rule is hard-coded beyond approved decisions.

## Phase 8: Add database constraints and indexes

Create:

- Unique constraints.
- Check constraints.
- Foreign-key indexes where needed.
- Booking overlap protection strategy for confirmed bookings.
- Indexes for match search, pitch availability, and user-facing queries.

Depends on:

- Phase 7 initial schema.

Test before moving forward:

- Constraint tests cover invalid values and invalid relationships.
- Confirmed booking overlap is prevented.
- Provisional booking overlap remains allowed.
- One-active/upcoming-match-per-group implementation is reviewed before being
  encoded.

## Phase 9: Add JPA entities and repositories

Create:

- JPA entities matching the approved schema.
- Repository interfaces or custom repositories.
- Explicit mappings for relationships.
- Enum-like Java types where useful, mapped to stable English database values.

Depends on:

- Phase 7 and Phase 8 database shape.

Test before moving forward:

- Repository tests pass with Testcontainers.
- Mappings do not create or mutate schema automatically.
- Hibernate validation does not conflict with Flyway-managed schema.

## Phase 10: Expand database/integration tests

Create:

- Migration tests.
- Repository tests for important constraints and relationships.
- Transaction/concurrency tests for booking confirmation behavior.
- Cross-entity database tests for payment/refund/booking audit behavior.

Depends on:

- Phase 6 Testcontainers baseline.
- Phase 9 repositories.

Test before moving forward:

- Test suite runs from a clean checkout.
- Database starts and stops reliably in tests.
- Constraint and migration failures are visible and actionable.
- Repository and transaction tests prove the most important persistence rules.

## Phase 11: Build backend domain/services

Create:

- Use-case services for users, pitches, groups, matches, bookings, payments,
  refunds, chats, and notifications.
- Status-transition enforcement from `STATUS_TRANSITIONS_V0.1.md`.
- Transaction boundaries for booking/payment/refund workflows.

Depends on:

- Phase 9 persistence.
- Phase 10 integration test coverage.

Test before moving forward:

- Unit tests cover domain status transitions.
- Integration tests cover cross-entity side effects.
- Booking confirmation remains concurrency-safe and idempotent.
- Financial audit history is preserved.

## Phase 12: Build REST API

Create:

- REST controllers.
- Request and response objects.
- Spring Validation annotations and validators.
- API error response conventions.

Depends on:

- Phase 11 services.

Test before moving forward:

- Controller tests cover validation and expected responses.
- API contracts match the Flutter frontend needs.
- No business logic lives only in controllers.

## Phase 13: Add authentication/authorization

Create:

- Spring Security configuration.
- Authentication integration once provider is decided.
- Contextual authorization rules for player, pitch owner, group admin, and
  match admin actions.

Depends on:

- Authentication provider decision.
- REST API shape from Phase 12.

Test before moving forward:

- Authenticated and unauthenticated access is tested.
- Contextual permission checks are covered.
- A user can act in multiple contexts without global-role duplication.

## Phase 14: Add Stripe test-mode integration

Create:

- Stripe Java SDK integration.
- Test-mode payment and refund flows.
- Webhook or provider callback handling if required.
- Idempotency strategy for provider interactions.

Depends on:

- Payment provider details.
- Payment/refund domain services.
- Secrets/environment handling.

Test before moving forward:

- Stripe test-mode payments and refunds are covered.
- No card data is stored.
- Successful payment confirms eligible participants.
- Refunds preserve financial audit history.
- Failed refund retry creates a new refund attempt.

## Phase 15: Add chats and notifications

Create:

- Chat service and REST API.
- Notification records and delivery integration when decided.
- Access rules for private group chat, match chat, and booking chat.

Depends on:

- Core match/group/booking flows.
- Notification delivery decision.
- Authentication and authorization.

Test before moving forward:

- Outside players in private-group public vacancies can access match chat but
  not private group chat.
- Booking chat includes the right match admin and pitch owner context.
- Notifications use stable English technical types and localized rendering
  remains frontend/application controlled.

## Phase 16: Connect Flutter frontend

Create:

- API contract alignment with the Flutter app.
- Development environment connection instructions.
- End-to-end smoke tests for core flows.

Depends on:

- REST API readiness.
- Authentication approach.
- Frontend integration planning.

Test before moving forward:

- Create/find/join match flow works in development.
- Pitch booking and payment test flows work with safe test data.
- Portuguese and English user-facing behavior is preserved.
- No frontend files are changed from backend tasks unless explicitly requested.

## Cross-cutting implementation rules

- Respect `DATABASE_DESIGN_V0.1.md` and `STATUS_TRANSITIONS_V0.1.md`.
- Do not hard-code open V1 decisions.
- Keep migrations reviewed and separate from unrelated changes.
- Keep payment/refund/booking operations auditable and idempotent.
- Use environment variables or a future secret manager for secrets.
- Do not store payment-card data.
- Keep technical names in English.
- Keep user-facing system content localization-aware.

## Known decisions still needed before implementation

- Java runtime version.
- Spring Boot version.
- PostgreSQL Docker image version.
- Flyway version if not managed by Spring Boot.
- Testcontainers version if not managed by Spring Boot.
- Exact package base name if `com.peladinhas.backend` is not desired.
- Authentication provider and token model.
- Stripe integration details and webhook strategy.
- Notification delivery provider.
- Final localization storage strategy for system-controlled database content.
