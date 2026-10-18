-- The pictures printed with a Quote, captured with its content. Kept apart from the content, which
-- the app loads with every list of Quotes. Quotes created before have none.
ALTER TABLE quote ADD COLUMN pictures JSONB;
