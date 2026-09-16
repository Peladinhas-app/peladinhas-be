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

- `PELADINHAS_DB_NAME`
- `PELADINHAS_DB_USER`
- `PELADINHAS_DB_PASSWORD`
- `PELADINHAS_DB_PORT`

Do not commit local `.env` files. They are ignored by Git.

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
