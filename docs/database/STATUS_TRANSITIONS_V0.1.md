# Status Transitions v0.1

This document defines the proposed V1 status-transition model for the
database design. It is documentation only. It is not SQL, a migration, backend
code, or final implementation.

Sources:

- `../Documentation/REQUIREMENTS_V1.md`
- `docs/database/DATABASE_DESIGN_V0.1.md`

## General rules

- Status values are stable English technical identifiers.
- User-facing status labels must be localized by the application.
- Historical payment, refund, booking, and price data must not be overwritten.
- Refund eligibility is financial logic and must not be encoded as a
  participant status.
- State-changing workflows that affect bookings, payments, or refunds must be
  auditable, concurrency-safe, and idempotent where retries are possible.
- Open business rules from the V1 requirements are marked as TBD rather than
  silently decided here.

## Active / Upcoming Match Definition

V1 definition:

- Count `draft`, `recruiting`, and `ready` as active/upcoming for the
  one-active/upcoming-match-per-group rule when their scheduled end time is in
  the future.
- Do not count `completed` or `cancelled`.

Reasoning:

- `draft` can still reserve the group's current organizing slot before it is
  opened to players.
- `recruiting` and `ready` are clearly upcoming operational states.
- `completed` and `cancelled` are terminal historical states.

Do not implement the database constraint yet. The implementation mechanism must
be decided during the migration/schema phase.

## matches

Statuses:

- `draft`
- `recruiting`
- `ready`
- `completed`
- `cancelled`

Initial state:

- `draft`.

Terminal states:

- `completed`
- `cancelled`

Valid transitions:

| From | To | Trigger | Actor | Reversible | Side effects |
|---|---|---|---|---:|---|
| `draft` | `recruiting` | Match admin opens the match for players. | Match admin | no | Match becomes joinable or requestable according to `join_mode`. |
| `draft` | `cancelled` | Organizer abandons the draft. | Match admin | no | Any provisional booking attempt should be cancelled if one exists. |
| `recruiting` | `ready` | Required readiness conditions are satisfied. | System or match admin | TBD | May trigger pitch confirmation workflow when booking conditions are satisfied. |
| `recruiting` | `cancelled` | Admin cancels because the match will not happen. | Match admin | no | May cancel participants and trigger refunds for successful payments. |
| `ready` | `recruiting` | Match no longer satisfies readiness conditions before it takes place, for example because participant availability or payment conditions change. | System | yes | Match is no longer considered ready; related payment or participant workflows may continue. |
| `ready` | `completed` | Scheduled match has finished. | System or match admin | no | Historical booking and payment states remain unchanged. |
| `ready` | `cancelled` | Match is cancelled before it happens. | Match admin | no | May cancel participants, cancel/release booking, and trigger refunds. |

Important invalid transitions:

- `completed -> *` must not happen.
- `cancelled -> *` must not happen.
- `draft -> ready` must not bypass recruiting/readiness validation.
- `completed -> cancelled` must not happen.
- `ready -> recruiting` must not happen after the match is completed or
  cancelled.
- Match completion must not rewrite historical booking, payment, or refund
  records.

TBD business rules:

- Exact readiness conditions.
- Minimum-player formula based on match size.
- Formation deadline.
- Top-up rules that may affect match readiness.

## match_participants

Statuses:

- `requested`
- `approved`
- `rejected`
- `awaiting_payment`
- `confirmed`
- `cancelled`

Initial state:

- `awaiting_payment` for Open Join.
- `requested` for Request to Join.

Terminal states:

- None are permanently terminal for the row while the match is still accepting
  players.
- `rejected` and `cancelled` end one participation attempt, but V1 allows a new
  join attempt on the same row when product flow permits it.

Valid transitions:

| From | To | Trigger | Actor | Reversible | Side effects |
|---|---|---|---|---:|---|
| `requested` | `approved` | Match admin approves a join request. | Match admin | no | Participant may proceed to payment. |
| `requested` | `rejected` | Match admin rejects a join request. | Match admin | no | No payment should be required. |
| `approved` | `awaiting_payment` | Approved player is asked to pay. | System | no | Creates or expects a pending initial payment. |
| `awaiting_payment` | `confirmed` | Required payment succeeds. | System | no | Spot is guaranteed; may contribute to match/booking readiness. |
| `requested` | `cancelled` | Player withdraws request before decision. | Player | no | No refund workflow expected unless a future flow adds pre-approval payment. |
| `approved` | `cancelled` | Player withdraws before payment. | Player | no | Pending payment may be cancelled if one exists. |
| `awaiting_payment` | `cancelled` | Player/admin cancels before successful payment or payment deadline expires. | Player, match admin, or system | no | Pending payment may be cancelled. |
| `confirmed` | `cancelled` | Player or admin cancels a confirmed spot. | Player, match admin, or system | no | Refund eligibility is evaluated separately; spot may reopen according to match rules. |
| `rejected` | `requested` | User starts a new Request to Join attempt while the match accepts players. | User | no | Eligibility and capacity must be checked again; prior rejection remains historical context on the row. |
| `cancelled` | `requested` | User starts a new Request to Join attempt while the match accepts players. | User | no | Eligibility and capacity must be checked again; previous payments/refunds remain unchanged. |
| `cancelled` | `awaiting_payment` | User starts a new Open Join attempt while the match accepts players. | User | no | A new payment attempt must use a new payment record. |

Important invalid transitions:

- `rejected -> confirmed` must not happen.
- `cancelled -> confirmed` must not happen.
- `awaiting_payment -> confirmed` must not happen without successful payment.
- `confirmed -> awaiting_payment` must not happen for top-ups; top-ups are
  separate payment records.
- `refunded` must never be a participant status.

Re-entry rules:

- Re-entry represents a new join attempt by the user, not reversal of the prior
  rejection or cancellation.
- Re-entry is allowed only while the match is still accepting players.
- Capacity and other eligibility rules must be checked again.
- Previous payments and refunds must never be deleted or overwritten.
- A new payment attempt must use a new payment record.
- V1 keeps one `match_participants` row per user per match because
  `UNIQUE(match_id, user_id)` prevents multiple participant rows.
- This is an accepted V1 simplification. It loses some per-attempt history, so
  a future participation-attempt/history model may be introduced if needed.
- For v0.1, `joined_at` represents the first-ever participation timestamp for
  that user in that match. A future attempt-history model would be needed if
  the product must report each latest join attempt separately.

TBD business rules:

- Refund deadline.
- Top-up payment deadline.
- Whether an approved request can expire before payment.
- Rules for reopening spots after cancellation.
- Whether future analytics require per-attempt participant history beyond the
  single-row V1 simplification.

## bookings

Statuses:

- `provisional`
- `confirmed`
- `lost`
- `rejected`
- `cancelled`

Initial state:

- `provisional`.

Terminal states:

- `lost`
- `rejected`
- `cancelled`

`confirmed` is operationally stable but may later transition to `cancelled` if
the booking or match is cancelled.

Valid transitions:

| From | To | Trigger | Actor | Reversible | Side effects |
|---|---|---|---|---:|---|
| `provisional` | `confirmed` | Booking wins the pitch slot and satisfies final confirmation conditions. | System | no | Competing provisional bookings for the same pitch/time become `lost`. |
| `provisional` | `lost` | Another competing booking wins the pitch slot. | System | no | Successful payments affected by the lost slot require refunds. |
| `provisional` | `rejected` | Pitch owner rejects the booking. | Pitch owner | no | Paid affected participants require full refunds, including refundable service fee according to V1. |
| `provisional` | `cancelled` | Match admin abandons this booking attempt. | Match admin | no | May allow selecting another pitch/time. |
| `confirmed` | `cancelled` | Confirmed booking or match is cancelled. | Match admin, pitch owner, or system | no | May trigger participant cancellation and refund workflows. |

Important invalid transitions:

- `lost -> confirmed` must not happen.
- `rejected -> confirmed` must not happen.
- `cancelled -> confirmed` must not happen.
- `confirmed -> lost` must not happen; `lost` is for provisional bookings that
  fail competition.
- Two confirmed bookings for the same pitch must never overlap.
- Booking confirmation must not produce duplicate side effects when retried.

Concurrency and idempotency:

- Booking confirmation must be protected by database transactions and a
  PostgreSQL-safe overlap-prevention strategy during implementation.
- The winning confirmation operation must be idempotent.
- Losing provisional bookings should become `lost` in the same workflow that
  confirms the winning booking.

TBD business rules:

- Exact minimum-player formula for final pitch confirmation.
- Exact owner cancellation penalties.
- Whether owner cancellation of an already confirmed booking uses `cancelled`
  plus a separate audit reason, or another future status.
- Exact workflow for resuming/reopening a match after a lost booking.

## payments

Statuses:

- `pending`
- `succeeded`
- `failed`
- `cancelled`

Initial state:

- `pending`.

Terminal states:

- `succeeded`
- `failed`
- `cancelled`

Valid transitions:

| From | To | Trigger | Actor | Reversible | Side effects |
|---|---|---|---|---:|---|
| `pending` | `succeeded` | Payment provider confirms payment. | Payment provider callback or system verification | no | May move participant from `awaiting_payment` to `confirmed`. |
| `pending` | `failed` | Payment provider reports failure. | Payment provider callback or system verification | no | Participant remains unconfirmed; user may need a new payment attempt. |
| `pending` | `cancelled` | Payment is cancelled before success. | User, system, or provider | no | Participant may stay awaiting payment or move to cancelled depending on flow. |

Important invalid transitions:

- `succeeded -> failed` must not happen.
- `succeeded -> cancelled` must not happen.
- `failed -> succeeded` should not happen on the same row unless a provider
  explicitly supports late correction and the audit strategy is approved.
- `cancelled -> succeeded` must not happen.
- A refund must not rewrite a succeeded payment to failed or cancelled.
- A top-up must not overwrite an initial payment; it is a separate payment
  record.

TBD business rules:

- Final payment provider details.
- Exact service-fee amount and structure.
- Whether service fee applies only to initial payment or also to top-ups.
- Whether failed payment retry reuses the same row or creates a new payment
  row. Creating a new row is safer for auditability, but this is not finalized.

## refunds

Statuses:

- `pending`
- `processed`
- `failed`

Initial state:

- `pending`.

Terminal states:

- `processed`
- `failed`

`failed` is terminal for that refund record. Retrying a failed refund creates a
new refund record/attempt rather than changing the failed row back to
`pending`. This preserves financial audit history.

Valid transitions:

| From | To | Trigger | Actor | Reversible | Side effects |
|---|---|---|---|---:|---|
| `pending` | `processed` | Provider confirms refund completion. | Payment provider callback or system verification | no | User-facing refund notification may be sent. |
| `pending` | `failed` | Provider reports refund failure. | Payment provider callback or system verification | no | A retry creates a new refund record if another attempt is needed. |

Important invalid transitions:

- `processed -> pending` must not happen.
- `processed -> failed` must not happen.
- `failed -> pending` must not happen.
- `failed -> processed` must not happen.
- Refunds must not erase or overwrite the original payment.
- Refunds must not create participant status `refunded`.

TBD business rules:

- Exact service-fee refund policy.
- Payment-provider-specific refund behavior.
- Operational handling when refund processing fails repeatedly.
- Future schema may need a way to link refund retry attempts to an original
  failed refund record, but no new table or column is added in v0.1.

## Cross-entity side effects

- Successful initial payment may move `match_participants.awaiting_payment` to
  `confirmed`.
- Successful top-up payment should not change participant status by itself
  unless a future rule defines a separate delinquent state.
- Booking confirmation may move competing provisional bookings to `lost`.
- Booking `lost` or paid booking `rejected` may create refund records for
  affected successful payments.
- Match cancellation may require participant cancellation, booking
  cancellation, and refund workflows.
- Price recalculation may create `match_price_adjustments` and require new
  `top_up` payment records.
- Player cancellation may reopen a spot and may trigger refund evaluation.
- Match completion should not rewrite historical payment, booking, or refund
  states.

## Ambiguities and TBD decisions

- Exact active/upcoming statuses for one-active/upcoming-match-per-group.
- Exact match readiness conditions.
- Minimum-player formula based on match size.
- Formation deadline.
- Refund deadline.
- Top-up payment deadline.
- Automatic vacancy-opening behavior and timing.
- Pitch-owner cancellation penalties.
- Final payment provider details.
- Exact service-fee amount and structure.
- Whether service fee applies only to initial payments or also to top-ups.
- Whether failed payment retry reuses the same row or creates a new row.
- Whether owner cancellation of a confirmed booking needs a future distinct
  status beyond `cancelled`.
- Whether future schema should link refund retry attempts to original failed
  refund records.
