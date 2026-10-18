-- Mirror replaces a blind corner's blind side, for any Cabinet: a blind side on the left is the
-- template mirrored.
UPDATE project
SET design = jsonb_set(design, '{cabinets}', (
	SELECT COALESCE(jsonb_agg(
		CASE WHEN cabinet ->> 'blindSide' = 'LEFT' THEN (cabinet - 'blindSide') || '{"mirrored": true}'::jsonb
			ELSE cabinet - 'blindSide' END
		ORDER BY position), '[]'::jsonb)
	FROM jsonb_array_elements(design -> 'cabinets') WITH ORDINALITY AS cabinets(cabinet, position)
))
WHERE jsonb_typeof(design -> 'cabinets') = 'array';

UPDATE shop_cabinet_template
SET preset = CASE WHEN preset ->> 'blindSide' = 'LEFT' THEN (preset - 'blindSide') || '{"mirrored": true}'::jsonb
	ELSE preset - 'blindSide' END;

-- Templates shared with the Shop library are used by every Project of the Shop; those created in
-- the library belong to no Project. Archived templates are hidden when adding Cabinets.
ALTER TABLE project_template ALTER COLUMN project_id DROP NOT NULL;
ALTER TABLE project_template ADD COLUMN shared_timestamp TIMESTAMP;
ALTER TABLE project_template ADD COLUMN archived_timestamp TIMESTAMP;
CREATE INDEX project_template_shop_idx ON project_template(shop_id);
