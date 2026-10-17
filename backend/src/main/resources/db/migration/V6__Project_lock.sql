ALTER TABLE project ADD COLUMN locked_by UUID REFERENCES member(id) ON DELETE SET NULL;
ALTER TABLE project ADD COLUMN lock_expiration_timestamp TIMESTAMP;
