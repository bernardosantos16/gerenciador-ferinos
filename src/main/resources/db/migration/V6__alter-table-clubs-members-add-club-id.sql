alter table clubs_members
    add column if not exists club_id UUID;

create index if not exists idx_clubs_members_club_id on clubs_members (club_id);
create index if not exists idx_clubs_members_club_id_user_id on clubs_members (club_id, user_id);

