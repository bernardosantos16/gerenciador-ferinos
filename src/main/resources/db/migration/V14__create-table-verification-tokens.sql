ALTER TABLE users DROP CONSTRAINT IF EXISTS uk_users_email_verification_token;
ALTER TABLE users DROP COLUMN IF EXISTS email_verification_token;

CREATE TABLE verification_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64)  NOT NULL,
    type       VARCHAR(30)  NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at    TIMESTAMP WITH TIME ZONE,
    user_id    UUID         NOT NULL
);

CREATE INDEX idx_verification_tokens_token_hash_type ON verification_tokens (token_hash, type);
CREATE INDEX idx_verification_tokens_user_id ON verification_tokens (user_id);
