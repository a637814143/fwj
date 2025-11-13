ALTER TABLE user_accounts
    ADD COLUMN IF NOT EXISTS email VARCHAR(255);

UPDATE user_accounts
SET email = CONCAT(username, '@example.com')
WHERE email IS NULL OR email = '';

ALTER TABLE user_accounts
    MODIFY COLUMN email VARCHAR(255) NOT NULL;

ALTER TABLE user_accounts
    ADD UNIQUE INDEX IF NOT EXISTS uk_user_accounts_email (email);
