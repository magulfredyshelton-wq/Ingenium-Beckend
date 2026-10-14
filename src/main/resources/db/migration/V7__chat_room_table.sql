CREATE TABLE chatroom (
    id UUID PRIMARY KEY,
    sender_id UUID REFERENCES usuario(id),
    receptor_id UUID REFERENCES usuario(id),
    created_at TIMESTAMP NOT NULL
);