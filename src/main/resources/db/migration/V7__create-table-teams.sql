create table if not exists teams
(
    id             bigserial primary key,
    match_id       uuid   not null,
    club_jersey_id bigint not null
);

create index if not exists idx_teams_match_id on teams (match_id);

