DELETE FROM message;
DELETE FROM chatroom;

ALTER TABLE chatroom RENAME COLUMN sender_id TO user1_id;
ALTER TABLE chatroom RENAME COLUMN receptor_id TO user2_id;

ALTER TABLE chatroom ALTER COLUMN user1_id SET NOT NULL;
ALTER TABLE chatroom ALTER COLUMN user2_id SET NOT NULL;