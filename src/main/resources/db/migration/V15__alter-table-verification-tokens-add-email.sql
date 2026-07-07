-- Adiciona coluna email
ALTER TABLE verification_tokens ADD COLUMN email VARCHAR(100);

-- Preenche email para registros existentes
UPDATE verification_tokens vt SET email = (SELECT u.login FROM users u WHERE u.id = vt.user_id);

-- Torna email NOT NULL
ALTER TABLE verification_tokens ALTER COLUMN email SET NOT NULL;

-- Remove coluna user_id (substituida por email)
ALTER TABLE verification_tokens DROP COLUMN user_id;

-- Novo indice para buscas por email + tipo
CREATE INDEX idx_verification_tokens_email_type ON verification_tokens (email, type);
