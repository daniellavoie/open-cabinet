-- Cabinets now keep the Template Version they were placed with
-- (docs/adr/0005-cabinet-keeps-its-template-version.md). Every Cabinet saved so far was built
-- from the starter templates as they shipped, which are now version 1.

UPDATE project
SET design = jsonb_set(design, '{cabinets}', (
	SELECT COALESCE(jsonb_agg(cabinet || '{"templateVersion": 1}'::jsonb ORDER BY position), '[]'::jsonb)
	FROM jsonb_array_elements(design -> 'cabinets') WITH ORDINALITY AS cabinets(cabinet, position)
))
WHERE jsonb_typeof(design -> 'cabinets') = 'array';

UPDATE shop_cabinet_template
SET preset = preset || '{"templateVersion": 1}'::jsonb;
