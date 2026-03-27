create table if not exists clubs_members
(
    id             bigserial PRIMARY KEY,
    user_id        UUID,
    name           varchar(250),
    rating         integer,
    times_mvp      integer,
    times_champion integer,
    team_id        bigint,
    club_role      varchar(50)
);

create index if not exists idx_clubs_members_user_id on clubs_members (user_id);

