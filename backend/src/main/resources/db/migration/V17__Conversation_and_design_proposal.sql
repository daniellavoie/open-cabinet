-- A Project's Conversation with the Design Assistant: the Members' messages and its answers.
-- One request runs at a time per Conversation.
CREATE TABLE assistant_conversation (
	id UUID PRIMARY KEY,
	shop_id UUID NOT NULL REFERENCES shop (id),
	project_id UUID NOT NULL REFERENCES project (id),
	running BOOLEAN NOT NULL,
	creation_timestamp TIMESTAMP NOT NULL
);

CREATE INDEX assistant_conversation_project ON assistant_conversation (shop_id, project_id);

CREATE TABLE assistant_message (
	id UUID PRIMARY KEY,
	conversation_id UUID NOT NULL REFERENCES assistant_conversation (id),
	author VARCHAR(20) NOT NULL,
	member_id UUID REFERENCES member (id) ON DELETE SET NULL,
	text TEXT,
	error_code VARCHAR(40),
	proposal_id UUID,
	creation_timestamp TIMESTAMP NOT NULL
);

CREATE INDEX assistant_message_conversation ON assistant_message (conversation_id, creation_timestamp);

-- A candidate design built by the assistant (ADR 0004): the steps it took, each passing the
-- Construction Rules, replayed onto the Project's design when a Member accepts it. One is open per
-- Project at a time.
CREATE TABLE design_proposal (
	id UUID PRIMARY KEY,
	shop_id UUID NOT NULL REFERENCES shop (id),
	project_id UUID NOT NULL REFERENCES project (id),
	conversation_id UUID NOT NULL REFERENCES assistant_conversation (id),
	status VARCHAR(20) NOT NULL,
	base_design JSONB NOT NULL,
	design JSONB NOT NULL,
	steps JSONB NOT NULL,
	summary TEXT,
	open_questions JSONB NOT NULL,
	assumptions JSONB NOT NULL,
	accepted_by UUID REFERENCES member (id) ON DELETE SET NULL,
	accepted_timestamp TIMESTAMP,
	design_before_accept JSONB,
	accepted_project_timestamp TIMESTAMP,
	applied_steps INTEGER,
	refused_step INTEGER,
	refused_violations JSONB,
	creation_timestamp TIMESTAMP NOT NULL,
	update_timestamp TIMESTAMP NOT NULL
);

CREATE INDEX design_proposal_project ON design_proposal (shop_id, project_id, status);
