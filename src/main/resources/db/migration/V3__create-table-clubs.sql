create table if not exists clubs
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       varchar(250) not null,
    nickname   varchar(100) not null unique,
    created_at TIMESTAMP WITH TIME ZONE not null,
    status     varchar(50) not null
);

