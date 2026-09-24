-- Adds provider-independent funding state for match booking confirmation.
-- Rollback expectation: rebuild disposable development/test databases from scratch.

alter table matches
    add column funding_mode varchar,
    add column funding_state varchar;

update matches
set funding_mode = 'split_payment',
    funding_state = 'collecting'
where funding_mode is null
   or funding_state is null;

alter table matches
    alter column funding_mode set not null,
    alter column funding_state set not null,
    add constraint chk_matches_funding_mode check (funding_mode in ('organizer_prepaid', 'split_payment')),
    add constraint chk_matches_funding_state check (
        funding_state in ('collecting', 'fully_funded', 'booking_confirmed', 'booking_failed', 'cancelled')
    );

create table match_funding_contributions (
    id uuid primary key,
    match_id uuid not null,
    booking_id uuid,
    actor_user_id uuid not null,
    amount numeric(10,2) not null,
    currency char(3) not null,
    purpose varchar not null,
    state varchar not null,
    external_provider varchar,
    external_payment_reference varchar,
    idempotency_key varchar,
    created_at timestamptz not null,
    settled_at timestamptz,
    constraint fk_match_funding_contributions_match foreign key (match_id) references matches (id),
    constraint fk_match_funding_contributions_booking foreign key (booking_id) references bookings (id),
    constraint fk_match_funding_contributions_actor foreign key (actor_user_id) references users (id),
    constraint chk_match_funding_contributions_amount check (amount >= 0),
    constraint chk_match_funding_contributions_currency check (currency ~ '^[A-Z]{3}$'),
    constraint chk_match_funding_contributions_purpose check (
        purpose in ('participant_share', 'organizer_advance', 'replacement_payment', 'reversal_refund_allocation')
    ),
    constraint chk_match_funding_contributions_state check (
        state in ('pending', 'settled', 'failed', 'cancelled', 'reversed')
    ),
    constraint chk_match_funding_contributions_settled_at check (
        (state = 'settled' and settled_at is not null)
        or (state <> 'settled')
    )
);

create unique index uq_match_funding_contributions_idempotency_key
    on match_funding_contributions (idempotency_key)
    where idempotency_key is not null;

create unique index uq_match_funding_contributions_external_reference
    on match_funding_contributions (external_provider, external_payment_reference)
    where external_provider is not null and external_payment_reference is not null;

create index idx_match_funding_contributions_match_state
    on match_funding_contributions (match_id, state);

create index idx_match_funding_contributions_booking_state
    on match_funding_contributions (booking_id, state)
    where booking_id is not null;
