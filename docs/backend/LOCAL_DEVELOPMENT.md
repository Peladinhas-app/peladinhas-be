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
explicit local PostgreSQL connectivity test, start PostgreSQL first and opt in:

```powershell
$env:PELADINHAS_RUN_LOCAL_DB_TESTS = "true"
mvn test -Dtest=DatabaseConnectionTests
Remove-Item Env:\PELADINHAS_RUN_LOCAL_DB_TESTS
```

## Flyway migrations

Flyway owns database schema changes. Future migration files will live under
`src/main/resources/db/migration/` and will run automatically when the
application starts with the `local` profile and can connect to PostgreSQL.

This phase intentionally has no numbered migration files, so Flyway only
initializes its metadata table and records that no application migrations have
been applied. The local `fail-on-missing-locations: false` setting exists only
for this zero-migration phase and must be revisited when real migrations are
introduced. Do not manually edit the database schema and do not use Hibernate
or Java Persistence API schema generation for Peladinhas tables.

Production or hosted environments must use the same Flyway migration mechanism
with database connection values supplied by environment variables or a future
secret manager.

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
development data can be discarded.

```powershell
docker compose down -v
```
