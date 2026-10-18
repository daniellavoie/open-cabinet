-- The Design Assistant is off until an Owner opts the Shop in (ADR 0004), within a monthly budget
-- in US dollars, the currency the model provider bills in.
CREATE TABLE assistant_settings (
	shop_id UUID PRIMARY KEY REFERENCES shop (id),
	enabled BOOLEAN NOT NULL,
	monthly_budget_usd NUMERIC(10, 2) NOT NULL,
	enabled_by UUID REFERENCES member (id) ON DELETE SET NULL,
	enabled_timestamp TIMESTAMP,
	update_timestamp TIMESTAMP NOT NULL
);

-- Every call to the model, with what it cost, counted against the Shop's budget.
CREATE TABLE assistant_usage (
	id UUID PRIMARY KEY,
	shop_id UUID NOT NULL REFERENCES shop (id),
	member_id UUID REFERENCES member (id) ON DELETE SET NULL,
	purpose VARCHAR(40) NOT NULL,
	model VARCHAR(80) NOT NULL,
	input_tokens BIGINT NOT NULL,
	output_tokens BIGINT NOT NULL,
	cost_usd NUMERIC(12, 6) NOT NULL,
	creation_timestamp TIMESTAMP NOT NULL
);

CREATE INDEX assistant_usage_shop_month ON assistant_usage (shop_id, creation_timestamp);
