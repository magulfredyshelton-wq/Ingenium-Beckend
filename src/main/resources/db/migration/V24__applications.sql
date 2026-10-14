-- Candidaturas dos estudantes às vagas das empresas
CREATE TABLE application (
                             id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                             job_post_id  uuid        NOT NULL REFERENCES job(post_id) ON DELETE CASCADE,
                             student_id   uuid        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
                             message      varchar(1000),
                             status       varchar(12) NOT NULL DEFAULT 'SUBMITTED'
                                 CHECK (status IN ('SUBMITTED', 'VIEWED', 'SHORTLISTED', 'REJECTED')),
                             created_at   timestamptz NOT NULL DEFAULT now(),
                             updated_at   timestamptz NOT NULL DEFAULT now(),
                             UNIQUE (job_post_id, student_id)
);

CREATE INDEX idx_application_job     ON application (job_post_id, created_at DESC);
CREATE INDEX idx_application_student ON application (student_id, created_at DESC);

-- novos tipos de notificação (a coluna era varchar(10))
ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check;
ALTER TABLE notification ALTER COLUMN type TYPE varchar(30);
ALTER TABLE notification
    ADD CONSTRAINT notification_type_check
        CHECK (type IN ('MESSAGE', 'POST', 'LIKE', 'COMMENT', 'FOLLOW',
                        'APPLICATION', 'APPLICATION_UPDATE'));