create extension if not exists pg_trgm;

create index if not exists idx_clubs_name_trgm on clubs using gin (name gin_trgm_ops);
create index if not exists idx_clubs_nickname_trgm on clubs using gin (nickname gin_trgm_ops);

alter table clubs add column if not exists join_policy varchar(20) not null default 'INVITE_ONLY';

create table if not exists club_invite_tokens
(
    id         bigserial primary key,
    club_id    uuid not null references clubs (id) on delete cascade,
    token_hash varchar(64) not null,
    token_salt varchar(64) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    revoked_at timestamp with time zone
);

create index if not exists idx_club_invite_tokens_club on club_invite_tokens (club_id);
create index if not exists idx_club_invite_tokens_hash on club_invite_tokens (token_hash);

create table if not exists club_membership_requests
(
    id           bigserial primary key,
    club_id      uuid not null references clubs (id) on delete cascade,
    user_id      uuid not null references users (id),
    name         varchar(250) not null,
    nickname     varchar(100) not null,
    status       varchar(20) not null,
    requested_at timestamp with time zone not null,
    reviewed_at  timestamp with time zone,
    reviewed_by  uuid
);

create index if not exists idx_membership_requests_club_status on club_membership_requests (club_id, status);

create unique index if not exists uq_membership_requests_pending
    on club_membership_requests (club_id, user_id)
    where status = 'PENDING';

create table if not exists notifications
(
    id         bigserial primary key,
    user_id    uuid not null references users (id),
    type       varchar(50) not null,
    title      varchar(250) not null,
    message    varchar(500) not null,
    read       boolean not null default false,
    created_at timestamp with time zone not null
);

create index if not exists idx_notifications_user_read on notifications (user_id, read);
