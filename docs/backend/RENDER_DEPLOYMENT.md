# Render Deployment

This guide describes the initial public test deployment for the Peladinhas
Spring Boot backend on Render. It uses Supabase Auth for bearer-token
authentication and the Supabase hosted PostgreSQL session pooler for the
database.

Do not commit database passwords, Supabase tokens, signing keys, or service
role keys. Configure all deployment values in Render environment variables.

## Render Service

Use the tracked `render.yaml` blueprint or create an equivalent Render web
service manually.

Exact build/start configuration:

| Setting | Value |
|---|---|
| Runtime | Docker |
| Dockerfile path | `./Dockerfile` |
| Health check path | `/health` |
| Start command | Docker image `ENTRYPOINT`, `java -jar /app/peladinhas-backend.jar` |

Render supplies the `PORT` environment variable. The production Spring profile
uses it through `server.port=${PORT:8080}`.

## Required Environment Variables

Set these variables in Render before the first deploy:

| Variable | Purpose | Example placeholder |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Enables production configuration | `production` |
| `PELADINHAS_DB_HOST` | Supabase session pooler host | `<supabase-session-pooler-host>` |
| `PELADINHAS_DB_PORT` | Supabase session pooler port | `<supabase-session-pooler-port>` |
| `PELADINHAS_DB_NAME` | PostgreSQL database name | `<database-name>` |
| `PELADINHAS_DB_USER` | Supabase pooler database user | `<database-user>` |
| `PELADINHAS_DB_PASSWORD` | Supabase database password | `<database-password>` |
| `PELADINHAS_DB_SSL_MODE` | PostgreSQL JDBC SSL mode | `require` |
| `PELADINHAS_AUTH_PROVIDER` | Local auth-provider identifier | `supabase` |
| `PELADINHAS_AUTH_ISSUER_URI` | Supabase JWT issuer URI | `<supabase-auth-issuer-uri>` |
| `PELADINHAS_AUTH_JWK_SET_URI` | Supabase JSON Web Key Set URI | `<supabase-jwks-uri>` |
| `PELADINHAS_AUTH_AUDIENCE` | Expected JWT audience | `authenticated` |
| `PELADINHAS_CORS_ALLOWED_ORIGINS` | Browser origins allowed to call `/api/v1/**` | `<firebase-hosting-url>` |

`PELADINHAS_CORS_ALLOWED_ORIGINS` accepts a comma-separated list if more than
one public origin is needed.

## Supabase Connection Information

Use the Supabase PostgreSQL **session pooler** connection fields:

- Host: set as `PELADINHAS_DB_HOST`.
- Port: set as `PELADINHAS_DB_PORT`.
- Database: set as `PELADINHAS_DB_NAME`.
- User: set as `PELADINHAS_DB_USER`.
- Password: set as `PELADINHAS_DB_PASSWORD`.
- SSL mode: set `PELADINHAS_DB_SSL_MODE=require`.

The production JDBC URL is assembled by Spring as:

```text
jdbc:postgresql://${PELADINHAS_DB_HOST}:${PELADINHAS_DB_PORT}/${PELADINHAS_DB_NAME}?sslmode=${PELADINHAS_DB_SSL_MODE}
```

For Supabase Auth, set:

- `PELADINHAS_AUTH_ISSUER_URI` to the project issuer URI.
- `PELADINHAS_AUTH_JWK_SET_URI` to the project JSON Web Key Set URI.
- `PELADINHAS_AUTH_AUDIENCE` to `authenticated` for signed-in Supabase users.

The backend validates token signature, issuer, expiration, and audience. It
does not use a Supabase service-role key for normal API authentication.

## Database Migrations

Flyway runs automatically when the production application starts. The empty
hosted database must allow the configured database user to create the schema
objects required by:

- `V1__create_initial_schema.sql`
- `V2__add_database_constraints_and_indexes.sql`
- `V3__add_user_auth_identity.sql`
- `V4__add_match_funding_foundation.sql`

Hibernate remains validation-only with `spring.jpa.hibernate.ddl-auto=validate`.
Flyway owns schema creation and future schema changes.

## Manual Deployment Steps

1. Create or choose the Supabase project.
2. Confirm the Supabase hosted PostgreSQL session pooler connection fields.
3. Create a Render web service from this repository or apply `render.yaml`.
4. Set all required Render environment variables using real values in Render.
5. Set `PELADINHAS_CORS_ALLOWED_ORIGINS` to the Firebase Hosting URL when it is known.
6. Deploy the Render service.
7. Confirm `/health` returns `{"status":"ok", ...}`.
8. Confirm Flyway applied V1 through V4 in the hosted database.
9. Test an authenticated `/api/v1/**` request with a Supabase access token.

Do not deploy by committing local `.env` files or credentials.
