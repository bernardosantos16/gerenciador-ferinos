create table users
(
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name     varchar(250) not null,
    nickname varchar(100) not null unique,
    login    varchar(100) not null unique,
    password varchar(255) not null,
    created_at TIMESTAMP WITH TIME ZONE not null,
    status   varchar(50) not null
)