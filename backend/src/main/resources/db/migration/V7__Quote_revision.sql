ALTER TABLE quote ADD COLUMN revision INTEGER NOT NULL DEFAULT 0;
ALTER TABLE quote DROP CONSTRAINT quote_shop_id_number_key;
ALTER TABLE quote ADD CONSTRAINT quote_shop_id_number_revision_key UNIQUE (shop_id, number, revision);
