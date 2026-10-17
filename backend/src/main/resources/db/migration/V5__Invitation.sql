CREATE TABLE invitation (
	id UUID PRIMARY KEY,
	shop_id UUID NOT NULL REFERENCES shop(id),
	token_hash CHAR(64) NOT NULL UNIQUE,
	email VARCHAR(320),
	role VARCHAR(20) NOT NULL,
	invited_by UUID REFERENCES member(id) ON DELETE SET NULL,
	expiration_timestamp TIMESTAMP NOT NULL,
	acceptance_timestamp TIMESTAMP,
	creation_timestamp TIMESTAMP NOT NULL
);

CREATE INDEX invitation_shop_idx ON invitation(shop_id);
