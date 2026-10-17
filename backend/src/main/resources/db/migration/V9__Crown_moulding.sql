-- Cabinets can carry crown moulding: give every Shop a starter moulding, priced per metre, and make
-- it its Construction Standard's crown moulding.
WITH seeded AS (
	INSERT INTO material (id, shop_id, kind, name, sku, price, grain, creation_timestamp, update_timestamp)
	SELECT gen_random_uuid(), id, 'MOULDING', 'Moulure couronne 3" MDF apprêtée', 'CRN-3', 9.50, FALSE, now(), now()
	FROM shop
	RETURNING id, shop_id
)
UPDATE shop
SET settings = jsonb_set(settings, '{constructionStandard,crownMouldingId}', to_jsonb(seeded.id::text))
FROM seeded
WHERE shop.id = seeded.shop_id;
