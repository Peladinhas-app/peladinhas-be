-- Adds external authentication identity mapping for Peladinhas users.
-- Rollback expectation: rebuild disposable development/test databases from scratch.

alter table users
    add column auth_provider varchar,
    add column auth_subject varchar;

update users
set auth_provider = 'development',
    auth_subject = id::text
where auth_provider is null
   or auth_subject is null;

alter table users
    alter column auth_provider set not null,
    alter column auth_subject set not null,
    add constraint uq_users_auth_identity unique (auth_provider, auth_subject),
    add constraint chk_users_auth_provider_not_blank check (length(trim(auth_provider)) > 0),
    add constraint chk_users_auth_subject_not_blank check (length(trim(auth_subject)) > 0);
