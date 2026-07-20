ALTER TABLE verification_tokens ADD COLUMN IF NOT EXISTS token_salt VARCHAR(64);
