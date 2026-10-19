-- How the Shop likes its kitchens, in its own words, given to the Design Assistant before the
-- built-in Design Guidelines.
ALTER TABLE assistant_settings ADD COLUMN style_text TEXT;

-- The Remarks left on a Design Proposal and the assistant's explanation for each, by remark key.
ALTER TABLE design_proposal ADD COLUMN remarks JSONB;
ALTER TABLE design_proposal ADD COLUMN remark_explanations JSONB;
