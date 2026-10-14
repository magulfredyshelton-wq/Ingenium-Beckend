CREATE TABLE notification (
                              id          uuid PRIMARY KEY,
                              user_id     uuid         NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                              type        varchar(10)  NOT NULL CHECK (type IN ('MESSAGE', 'POST')),
                              actor_id    uuid         REFERENCES usuario(id) ON DELETE SET NULL,
                              post_id     uuid         REFERENCES post(id) ON DELETE CASCADE,
                              room_id     uuid         REFERENCES chatroom(id) ON DELETE CASCADE,
                              text        varchar(200) NOT NULL,
                              read_at     timestamp    NULL,
                              created_at  timestamp    NOT NULL
);

CREATE INDEX idx_notification_user ON notification (user_id, created_at DESC);
CREATE INDEX idx_notification_unread ON notification (user_id) WHERE read_at IS NULL;