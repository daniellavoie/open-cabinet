-- How a Project's sheets are nested (RIP_FIRST, CROSSCUT_FIRST); empty keeps the layout with the
-- fewest sheets for each material.
ALTER TABLE project ADD COLUMN nesting_layout VARCHAR(20);

-- The narrowest strip the saw can hold, 1" for every Shop to start.
UPDATE shop
SET settings = jsonb_set(settings, '{constructionStandard,minOffcutMm}', '25.4'::jsonb)
WHERE settings -> 'constructionStandard' IS NOT NULL;
