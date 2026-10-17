CREATE TABLE shop_cabinet_template (
	id UUID PRIMARY KEY,
	shop_id UUID NOT NULL REFERENCES shop(id),
	name VARCHAR(200) NOT NULL,
	base_code VARCHAR(100) NOT NULL,
	preset JSONB NOT NULL,
	creation_timestamp TIMESTAMP NOT NULL
);

CREATE INDEX shop_cabinet_template_shop_idx ON shop_cabinet_template(shop_id);
