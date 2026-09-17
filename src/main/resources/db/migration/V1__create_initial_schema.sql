-- Creates the initial Peladinhas V1 relational schema.
-- Rollback expectation: rebuild disposable development/test databases from scratch.

create table users (
    id uuid primary key,
    email varchar not null,
    name varchar not null,
    preferred_language varchar not null,
    profile_image_url text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uq_users_email unique (email),
    constraint chk_users_preferred_language check (preferred_language in ('pt', 'en'))
);

create table groups (
    id uuid primary key,
    name varchar not null,
    description text,
    visibility varchar not null,
    created_by_user_id uuid not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_groups_created_by_user foreign key (created_by_user_id) references users (id),
    constraint chk_groups_visibility check (visibility in ('public', 'private'))
);

create table group_members (
    group_id uuid not null,
    user_id uuid not null,
    role varchar not null,
    status varchar not null,
    joined_at timestamptz not null,
    updated_at timestamptz not null,
    constraint pk_group_members primary key (group_id, user_id),
    constraint fk_group_members_group foreign key (group_id) references groups (id),
    constraint fk_group_members_user foreign key (user_id) references users (id),
    constraint chk_group_members_role check (role in ('member', 'admin')),
    constraint chk_group_members_status check (status in ('active', 'left', 'removed'))
);

create table pitches (
    id uuid primary key,
    owner_user_id uuid not null,
    name varchar not null,
    description text,
    address text not null,
    latitude decimal,
    longitude decimal,
    timezone varchar not null,
    base_price numeric(10,2),
    currency char(3) not null,
    is_active boolean not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_pitches_owner_user foreign key (owner_user_id) references users (id),
    constraint chk_pitches_base_price check (base_price is null or base_price >= 0),
    constraint chk_pitches_coordinate_pair check (
        (latitude is null and longitude is null)
        or (latitude is not null and longitude is not null)
    ),
    constraint chk_pitches_latitude check (latitude is null or latitude between -90 and 90),
    constraint chk_pitches_longitude check (longitude is null or longitude between -180 and 180),
    constraint chk_pitches_currency check (currency ~ '^[A-Z]{3}$')
);

create table pitch_images (
    id uuid primary key,
    pitch_id uuid not null,
    image_url text not null,
    display_order integer not null,
    created_at timestamptz not null,
    constraint fk_pitch_images_pitch foreign key (pitch_id) references pitches (id),
    constraint uq_pitch_images_display_order unique (pitch_id, display_order),
    constraint chk_pitch_images_display_order check (display_order >= 0)
);

create table pitch_schedules (
    id uuid primary key,
    pitch_id uuid not null,
    day_of_week smallint not null,
    starts_at time not null,
    ends_at time not null,
    constraint fk_pitch_schedules_pitch foreign key (pitch_id) references pitches (id),
    constraint chk_pitch_schedules_day_of_week check (day_of_week between 1 and 7),
    constraint chk_pitch_schedules_time_range check (starts_at < ends_at)
);

create table pitch_blocks (
    id uuid primary key,
    pitch_id uuid not null,
    starts_at timestamptz not null,
    ends_at timestamptz not null,
    reason_code varchar,
    note text,
    created_at timestamptz not null,
    constraint fk_pitch_blocks_pitch foreign key (pitch_id) references pitches (id),
    constraint chk_pitch_blocks_time_range check (starts_at < ends_at)
);

create table matches (
    id uuid primary key,
    group_id uuid not null,
    created_by_user_id uuid not null,
    starts_at timestamptz not null,
    ends_at timestamptz not null,
    max_players integer not null,
    join_mode varchar not null,
    status varchar not null,
    public_vacancies_enabled boolean not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_matches_group foreign key (group_id) references groups (id),
    constraint fk_matches_created_by_user foreign key (created_by_user_id) references users (id),
    constraint chk_matches_time_range check (starts_at < ends_at),
    constraint chk_matches_max_players check (max_players > 0),
    constraint chk_matches_join_mode check (join_mode in ('open_join', 'request_to_join')),
    constraint chk_matches_status check (status in ('draft', 'recruiting', 'ready', 'completed', 'cancelled'))
);

create table match_admins (
    match_id uuid not null,
    user_id uuid not null,
    assigned_at timestamptz not null,
    constraint pk_match_admins primary key (match_id, user_id),
    constraint fk_match_admins_match foreign key (match_id) references matches (id),
    constraint fk_match_admins_user foreign key (user_id) references users (id)
);

create table match_participants (
    id uuid primary key,
    match_id uuid not null,
    user_id uuid not null,
    status varchar not null,
    joined_at timestamptz not null,
    confirmed_at timestamptz,
    cancelled_at timestamptz,
    constraint fk_match_participants_match foreign key (match_id) references matches (id),
    constraint fk_match_participants_user foreign key (user_id) references users (id),
    constraint uq_match_participants_match_user unique (match_id, user_id),
    constraint chk_match_participants_status check (
        status in ('requested', 'approved', 'rejected', 'awaiting_payment', 'confirmed', 'cancelled')
    )
);

create table bookings (
    id uuid primary key,
    match_id uuid not null,
    pitch_id uuid not null,
    starts_at timestamptz not null,
    ends_at timestamptz not null,
    total_price numeric(10,2) not null,
    currency char(3) not null,
    status varchar not null,
    confirmed_at timestamptz,
    rejected_at timestamptz,
    cancelled_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_bookings_match foreign key (match_id) references matches (id),
    constraint fk_bookings_pitch foreign key (pitch_id) references pitches (id),
    constraint chk_bookings_time_range check (starts_at < ends_at),
    constraint chk_bookings_total_price check (total_price >= 0),
    constraint chk_bookings_currency check (currency ~ '^[A-Z]{3}$'),
    constraint chk_bookings_status check (status in ('provisional', 'confirmed', 'lost', 'rejected', 'cancelled'))
);

create table booking_rejections (
    id uuid primary key,
    booking_id uuid not null,
    reason_code varchar not null,
    explanation text,
    created_at timestamptz not null,
    constraint fk_booking_rejections_booking foreign key (booking_id) references bookings (id),
    constraint chk_booking_rejections_reason_code check (
        reason_code in ('maintenance', 'scheduling_conflict', 'private_event', 'pitch_unavailable', 'other')
    )
);

create table match_price_adjustments (
    id uuid primary key,
    match_id uuid not null,
    old_player_count integer not null,
    new_player_count integer not null,
    old_price_per_player numeric(10,2) not null,
    new_price_per_player numeric(10,2) not null,
    currency char(3) not null,
    created_by_user_id uuid not null,
    created_at timestamptz not null,
    constraint fk_match_price_adjustments_match foreign key (match_id) references matches (id),
    constraint fk_match_price_adjustments_created_by_user foreign key (created_by_user_id) references users (id),
    constraint chk_match_price_adjustments_old_player_count check (old_player_count > 0),
    constraint chk_match_price_adjustments_new_player_count check (new_player_count > 0),
    constraint chk_match_price_adjustments_old_price check (old_price_per_player >= 0),
    constraint chk_match_price_adjustments_new_price check (new_price_per_player >= 0),
    constraint chk_match_price_adjustments_currency check (currency ~ '^[A-Z]{3}$')
);

create table payments (
    id uuid primary key,
    participant_id uuid not null,
    type varchar not null,
    amount numeric(10,2) not null,
    service_fee numeric(10,2) not null,
    currency char(3) not null,
    status varchar not null,
    provider varchar,
    provider_payment_id varchar,
    created_at timestamptz not null,
    paid_at timestamptz,
    constraint fk_payments_participant foreign key (participant_id) references match_participants (id),
    constraint chk_payments_type check (type in ('initial', 'top_up')),
    constraint chk_payments_amount check (amount >= 0),
    constraint chk_payments_service_fee check (service_fee >= 0),
    constraint chk_payments_currency check (currency ~ '^[A-Z]{3}$'),
    constraint chk_payments_status check (status in ('pending', 'succeeded', 'failed', 'cancelled'))
);

create unique index uq_payments_provider_payment_id
    on payments (provider, provider_payment_id)
    where provider is not null and provider_payment_id is not null;

create table refunds (
    id uuid primary key,
    payment_id uuid not null,
    amount numeric(10,2) not null,
    reason_code varchar not null,
    status varchar not null,
    provider_refund_id varchar,
    created_at timestamptz not null,
    processed_at timestamptz,
    constraint fk_refunds_payment foreign key (payment_id) references payments (id),
    constraint chk_refunds_amount check (amount > 0),
    constraint chk_refunds_reason_code check (
        reason_code in ('player_cancelled', 'match_cancelled', 'booking_rejected', 'booking_lost', 'other')
    ),
    constraint chk_refunds_status check (status in ('pending', 'processed', 'failed'))
);

create table chats (
    id uuid primary key,
    type varchar not null,
    group_id uuid,
    match_id uuid,
    booking_id uuid,
    created_at timestamptz not null,
    constraint fk_chats_group foreign key (group_id) references groups (id),
    constraint fk_chats_match foreign key (match_id) references matches (id),
    constraint fk_chats_booking foreign key (booking_id) references bookings (id),
    constraint chk_chats_context check (
        (type = 'group' and group_id is not null and match_id is null and booking_id is null)
        or (type = 'match' and match_id is not null and group_id is null and booking_id is null)
        or (type = 'booking' and booking_id is not null and group_id is null and match_id is null)
    )
);

create table chat_members (
    chat_id uuid not null,
    user_id uuid not null,
    joined_at timestamptz not null,
    constraint pk_chat_members primary key (chat_id, user_id),
    constraint fk_chat_members_chat foreign key (chat_id) references chats (id),
    constraint fk_chat_members_user foreign key (user_id) references users (id)
);

create table messages (
    id uuid primary key,
    chat_id uuid not null,
    sender_user_id uuid not null,
    content text not null,
    created_at timestamptz not null,
    edited_at timestamptz,
    constraint fk_messages_chat foreign key (chat_id) references chats (id),
    constraint fk_messages_sender_user foreign key (sender_user_id) references users (id),
    constraint chk_messages_content check (length(trim(content)) > 0),
    constraint chk_messages_edited_at check (edited_at is null or edited_at >= created_at)
);

create table notifications (
    id uuid primary key,
    user_id uuid not null,
    type varchar not null,
    related_entity_type varchar,
    related_entity_id uuid,
    payload jsonb,
    created_at timestamptz not null,
    read_at timestamptz,
    constraint fk_notifications_user foreign key (user_id) references users (id),
    constraint chk_notifications_type check (length(trim(type)) > 0),
    constraint chk_notifications_read_at check (read_at is null or read_at >= created_at)
);
