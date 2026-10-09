-- Backfills the V1/V5 creator-as-player invariant for existing matches.
-- Rollback expectation: rebuild disposable development/test databases from scratch.

insert into match_participants (
    id,
    match_id,
    user_id,
    status,
    joined_at,
    confirmed_at,
    cancelled_at
)
select
    gen_random_uuid(),
    m.id,
    m.created_by_user_id,
    'approved',
    m.created_at,
    null,
    null
from matches m
where not exists (
    select 1
    from match_participants mp
    where mp.match_id = m.id
      and mp.user_id = m.created_by_user_id
);
