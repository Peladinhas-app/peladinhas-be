-- Adds advanced constraints and indexes for Peladinhas V1.
-- Rollback expectation: rebuild disposable development/test databases from scratch.

create extension if not exists btree_gist;

alter table bookings
    add constraint ex_bookings_confirmed_pitch_time_no_overlap
    exclude using gist (
        pitch_id with =,
        tstzrange(starts_at, ends_at, '[)') with &&
    )
    where (status = 'confirmed');

alter table pitch_schedules
    add constraint ex_pitch_schedules_pitch_day_time_no_overlap
    exclude using gist (
        pitch_id with =,
        day_of_week with =,
        tsrange(
            date '2000-01-01' + starts_at,
            date '2000-01-01' + ends_at,
            '[)'
        ) with &&
    );

create index idx_group_members_user_status
    on group_members (user_id, status);

create index idx_matches_group_status_time
    on matches (group_id, status, starts_at, ends_at);

create index idx_match_participants_match_status
    on match_participants (match_id, status);

create index idx_bookings_match
    on bookings (match_id);

create index idx_bookings_pitch_status_time
    on bookings (pitch_id, status, starts_at, ends_at);

create index idx_pitch_blocks_pitch_time
    on pitch_blocks (pitch_id, starts_at, ends_at);

create index idx_payments_participant_status
    on payments (participant_id, status);

create index idx_refunds_payment_status
    on refunds (payment_id, status);

create index idx_chat_members_user
    on chat_members (user_id);

create index idx_messages_chat_created_at
    on messages (chat_id, created_at);

create index idx_notifications_user_read_created
    on notifications (user_id, read_at, created_at desc);
