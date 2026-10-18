-- Photos, sketches and PDFs a Member adds to a Conversation message, sent to the model with it.
CREATE TABLE assistant_attachment (
	id UUID PRIMARY KEY,
	shop_id UUID NOT NULL REFERENCES shop (id),
	conversation_id UUID NOT NULL REFERENCES assistant_conversation (id),
	message_id UUID NOT NULL REFERENCES assistant_message (id),
	file_name VARCHAR(255) NOT NULL,
	media_type VARCHAR(100) NOT NULL,
	size_bytes BIGINT NOT NULL,
	content BYTEA NOT NULL,
	creation_timestamp TIMESTAMP NOT NULL
);

CREATE INDEX assistant_attachment_message ON assistant_attachment (message_id);
