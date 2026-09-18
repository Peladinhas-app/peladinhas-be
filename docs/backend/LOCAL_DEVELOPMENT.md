# Local Development

This document describes local-only backend runtime commands. These settings are
for development on a workstation and are not production secrets or deployment
configuration.

## PostgreSQL with Docker Compose

The local PostgreSQL service is defined in `docker-compose.yml`.

Default local values:

| Setting | Default value |
|---|---|
| Database | `peladinhas_dev` |
| User | `peladinhas_dev` |
| Password | `peladinhas_dev_password` |
| Host | `127.0.0.1` |
| Port | `55432` |
| Docker image | `postgres:18.6` |

The service binds to `127.0.0.1` so PostgreSQL is available only from the local
machine by default. The default host port is `55432` to avoid colliding with a
PostgreSQL server that may already be using the standard local port `5432`.
For PostgreSQL 18 Docker images, the local data volume is mounted at
`/var/lib/postgresql` so the image can manage versioned data directories.

The defaults can be overridden with environment variables:

- `PELADINHAS_DB_HOST`
- `PELADINHAS_DB_NAME`
- `PELADINHAS_DB_USER`
- `PELADINHAS_DB_PASSWORD`
- `PELADINHAS_DB_PORT`

Do not commit local `.env` files. They are ignored by Git.

## Spring local database connection

Spring Boot reads local database settings from `src/main/resources/application-local.yml`
when the `local` profile is active.

Default local Spring connection values:

| Setting | Environment variable | Default value |
|---|---|---|
| Host | `PELADINHAS_DB_HOST` | `127.0.0.1` |
| Port | `PELADINHAS_DB_PORT` | `55432` |
| Database | `PELADINHAS_DB_NAME` | `peladinhas_dev` |
| User | `PELADINHAS_DB_USER` | `peladinhas_dev` |
| Password | `PELADINHAS_DB_PASSWORD` | `peladinhas_dev_password` |

The local defaults match the Docker Compose service. Hosted or production
database credentials must be supplied by environment variables or a future
secret manager. Production credentials must never be committed.

Run the application with the local profile after PostgreSQL is started:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Normal `mvn test` does not require PostgreSQL to be running. To run the
full automated test suite:

```powershell
mvn test
```

## Automated database tests

Automated database tests use Testcontainers, which starts a temporary
PostgreSQL container for the test run. These tests do not use the manual local
`peladinhas_dev` database, do not require port `55432`, and do not require a
developer-created database.

Docker must be available for these integration tests. Testcontainers supplies
the temporary database connection to Spring during the test run, and Flyway runs
against that temporary database. Developers should not point automated tests at
`peladinhas_dev`.

## Flyway migrations

Flyway owns database schema changes. Future migration files will live under
`src/main/resources/db/migration/` and will run automatically when the
application starts with the `local` profile and can connect to PostgreSQL.

Migration files are immutable once committed or applied. If a schema change is
needed after a migration has been shared, create a new migration instead of
editing the existing file. Flyway fails when the configured migration location
is missing, so accidental removal of migration resources is visible.

Do not manually edit the database schema and do not use Hibernate or Java
Persistence API schema generation for Peladinhas tables.

Production or hosted environments must use the same Flyway migration mechanism
with database connection values supplied by environment variables or a future
secret manager.

## JPA persistence mappings

Spring Data JPA and Hibernate map the Flyway-managed schema. Hibernate is
configured with `spring.jpa.hibernate.ddl-auto=validate`, so it validates the
database structure but must not create, update, or drop Peladinhas tables.

Persistence classes live under `com.peladinhas.backend.domains.*.persistence`.
Repository interfaces stay in the same domain persistence package as the entity
they access. Repositories should start with ordinary Spring Data behavior; add
custom queries only when a service phase needs them.

Composite primary keys use `@EmbeddedId` with `@MapsId` so join tables keep the
approved database keys while still exposing explicit entity relationships.
Entity relationships use conservative cascading. Historical booking and
financial rows should not disappear through parent-entity cascade deletes.

Stable database status and code values are represented by Java enums and stored
as their approved English string values, never enum ordinals.

`notifications.payload` is mapped as PostgreSQL `jsonb` to a Java
`Map<String, Object>` using Hibernate's JSON support.

## Service-layer conventions

Business services live outside persistence packages, under domain-specific
`service` packages such as `com.peladinhas.backend.domains.groups.service` and
`com.peladinhas.backend.domains.matches.service`. Persistence entities remain
database mappings; transactional workflows and status rules belong in services.

Service methods define transaction boundaries with `@Transactional`. Core match
creation locks the related group row with a PostgreSQL pessimistic write lock,
then checks for an existing active/upcoming match before inserting the new
match. This serializes competing match-creation transactions for the same
group and enforces the V1 one-active/upcoming-match rule without a time-based
database index.

Business logic uses an injectable Java `Clock` bean instead of scattering
direct current-time calls. Tests can replace the clock with a fixed value.

Authorization is currently contextual and explicit because authentication is
not implemented yet. Service methods receive acting user identifiers and check
group-admin or match-admin relationships in the database.

Deferred workflows remain intentionally out of the current service layer:
booking confirmation, competing booking loss handling, payment-provider calls,
refund workflows, deadlines, chat workflows, notification delivery, REST APIs,
and Spring Security.

## Start PostgreSQL

```powershell
docker compose up -d postgres
```

## Check PostgreSQL

```powershell
docker compose ps postgres
docker compose exec postgres pg_isready -U peladinhas_dev -d peladinhas_dev
```

## Stop PostgreSQL

```powershell
docker compose stop postgres
```

To stop and remove the container while keeping the database volume:

```powershell
docker compose down
```

## Reset the local database volume

This deletes the local PostgreSQL data volume. Use it only when local
development data can be discarded. During early schema development, this is the
safest way to rebuild the disposable local database from committed Flyway
migrations.

```powershell
docker compose down -v
```

## REST API conventions

The initial REST API lives under the versioned base path:

```text
http://localhost:8080/api/v1
```

HTTP controllers live in domain `web` packages such as
`com.peladinhas.backend.domains.groups.web` and
`com.peladinhas.backend.domains.matches.web`. Controllers should stay thin:
they validate transport input, translate request objects to service commands,
and translate service results to response objects. Business rules remain in the
service layer.

The first API phase exposes:

- `POST /api/v1/groups`
- `POST /api/v1/matches`
- `POST /api/v1/matches/direct`
- `GET /api/v1/matches/{matchId}`
- `POST /api/v1/matches/{matchId}/join`
- `POST /api/v1/matches/{matchId}/join-requests`
- `POST /api/v1/matches/{matchId}/join-requests/{userId}/approve`
- `POST /api/v1/matches/{matchId}/join-requests/{userId}/awaiting-payment`
- `POST /api/v1/matches/{matchId}/join-requests/{userId}/reject`

Request and response classes are explicit DTOs. JPA entities must not be
returned directly from controllers.

Authentication is not implemented yet. Until Spring Security and the final
authentication provider are introduced, API requests explicitly include user
identifiers such as `creatorUserId`, `userId`, and `actingAdminUserId`. These
fields are temporary development inputs and must be replaced by authenticated
user context in a later phase.

API errors use a stable JSON shape:

```json
{
  "code": "validation_failed",
  "message": "Request validation failed.",
  "timestamp": "2026-09-18T10:00:00Z",
  "fieldErrors": [
    {
      "field": "name",
      "message": "must not be blank"
    }
  ]
}
```

`fieldErrors` is an empty list for non-field-specific errors. Error responses
must not expose stack traces or internal exception details.

## Local CORS

The backend allows local development browser origins for `/api/v1/**` only:

- `http://localhost:5173`
- `http://127.0.0.1:5173`

For local Flutter Web development, start the frontend on the fixed allowed port:

```powershell
flutter run -d chrome --web-port 5173
```

This explicit allowlist is temporary local-development configuration. It is not
a production CORS policy, and production origins must be configured during a
future deployment/security phase.
