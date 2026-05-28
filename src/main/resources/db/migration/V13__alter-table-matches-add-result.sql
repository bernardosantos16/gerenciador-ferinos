alter table matches
    add column if not exists team_champion_id bigint;

alter table matches
    add column if not exists club_member_mvp_id bigint;

create index if not exists idx_matches_team_champion_id
    on matches (team_champion_id);

create index if not exists idx_matches_club_member_mvp_id
    on matches (club_member_mvp_id);
