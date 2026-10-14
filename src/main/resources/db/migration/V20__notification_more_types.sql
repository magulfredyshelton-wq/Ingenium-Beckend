-- A V18 só aceitava MESSAGE e POST. Passam a existir gostos, comentários e novos seguidores.
ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check;

ALTER TABLE notification
    ADD CONSTRAINT notification_type_check
        CHECK (type IN ('MESSAGE', 'POST', 'LIKE', 'COMMENT', 'FOLLOW'));
