create table refresh_tokens
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        not null,
    token      varchar(255) not null unique,
    created_at TIMESTAMP WITH TIME ZONE not null,
    expires_at TIMESTAMP WITH TIME ZONE not null,
    revoked    boolean     not null default false
);

create index if not exists idx_refresh_tokens_user_id on refresh_tokens (user_id);

