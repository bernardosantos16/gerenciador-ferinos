delete from club_invite_tokens;

alter table club_invite_tokens
    drop column if exists token_hash,
    drop column if exists token_salt,
    drop column if exists revoked_at;

alter table club_invite_tokens
    add column token_cipher varchar(512) not null;

alter table club_invite_tokens
    add constraint uq_club_invite_tokens_club unique (club_id);
