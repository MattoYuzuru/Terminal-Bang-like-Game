ALTER TABLE accounts
ADD COLUMN language TEXT NOT NULL DEFAULT 'en';

UPDATE accounts
SET language = 'en'
WHERE language IS NULL OR language = '';
