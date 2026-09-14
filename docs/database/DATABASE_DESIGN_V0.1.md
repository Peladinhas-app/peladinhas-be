# Database Design v0.1

This document describes the proposed PostgreSQL database model for Peladinhas
V1. It is a design document only. It is not a migration, schema file, backend
implementation, or final approval to implement the database.

The product source of truth is `../Documentation/REQUIREMENTS_V1.md`.

## Design principles

- Main entity identifiers use `UUID`.
- Timestamps use `TIMESTAMPTZ` where the value represents a real point in time.
- Money uses `NUMERIC(10,2)` for this conceptual version. Do not use `FLOAT`.
- Financial values keep `currency CHAR(3)` on financial records.
- Technical identifiers, table names, column names, enum-like values, logs,
  migration names, and technical documentation use English.
- Peladinhas-controlled user-facing content must support Portuguese and English
  where appropriate.
- Proper names and user-generated content are not automatically translated.
- User permissions are contextual. Do not duplicate global account roles.
- Historical booking and financial data must not be overwritten.
- Open V1 business decisions must remain configurable, pending, or explicitly
  documented.
- Payment-card data such as card numbers, CVV values, or raw card details must
  not be stored.

## Product rules represented by the model

- One account can act as a player, pitch owner, group admin, or match admin.
- Admin rights are contextual, not global account types.
- Groups persist after a match.
- Groups may have multiple admins.
- A member may later be promoted to admin.
- A group may be public or private.
- V1 allows one active/upcoming match per group.
- A user can create a match directly; Peladinhas creates a persistent public
  group around it.
- Public matches support `open_join` or `request_to_join`.
- A private group may expose only empty match vacancies publicly.
- An outside player can join that match without joining the private group.
- An outside player cannot access the private group chat.
- Payment is required to guarantee a match spot.
- Pitch price is divided among players.
- If fewer players remain at the formation deadline, an admin may cancel or
  continue.
- If the match continues, per-player price may be recalculated and top-up
  payments may be required.
- If a player cancels before the refund deadline, the spot can reopen and a
  refund may occur.
- If cancellation occurs too late, refund eligibility may differ.
- A booking begins as provisional.
- Multiple provisional bookings may target the same pitch and time.
- The first booking to satisfy final confirmation conditions wins.
- Competing provisional bookings become `lost`.
- Paid players affected by a lost booking receive full refunds according to V1.
- If a pitch owner rejects a paid booking, affected players receive full
  refunds including the service fee according to V1.

## Entity overview and cardinalities

- `users` 1 to many `pitches`.
- `users` many to many `groups` through `group_members`.
- `groups` 1 to many `matches`.
- `users` many to many `matches` through `match_participants`.
- `users` many to many `matches` through `match_admins`.
- `matches` 1 to many `bookings`.
- `pitches` 1 to many `bookings`.
- `pitches` 1 to many `pitch_images`.
- `pitches` 1 to many `pitch_schedules`.
- `pitches` 1 to many `pitch_blocks`.
- `matches` 1 to many `match_price_adjustments`.
- `bookings` 1 to many `booking_rejections`.
- `match_participants` 1 to many `payments`.
- `payments` 1 to many `refunds`.
- `users` many to many `chats` through `chat_members`.
- `chats` 1 to many `messages`.
- `users` 1 to many `notifications`.

## Tables

### users

Accounts are not split by global role. A user can participate in matches,
own pitches, administer groups, or administer matches depending on related
records.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main user identifier. |
| `email` | `VARCHAR` | yes | UNIQUE | Login/contact identifier. |
| `name` | `VARCHAR` | yes |  | Proper name; do not translate. |
| `preferred_language` | `VARCHAR` | yes |  | Values: `pt`, `en`. |
| `profile_image_url` | `TEXT` | no |  | External object URL/path/reference. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Creation timestamp. |
| `updated_at` | `TIMESTAMPTZ` | yes |  | Last update timestamp. |

Important constraints:

- `email` unique.
- `preferred_language IN ('pt', 'en')`.
- No `password_hash` in v0.1 because the authentication provider is TBD.

### groups

Groups are persistent organizing spaces. A group remains after a match ends.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main group identifier. |
| `name` | `VARCHAR` | yes |  | User-generated; do not translate. |
| `description` | `TEXT` | no |  | User-generated; do not translate. |
| `visibility` | `VARCHAR` | yes |  | Values: `public`, `private`. |
| `created_by_user_id` | `UUID` | yes | FK -> `users.id` | Historical creator only. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Creation timestamp. |
| `updated_at` | `TIMESTAMPTZ` | yes |  | Last update timestamp. |

Important constraints:

- `visibility IN ('public', 'private')`.
- `created_by_user_id` does not represent permanent admin rights.
- Direct match creation creates a persistent public group for that match.

### group_members

Group membership and group admin rights live here, not in `users`.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `group_id` | `UUID` | yes | PK, FK -> `groups.id` | Group membership context. |
| `user_id` | `UUID` | yes | PK, FK -> `users.id` | Member user. |
| `role` | `VARCHAR` | yes |  | Values: `member`, `admin`. |
| `status` | `VARCHAR` | yes |  | Values: `active`, `left`, `removed`. |
| `joined_at` | `TIMESTAMPTZ` | yes |  | Membership start timestamp. |
| `updated_at` | `TIMESTAMPTZ` | yes |  | Last membership update. |

Primary key:

- (`group_id`, `user_id`).

Important constraints:

- `role IN ('member', 'admin')`.
- `status IN ('active', 'left', 'removed')`.
- A member can later be promoted to `admin`.

### pitches

Pitches belong to users acting as pitch owners.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main pitch identifier. |
| `owner_user_id` | `UUID` | yes | FK -> `users.id` | Pitch owner context. |
| `name` | `VARCHAR` | yes |  | Proper/user-provided name; do not translate. |
| `description` | `TEXT` | no |  | User-generated; do not translate. |
| `address` | `TEXT` | yes |  | Human-readable location. |
| `latitude` | `DECIMAL` | no |  | Map/search coordinate. |
| `longitude` | `DECIMAL` | no |  | Map/search coordinate. |
| `timezone` | `VARCHAR` | yes |  | Expected V1 value: `Europe/Lisbon`. |
| `base_price` | `NUMERIC(10,2)` | no |  | Current/default advertised price. |
| `currency` | `CHAR(3)` | yes |  | Expected V1 value: `EUR`. |
| `is_active` | `BOOLEAN` | yes |  | Controls whether pitch is offered. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Creation timestamp. |
| `updated_at` | `TIMESTAMPTZ` | yes |  | Last update timestamp. |

Important constraints:

- `currency` should follow ISO 4217 three-letter currency codes.
- `base_price IS NULL OR base_price >= 0`.
- `latitude` and `longitude` should be either both present or both absent.
- If present, `latitude BETWEEN -90 AND 90`.
- If present, `longitude BETWEEN -180 AND 180`.

Design reasoning:

- `address` and coordinates serve different purposes. The address is readable;
  coordinates support maps, distance, and nearby search.
- `base_price` is not historical. Agreed booking price is stored on `bookings`.
- `timezone` and `currency` are not globally hard-coded in the model because
  future locations and currencies may need support.

### pitch_images

Stores references to pitch images. Binary image content is not stored in
PostgreSQL.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main image identifier. |
| `pitch_id` | `UUID` | yes | FK -> `pitches.id` | Owning pitch. |
| `image_url` | `TEXT` | yes |  | URL/path/reference to image storage. |
| `display_order` | `INTEGER` | yes |  | Ordering in pitch galleries. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Creation timestamp. |

Important constraints:

- `display_order >= 0`.
- Recommended unique constraint: (`pitch_id`, `display_order`).

### pitch_schedules

Recurring opening hours for a pitch.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main schedule identifier. |
| `pitch_id` | `UUID` | yes | FK -> `pitches.id` | Scheduled pitch. |
| `day_of_week` | `SMALLINT` | yes |  | 1 to 7. |
| `starts_at` | `TIME` | yes |  | Local opening time. |
| `ends_at` | `TIME` | yes |  | Local closing time. |

Important constraints:

- `day_of_week BETWEEN 1 AND 7`.
- `starts_at < ends_at`.
- Recommended validation should prevent overlapping recurring windows for the
  same pitch and day.

Design reasoning:

- Recurring schedule is separate from exception blocks so owners can express
  normal weekly availability and one-off closures independently.

### pitch_blocks

Exceptions to recurring availability.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main block identifier. |
| `pitch_id` | `UUID` | yes | FK -> `pitches.id` | Blocked pitch. |
| `starts_at` | `TIMESTAMPTZ` | yes |  | Block start. |
| `ends_at` | `TIMESTAMPTZ` | yes |  | Block end. |
| `reason_code` | `VARCHAR` | no |  | Stable English internal reason. |
| `note` | `TEXT` | no |  | Owner-entered note; do not auto-translate. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Creation timestamp. |

Important constraints:

- `starts_at < ends_at`.

Availability model:

```text
recurring pitch schedule
- pitch blocks
- confirmed bookings
= available times
```

### matches

Matches belong to a persistent group. Direct match creation creates a public
group first, then creates the match against that group.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main match identifier. |
| `group_id` | `UUID` | yes | FK -> `groups.id` | Organizing group. |
| `created_by_user_id` | `UUID` | yes | FK -> `users.id` | Historical creator. |
| `starts_at` | `TIMESTAMPTZ` | yes |  | Match start. |
| `ends_at` | `TIMESTAMPTZ` | yes |  | Match end. |
| `max_players` | `INTEGER` | yes |  | Intended player count. |
| `join_mode` | `VARCHAR` | yes |  | Values: `open_join`, `request_to_join`. |
| `status` | `VARCHAR` | yes |  | Values listed below. |
| `public_vacancies_enabled` | `BOOLEAN` | yes |  | Whether vacancies may be public. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Creation timestamp. |
| `updated_at` | `TIMESTAMPTZ` | yes |  | Last update timestamp. |

Proposed statuses:

- `draft`
- `recruiting`
- `ready`
- `completed`
- `cancelled`

Important constraints:

- `starts_at < ends_at`.
- `max_players > 0`.
- `join_mode IN ('open_join', 'request_to_join')`.
- `status IN ('draft', 'recruiting', 'ready', 'completed', 'cancelled')`.

Business constraints:

- Do not store `pitch_id` directly on `matches`; pitch choice and history live
  in `bookings`.
- V1 allows one active/upcoming match per group.
- Do not hard-code the one-active/upcoming-match rule as a database constraint
  until the exact active statuses are finalized.

Design reasoning:

- Keeping pitch selection in `bookings` allows a match to have multiple booking
  attempts over time without destroying history.
- `matches.starts_at` and `matches.ends_at` represent the match's current
  scheduled time.
- `bookings.starts_at` and `bookings.ends_at` preserve the slot associated with
  that specific booking attempt.
- This duplication is intentional: a match may lose or reject one booking
  attempt and later choose another pitch or time, while historical booking
  attempts must remain unchanged.

### match_admins

Current match-admin permissions live here.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `match_id` | `UUID` | yes | PK, FK -> `matches.id` | Match context. |
| `user_id` | `UUID` | yes | PK, FK -> `users.id` | Admin user. |
| `assigned_at` | `TIMESTAMPTZ` | yes |  | Assignment timestamp. |

Primary key:

- (`match_id`, `user_id`).

Design reasoning:

- `matches.created_by_user_id` records the historical creator.
- `match_admins` records current match-admin permissions.

### match_participants

Participant state is separate from financial records.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main participant identifier. |
| `match_id` | `UUID` | yes | FK -> `matches.id` | Match being joined. |
| `user_id` | `UUID` | yes | FK -> `users.id` | Participating user. |
| `status` | `VARCHAR` | yes |  | Values listed below. |
| `joined_at` | `TIMESTAMPTZ` | yes |  | First participation timestamp. |
| `confirmed_at` | `TIMESTAMPTZ` | no |  | Spot confirmation timestamp. |
| `cancelled_at` | `TIMESTAMPTZ` | no |  | Cancellation timestamp. |

Unique constraints:

- `UNIQUE(match_id, user_id)`.

Proposed statuses:

- `requested`
- `approved`
- `rejected`
- `awaiting_payment`
- `confirmed`
- `cancelled`

Important flow rules:

- Open Join: `awaiting_payment -> confirmed`.
- Request to Join: `requested -> approved -> awaiting_payment -> confirmed`.
- Do not use `refunded` as a participant status. Refunds are financial records.
- Payment is required to guarantee a match spot.

### bookings

Bookings represent pitch selection attempts for a match.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main booking identifier. |
| `match_id` | `UUID` | yes | FK -> `matches.id` | Related match. |
| `pitch_id` | `UUID` | yes | FK -> `pitches.id` | Requested pitch. |
| `starts_at` | `TIMESTAMPTZ` | yes |  | Booking start. |
| `ends_at` | `TIMESTAMPTZ` | yes |  | Booking end. |
| `total_price` | `NUMERIC(10,2)` | yes |  | Historical agreed price. |
| `currency` | `CHAR(3)` | yes |  | Currency for this booking. |
| `status` | `VARCHAR` | yes |  | Values listed below. |
| `confirmed_at` | `TIMESTAMPTZ` | no |  | Confirmation timestamp. |
| `rejected_at` | `TIMESTAMPTZ` | no |  | Owner rejection timestamp. |
| `cancelled_at` | `TIMESTAMPTZ` | no |  | Cancellation timestamp. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Creation timestamp. |
| `updated_at` | `TIMESTAMPTZ` | yes |  | Last update timestamp. |

Proposed statuses:

- `provisional`
- `confirmed`
- `lost`
- `rejected`
- `cancelled`

Important constraints:

- `starts_at < ends_at`.
- `total_price >= 0`.
- `currency` should follow ISO 4217 three-letter currency codes.

Business constraints:

- A match may have several booking attempts over its lifetime.
- Provisional bookings may overlap.
- Two confirmed bookings for the same pitch must never overlap.
- `total_price` preserves the historical price agreed for that booking even if
  `pitches.base_price` changes later.
- Paid players affected by `lost` or paid rejected bookings receive full
  refunds according to V1.

Concurrency rules:

- Confirmation must happen inside a database transaction.
- The implementation should use PostgreSQL-safe protection, such as an
  exclusion constraint over pitch and time range for confirmed bookings, or an
  equivalent locking strategy.
- The confirmation operation must be idempotent so retrying a request does not
  duplicate financial effects or state transitions.
- Competing provisional bookings that lose the slot should transition to
  `lost` in the same workflow that confirms the winning booking.

No SQL constraint is implemented in this document.

### booking_rejections

Records structured rejection history for bookings.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main rejection identifier. |
| `booking_id` | `UUID` | yes | FK -> `bookings.id` | Rejected booking. |
| `reason_code` | `VARCHAR` | yes |  | Stable English internal reason. |
| `explanation` | `TEXT` | no |  | Owner-provided explanation. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Rejection timestamp. |

Example reason codes:

- `maintenance`
- `scheduling_conflict`
- `private_event`
- `pitch_unavailable`
- `other`

Cardinality:

- v0.1 allows one booking to have multiple rejection records for audit history.
- If product rules later decide there can be only one rejection per booking,
  add `UNIQUE(booking_id)`.

Localization:

- Internal reason codes remain English.
- User-facing labels are localized, for example `maintenance` renders as
  Portuguese `Manutenção` and English `Maintenance`.

### match_price_adjustments

Records historical price recalculations instead of overwriting prior values.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main adjustment identifier. |
| `match_id` | `UUID` | yes | FK -> `matches.id` | Related match. |
| `old_player_count` | `INTEGER` | yes |  | Previous player count. |
| `new_player_count` | `INTEGER` | yes |  | New player count. |
| `old_price_per_player` | `NUMERIC(10,2)` | yes |  | Previous per-player amount. |
| `new_price_per_player` | `NUMERIC(10,2)` | yes |  | New per-player amount. |
| `currency` | `CHAR(3)` | yes |  | Currency of this price adjustment. |
| `created_by_user_id` | `UUID` | yes | FK -> `users.id` | Admin who made the change. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Adjustment timestamp. |

Important constraints:

- `old_player_count > 0`.
- `new_player_count > 0`.
- `old_price_per_player >= 0`.
- `new_price_per_player >= 0`.
- `currency` should follow ISO 4217 three-letter currency codes.

Business rules:

- If a match continues with fewer players after the formation deadline,
  per-player price may increase and top-up payments may be required.
- The exact top-up payment deadline is TBD.
- `currency` records the currency of the old and new per-player prices at the
  time of the adjustment.

### payments

Financial records are append-only audit history from a product perspective.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main payment identifier. |
| `participant_id` | `UUID` | yes | FK -> `match_participants.id` | Paying participant. |
| `type` | `VARCHAR` | yes |  | Values: `initial`, `top_up`. |
| `amount` | `NUMERIC(10,2)` | yes |  | Payment amount excluding service fee. |
| `service_fee` | `NUMERIC(10,2)` | yes |  | Service fee charged for this payment. |
| `currency` | `CHAR(3)` | yes |  | Currency for this payment. |
| `status` | `VARCHAR` | yes |  | Values listed below. |
| `provider` | `VARCHAR` | no |  | Payment provider name. |
| `provider_payment_id` | `VARCHAR` | no |  | Provider-side payment reference. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Creation timestamp. |
| `paid_at` | `TIMESTAMPTZ` | no |  | Successful payment timestamp. |

Proposed statuses:

- `pending`
- `succeeded`
- `failed`
- `cancelled`

Important constraints:

- `type IN ('initial', 'top_up')`.
- `amount >= 0`.
- `service_fee >= 0`.
- `currency` should follow ISO 4217 three-letter currency codes.
- `provider_payment_id` should be unique per provider when present.

Financial audit rules:

- Never overwrite an initial payment when a top-up occurs.
- Example: payment 1 is `initial`; payment 2 is `top_up`.
- `payments.amount` is the payment amount excluding `service_fee`.
- `payments.service_fee` is the service fee charged for that payment.
- Do not store card number, CVV, or raw card data.
- Stripe is expected later, but this model stays provider-neutral.
- The current working product rule says the service fee applies to the initial
  payment and not to later top-ups; the final fee structure remains TBD.

### refunds

Refunds are separate records. They do not erase or overwrite payments.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main refund identifier. |
| `payment_id` | `UUID` | yes | FK -> `payments.id` | Refunded payment. |
| `amount` | `NUMERIC(10,2)` | yes |  | Refunded amount. |
| `reason_code` | `VARCHAR` | yes |  | Stable English reason. |
| `status` | `VARCHAR` | yes |  | Values listed below. |
| `provider_refund_id` | `VARCHAR` | no |  | Provider-side refund reference. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Creation timestamp. |
| `processed_at` | `TIMESTAMPTZ` | no |  | Provider completion timestamp. |

Reason codes:

- `player_cancelled`
- `match_cancelled`
- `booking_rejected`
- `booking_lost`
- `other`

Statuses:

- `pending`
- `processed`
- `failed`

Important constraints:

- `amount > 0`.
- Total processed refunds for a payment must not exceed that payment's paid
  amount plus refundable service fee according to the final fee policy.
- `provider_refund_id` should be unique per provider when present.

Financial audit rules:

- Refunds preserve complete history.
- `refunds.amount` is the total monetary amount returned to the customer for
  that refund operation, including any refundable service-fee portion when
  applicable.
- Lost paid bookings and rejected paid bookings require full refunds according
  to V1.
- Refund eligibility for player cancellation depends on the refund deadline,
  which remains TBD.
- No extra refund amount columns are added in v0.1 because the exact
  service-fee refund policy remains TBD.

### chats

Chats attach to exactly one context: group, match, or booking.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main chat identifier. |
| `type` | `VARCHAR` | yes |  | Values: `group`, `match`, `booking`. |
| `group_id` | `UUID` | no | FK -> `groups.id` | Required for group chats. |
| `match_id` | `UUID` | no | FK -> `matches.id` | Required for match chats. |
| `booking_id` | `UUID` | no | FK -> `bookings.id` | Required for booking chats. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Creation timestamp. |

Conceptual CHECK constraint:

- `type = 'group'` requires `group_id` and no `match_id` or `booking_id`.
- `type = 'match'` requires `match_id` and no `group_id` or `booking_id`.
- `type = 'booking'` requires `booking_id` and no `group_id` or `match_id`.

Business rules:

- Group chat is persistent and restricted to group members.
- Match chat supports participants, including outside players who join public
  vacancies for private-group matches.
- Booking chat supports match admin and pitch owner communication.

### chat_members

Chat access is explicit and separate from group membership.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `chat_id` | `UUID` | yes | PK, FK -> `chats.id` | Chat context. |
| `user_id` | `UUID` | yes | PK, FK -> `users.id` | Chat member. |
| `joined_at` | `TIMESTAMPTZ` | yes |  | Chat access start. |

Primary key:

- (`chat_id`, `user_id`).

Business rule:

- A player joining a public vacancy in a private-group match can access the
  match chat, cannot access private group chat, and does not automatically
  become a group member.

### messages

Messages are user-generated content.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main message identifier. |
| `chat_id` | `UUID` | yes | FK -> `chats.id` | Owning chat. |
| `sender_user_id` | `UUID` | yes | FK -> `users.id` | Sending user. |
| `content` | `TEXT` | yes |  | User-generated message text. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Message creation time. |
| `edited_at` | `TIMESTAMPTZ` | no |  | Last edit time. |

Important constraints:

- `content` should not be empty after trimming.
- `edited_at IS NULL OR edited_at >= created_at`.

Localization:

- Message content must not be automatically translated.

### notifications

Notifications store stable technical types and optional structured payloads.

| Column | Type | Required | Key | Notes |
|---|---:|---:|---|---|
| `id` | `UUID` | yes | PK | Main notification identifier. |
| `user_id` | `UUID` | yes | FK -> `users.id` | Recipient user. |
| `type` | `VARCHAR` | yes |  | Stable English technical type. |
| `related_entity_type` | `VARCHAR` | no |  | Optional context type. |
| `related_entity_id` | `UUID` | no |  | Optional context identifier. |
| `payload` | `JSONB` | no |  | Structured non-secret context. |
| `created_at` | `TIMESTAMPTZ` | yes |  | Creation timestamp. |
| `read_at` | `TIMESTAMPTZ` | no |  | When the notification was read. |

Example types:

- `join_request_approved`
- `additional_payment_required`
- `booking_confirmed`
- `booking_lost`
- `refund_processed`
- `new_public_vacancy`

Important constraints:

- Use `read_at` rather than only `is_read` because it records whether and when
  the notification was read.
- `payload` must not contain secrets or sensitive payment-card data.

Localization:

- Store stable English technical types.
- The frontend renders Portuguese or English text according to the user's
  preferred language.

## Localization rules

- Technical identifiers remain English.
- User-facing system-controlled labels must support Portuguese and English.
- Stable code values should be stored once, then rendered through localization.
- Proper names and user-generated content must not be automatically translated.
- Examples of localized system-controlled data include booking rejection reason
  labels, cancellation reason labels, status labels, notification text, and
  system-generated explanatory content.
- The final strategy for storing localized system-controlled database content
  remains TBD. Options include frontend localization catalogs, database-backed
  localization tables, or a hybrid approach.

## Financial audit rules

- Payments and refunds are separate records.
- Initial payments are not overwritten by top-up payments.
- Refunds do not overwrite payments.
- Payment provider identifiers are stored as references only.
- Raw payment-card details must never be stored.
- Booking-lost and booking-rejected full-refund flows must be traceable from
  booking state through affected participants, payments, and refunds.
- Price recalculations are recorded in `match_price_adjustments`.
- Future implementation should consider idempotency keys for payment and refund
  operations, even though no table is proposed for them in v0.1.

## Pitch availability model

Availability is derived, not stored as a single final calendar table in v0.1.

```text
recurring pitch schedule
- pitch blocks
- confirmed bookings
= available times
```

Derived availability should account for:

- pitch timezone;
- recurring schedule windows;
- exception blocks;
- confirmed bookings;
- pitch active status;
- future search performance for nearby and available pitches.

## Booking concurrency rules

Critical invariant:

- Two confirmed bookings for the same pitch must never overlap.

Recommended implementation direction:

- Use database transactions for confirmation workflows.
- Protect confirmed booking overlap with PostgreSQL-safe constraints or locks.
- Keep provisional bookings allowed to overlap.
- Ensure booking confirmation is idempotent.
- Ensure payment and refund operations are idempotent.
- Transition losing provisional bookings to `lost` when a winning booking is
  confirmed.
- Trigger required full refunds for paid participants affected by `lost` or
  rejected paid bookings.

No SQL or migration is implemented in this design document.

## One active/upcoming match per group

V1 allows only one active/upcoming match per group.

This should be enforced once the exact set of active/upcoming statuses is
decided. Candidate statuses may include `draft`, `recruiting`, and `ready`, but
the final set is TBD.

Do not prematurely implement a database constraint until the status semantics
are finalized.

## Open/TBD decisions

- Authentication provider.
- Exact service fee.
- Whether service fee applies only to initial payment or also to top-ups.
- Refund deadline.
- Match formation deadline.
- Minimum-player formula based on match size.
- Top-up payment deadline.
- Exact automatic vacancy-opening behavior and timing.
- Pitch-owner cancellation penalties.
- Final payment provider details.
- Final notification delivery implementation.
- Whether `EUR` is permanently global or currency remains per financial record.
- Exact statuses that count as active/upcoming for the one-active-match-per-group rule.
- Final strategy for storing/translating localized database content.
- Whether payment amounts should later move from `NUMERIC(10,2)` to integer
  minor units after payment-provider selection.
- Whether booking rejections should be one per booking or many for history.
- Whether a dedicated idempotency table is needed for payment, refund, and
  booking-confirmation operations.

## Recommended improvements before implementation

1. Define status-transition diagrams for matches, participants, bookings,
   payments, and refunds before writing migrations.
2. Decide the localization storage strategy for system-controlled labels and
   messages.
3. Decide whether financial amounts should use integer minor units once the
   payment provider is confirmed.
4. Add an explicit idempotency strategy for external payment/refund callbacks
   and booking confirmation retries.
5. Decide the exact active/upcoming match statuses before enforcing the
   one-active/upcoming-match-per-group rule.
6. Define the standard migration tool and database test strategy before
   creating SQL.
