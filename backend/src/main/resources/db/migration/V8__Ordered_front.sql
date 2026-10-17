-- Fronts can be ordered from a supplier: give every Shop a starter Front style, priced per square
-- metre, and make it its Construction Standard's ordered Front style.
WITH seeded AS (
	INSERT INTO material (id, shop_id, kind, name, sku, price, grain, creation_timestamp, update_timestamp)
	SELECT gen_random_uuid(), id, 'FRONT', 'Façade shaker MDF peinte (commandée)', 'FRT-SHAKER', 300.00, FALSE,
		now(), now()
	FROM shop
	RETURNING id, shop_id
)
UPDATE shop
SET settings = jsonb_set(settings, '{constructionStandard,orderedFrontId}', to_jsonb(seeded.id::text))
FROM seeded
WHERE shop.id = seeded.shop_id;
