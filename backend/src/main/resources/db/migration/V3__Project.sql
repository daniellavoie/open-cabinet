CREATE TABLE project (
	id UUID PRIMARY KEY,
	shop_id UUID NOT NULL REFERENCES shop(id),
	client_id UUID NOT NULL REFERENCES client(id),
	name VARCHAR(200) NOT NULL,
	design JSONB NOT NULL,
	labour_method VARCHAR(20),
	labour_hours NUMERIC(10, 2),
	manual_lines JSONB NOT NULL,
	creation_timestamp TIMESTAMP NOT NULL,
	update_timestamp TIMESTAMP NOT NULL
);

CREATE INDEX project_shop_idx ON project(shop_id);
CREATE INDEX project_client_idx ON project(client_id);
