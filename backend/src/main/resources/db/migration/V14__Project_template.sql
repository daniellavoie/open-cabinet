-- Cabinet Templates designed for one Project (ADR 0006): an editable draft, and Template Versions
-- that never change once published (ADR 0005). A Cabinet uses one by its id and version.
CREATE TABLE project_template (
	id UUID PRIMARY KEY,
	shop_id UUID NOT NULL REFERENCES shop(id),
	project_id UUID NOT NULL REFERENCES project(id),
	name VARCHAR(200) NOT NULL,
	draft JSONB NOT NULL,
	source_cabinet_id VARCHAR(100),
	creation_timestamp TIMESTAMP NOT NULL,
	update_timestamp TIMESTAMP NOT NULL,
	UNIQUE (project_id, name)
);

CREATE INDEX project_template_project_idx ON project_template(project_id);

CREATE TABLE project_template_version (
	template_id UUID NOT NULL REFERENCES project_template(id),
	version INTEGER NOT NULL,
	content JSONB NOT NULL,
	publish_timestamp TIMESTAMP NOT NULL,
	PRIMARY KEY (template_id, version)
);
