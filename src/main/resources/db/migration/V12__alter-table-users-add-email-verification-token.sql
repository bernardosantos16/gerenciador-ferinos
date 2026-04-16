ALTER TABLE users ADD COLUMN email_verification_token VARCHAR(255);
ALTER TABLE users ADD CONSTRAINT uk_users_email_verification_token UNIQUE (email_verification_token);
