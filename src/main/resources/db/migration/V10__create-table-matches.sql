create table if not exists matches
(
    id        uuid primary key,
    club_id   uuid        not null,
    date_time timestamptz not null
);

create index if not exists idx_matches_club_id on matches (club_id);

