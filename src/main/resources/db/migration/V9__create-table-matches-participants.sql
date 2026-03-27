create table if not exists matches_participants
(
    id             bigserial primary key,
    match_id       uuid        not null,
    club_member_id bigint      not null,
    position       varchar(10) not null,
    team_id        bigint
);

create unique index if not exists uq_matches_participants_match_id_club_member_id
    on matches_participants (match_id, club_member_id);

create index if not exists idx_matches_participants_match_id
    on matches_participants (match_id);

create index if not exists idx_matches_participants_team_id
    on matches_participants (team_id);

