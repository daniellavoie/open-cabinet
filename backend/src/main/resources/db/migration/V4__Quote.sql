CREATE TABLE quote (
	id UUID PRIMARY KEY,
	shop_id UUID NOT NULL REFERENCES shop(id),
	project_id UUID NOT NULL REFERENCES project(id),
	number INTEGER NOT NULL,
	status VARCHAR(20) NOT NULL,
	content JSONB NOT NULL,
	creation_timestamp TIMESTAMP NOT NULL,
	sent_timestamp TIMESTAMP,
	UNIQUE (shop_id, number)
);

CREATE INDEX quote_project_idx ON quote(project_id);
