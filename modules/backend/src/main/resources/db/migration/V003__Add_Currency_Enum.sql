CREATE TYPE currency_enum AS ENUM ('USD', 'GBP', 'EUR');
ALTER TABLE jobs ADD COLUMN currency currency_enum not null default 'GBP';
