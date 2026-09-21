# Authentication Architecture v0.1

This document records the approved V1 authentication foundation for the
Peladinhas backend.

## Responsibilities

Supabase Auth is responsible for account authentication. For V1, users sign up
and sign in with email and password through Supabase. Supabase issues the access
token used by clients.

Spring Security is responsible for validating incoming bearer tokens on the
backend. The backend acts as an OAuth2 resource server and validates JWT access
tokens using configuration supplied through environment variables or application
configuration.

Peladinhas domain services remain responsible for contextual authorization.
Group admin, match admin, pitch owner, and player permissions are not global
Spring roles. They continue to be checked against Peladinhas data.

## Identity Mapping

Peladinhas keeps `users.id` as the internal domain identifier. It is not the
Supabase user UUID.

The backend maps a validated JWT to a local user with:

- `users.auth_provider`
- `users.auth_subject`

For Supabase users:

- `auth_provider = supabase`
- `auth_subject = JWT subject`

The pair `(auth_provider, auth_subject)` is unique. Existing local development
rows may use `development` as the provider so they are not confused with real
Supabase identities.

## Profile Completion

The backend must not create a Peladinhas user during arbitrary authenticated API
requests.

The intended flow is:

1. Flutter signs up through Supabase Auth.
2. Flutter receives an authenticated Supabase session.
3. Flutter calls a protected Peladinhas profile creation/completion endpoint.
4. The backend derives provider and subject from the validated JWT.
5. The backend creates the local Peladinhas `UserEntity`.
6. Future requests resolve the same auth identity to that local user.

Email verification is required before public use. Password recovery is V1 scope
before public use, but it is not part of this first backend security slice.

## Temporary API Identity Fields

The current group and match REST API still accepts explicit identity fields such
as `creatorUserId`, `userId`, and `actingAdminUserId`.

These fields are temporary until the next API migration phase. At that point,
caller identities should come from the authenticated current user. Target
identifiers remain in paths or request bodies where appropriate, for example the
target participant in `/join-requests/{userId}/approve`.

## Configuration

Token validation configuration must not be hard-coded. Local, test, and future
deployment environments provide the relevant issuer or JSON Web Key Set
configuration through environment/application settings.

If token verification is not configured, authenticated API calls fail as
unauthenticated rather than accepting unverifiable tokens.
