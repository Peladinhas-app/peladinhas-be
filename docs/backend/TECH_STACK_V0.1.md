# Backend Technical Stack v0.1

This document records the approved V1 backend stack for Peladinhas. It is
planning documentation only. It does not create project files, dependencies,
configuration, migrations, or backend implementation.

Current compatible stable versions must be selected when implementation begins.
Do not pin versions blindly in this document.

## Approved stack

| Technology | Role in Peladinhas | Why selected | Needed |
|---|---|---|---|
| Java | Main backend programming language. | Mature ecosystem, strong typing, good support for long-lived business systems, and excellent Spring Boot/PostgreSQL tooling. | Immediately |
| Spring Boot | Backend application framework. | Provides production-ready application structure, dependency injection, validation, data access integration, security integration, and testing support. | Immediately |
| Maven | Build and dependency management. | Standard Java build tool with predictable project structure and strong Spring Boot support. | Immediately |
| PostgreSQL | Primary relational database. | Fits the approved relational design, supports transactions, constraints, indexes, JSONB, time-aware data, and strong concurrency controls. | Immediately |
| Flyway | Database migration tool. | Keeps schema changes versioned, repeatable, reviewed, and tied to application history. | Immediately |
| Docker Compose | Local PostgreSQL runtime. | Gives developers a consistent local database without manually installing PostgreSQL. | Immediately |
| Spring Data JPA / Hibernate | Java persistence layer. | Reduces repetitive database access code while still allowing explicit transaction boundaries and custom queries where needed. | Immediately after migrations |
| Spring Validation | Request and command validation. | Centralizes validation rules for API inputs and service commands before business logic runs. | Immediately with API/service work |
| Testcontainers | Integration testing with real PostgreSQL containers. | Tests database behavior, migrations, constraints, and repository logic against PostgreSQL instead of an in-memory substitute. | Immediately with database tests |
| Spring Security | Authentication and authorization foundation. | Standard Spring security layer for authenticated users and contextual permissions. | Later |
| Stripe Java SDK | Payment provider integration. | Expected payment provider for V1 development using Stripe test mode; supports payment and refund workflows without storing card data. | Later |

## Component interaction

```text
Flutter frontend
  -> REST API controllers
  -> application/domain services
  -> Spring Data repositories / custom database access
  -> PostgreSQL

Flyway
  -> applies versioned migrations to PostgreSQL before application data access

Docker Compose
  -> provides local PostgreSQL for development

Testcontainers
  -> starts isolated PostgreSQL containers for integration tests

Spring Security
  -> later protects REST API endpoints and provides authenticated user context

Stripe Java SDK
  -> later handles payment/refund calls using Stripe test mode in development
```

## Immediate components

The first implementation phase needs:

- Java project baseline.
- Spring Boot application.
- Maven build.
- PostgreSQL driver.
- Docker Compose PostgreSQL service.
- Flyway configuration.
- Initial migrations based on the approved database design.
- Testcontainers setup for database/integration tests.

These are needed before API or business workflows because the project must
prove that migrations, constraints, and database access work reliably.

## Later components

Spring Security comes later because the authentication provider and final
authorization approach are still TBD. The codebase should still be structured
so contextual permissions can be added cleanly.

Stripe Java SDK comes later because the payment provider details and service
fee policy are still TBD. The database design already preserves provider
references and financial audit history without being Stripe-specific.

Chat and notification delivery details come later because the final
notification implementation remains TBD. Database and service boundaries should
not assume one delivery mechanism too early.

## Version selection

When implementation begins, choose current compatible stable versions for:

- Java long-term-support runtime.
- Spring Boot.
- Maven plugins.
- PostgreSQL Docker image.
- Flyway.
- Testcontainers.
- Stripe Java SDK when payment integration begins.

Version choices should be reviewed together because Spring Boot controls
compatible versions for many Spring dependencies.

## Stack fit with approved design

PostgreSQL and Flyway fit the approved design because bookings, payments,
refunds, and status transitions require strong transactional behavior,
constraints, and auditable migrations.

Spring Data JPA / Hibernate is suitable for ordinary entity persistence, but
booking confirmation and payment/refund workflows may need explicit
transactions, locking, custom queries, or database constraints. Convenience ORM
behavior must not hide concurrency-sensitive operations.

Testcontainers is important because the approved design depends on PostgreSQL
features and concurrency behavior that should not be approximated with an
in-memory database.
