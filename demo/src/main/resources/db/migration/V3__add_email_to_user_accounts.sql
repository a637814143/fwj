ALTER TABLE user_accounts
    ADD COLUMN email VARCHAR(255);

UPDATE user_accounts
SET email = CONCAT(username, '@example.com')
WHERE email IS NULL OR email = '';

ALTER TABLE user_accounts
    MODIFY COLUMN email VARCHAR(255) NOT NULL;

ALTER TABLE user_accounts
    ADD UNIQUE INDEX uk_user_accounts_email (email);
