alter table if exists clubs
    add column if not exists owner_user_id UUID;

update clubs c
set owner_user_id = (
    select cm.user_id
    from clubs_members cm
    where cm.club_id = c.id
      and cm.club_role = 'DIRECTOR'
    order by cm.id asc
    limit 1
)
where c.owner_user_id is null;
