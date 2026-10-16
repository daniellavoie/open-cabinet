CREATE TABLE shop (
	id UUID PRIMARY KEY,
	name VARCHAR(200) NOT NULL,
	currency CHAR(3) NOT NULL,
	units VARCHAR(20) NOT NULL,
	settings JSONB NOT NULL,
	quote_sequence INTEGER NOT NULL DEFAULT 0,
	creation_timestamp TIMESTAMP NOT NULL,
	update_timestamp TIMESTAMP NOT NULL
);

CREATE TABLE member (
	id UUID PRIMARY KEY,
	shop_id UUID NOT NULL REFERENCES shop(id),
	subject VARCHAR(255) NOT NULL UNIQUE,
	name VARCHAR(200) NOT NULL,
	email VARCHAR(320),
	role VARCHAR(20) NOT NULL,
	creation_timestamp TIMESTAMP NOT NULL
);

CREATE INDEX member_shop_idx ON member(shop_id);

CREATE TABLE client (
	id UUID PRIMARY KEY,
	shop_id UUID NOT NULL REFERENCES shop(id),
	name VARCHAR(200) NOT NULL,
	email VARCHAR(320),
	phone VARCHAR(50),
	address VARCHAR(500),
	creation_timestamp TIMESTAMP NOT NULL,
	update_timestamp TIMESTAMP NOT NULL
);

CREATE INDEX client_shop_idx ON client(shop_id);

CREATE TABLE material (
	id UUID PRIMARY KEY,
	shop_id UUID NOT NULL REFERENCES shop(id),
	kind VARCHAR(20) NOT NULL,
	name VARCHAR(200) NOT NULL,
	sku VARCHAR(100),
	price NUMERIC(12, 2) NOT NULL,
	thickness_mm DOUBLE PRECISION,
	length_mm DOUBLE PRECISION,
	width_mm DOUBLE PRECISION,
	grain BOOLEAN NOT NULL DEFAULT FALSE,
	creation_timestamp TIMESTAMP NOT NULL,
	update_timestamp TIMESTAMP NOT NULL
);

CREATE INDEX material_shop_idx ON material(shop_id);
