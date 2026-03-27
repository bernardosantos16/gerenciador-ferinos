create table if not exists clubs_jerseys
(
    id                  bigserial PRIMARY KEY,
    hex_color           varchar(7)   not null,
    name                varchar(100) not null,
    is_goalkeeper_jersey boolean      not null,
    club_id             UUID         not null
);

create index if not exists idx_clubs_jerseys_club_id on clubs_jerseys (club_id);

