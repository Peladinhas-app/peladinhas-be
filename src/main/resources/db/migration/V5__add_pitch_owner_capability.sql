create table pitch_owner_profiles (
    user_id uuid primary key references users(id) on delete restrict,
    activated_at timestamptz not null,
    created_at timestamptz not null
);

create table pitch_owner_invitation_codes (
    id uuid primary key,
    code_hash varchar not null unique,
    expires_at timestamptz null,
    used_at timestamptz null,
    used_by_user_id uuid null references users(id) on delete restrict,
    created_at timestamptz not null,
    constraint chk_pitch_owner_invitation_code_hash_not_blank check (length(trim(code_hash)) > 0),
    constraint chk_pitch_owner_invitation_used_pair check (
        (used_at is null and used_by_user_id is null)
        or (used_at is not null and used_by_user_id is not null)
    )
);

create index idx_pitch_owner_invitation_codes_unused_expiry
    on pitch_owner_invitation_codes (expires_at)
    where used_at is null;

create index idx_pitch_owner_invitation_codes_used_by_user
    on pitch_owner_invitation_codes (used_by_user_id)
    where used_by_user_id is not null;

insert into pitch_owner_profiles (user_id, activated_at, created_at)
select distinct owner_user_id, current_timestamp, current_timestamp
from pitches
on conflict (user_id) do nothing;
