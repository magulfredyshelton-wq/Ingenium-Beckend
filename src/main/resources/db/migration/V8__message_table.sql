CREATE TABLE message (
    id UUID PRIMARY KEY,
    message VARCHAR(1000) NOT NULL,
    sender_id UUID REFERENCES usuario(id),
    sent_at TIMESTAMP NOT NULL,
    room_id UUID REFERENCES chatroom(id)
);