ALTER TABLE message
    ADD COLUMN reply_to_message_id UUID,
    ADD COLUMN read_at TIMESTAMP,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN forwarded BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE message
    ADD CONSTRAINT fk_message_reply
        FOREIGN KEY (reply_to_message_id)
            REFERENCES message(id)
            ON DELETE SET NULL;

CREATE INDEX idx_message_reply_to
    ON message(reply_to_message_id);