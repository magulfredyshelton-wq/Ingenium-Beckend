ALTER TABLE activity_area
    ADD COLUMN field_study_id uuid REFERENCES field_study(id);

ALTER TABLE students
    ADD COLUMN seeking varchar(10) NOT NULL DEFAULT 'BOTH'
        CHECK (seeking IN ('INTERNSHIP', 'JOB', 'BOTH'));

CREATE TABLE tag_category (
                              id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                              field_study_id  uuid         NOT NULL REFERENCES field_study(id),
                              name            varchar(80)  NOT NULL UNIQUE
);

CREATE TABLE tag (
                     id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                     tag_category_id  uuid         NOT NULL REFERENCES tag_category(id),
                     name             varchar(80)  NOT NULL UNIQUE,
                     slug             varchar(80)  NOT NULL UNIQUE,
                     active           boolean      NOT NULL DEFAULT true
);

CREATE TABLE course_tag (
                            course_id  uuid          NOT NULL REFERENCES course(id),
                            tag_id     uuid          NOT NULL REFERENCES tag(id),
                            weight     numeric(3,2)  NOT NULL DEFAULT 1.00 CHECK (weight > 0 AND weight <= 1),
                            PRIMARY KEY (course_id, tag_id)
);

CREATE TABLE user_tag (
                          user_id  uuid NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                          tag_id   uuid NOT NULL REFERENCES tag(id),
                          PRIMARY KEY (user_id, tag_id)
);

CREATE TABLE user_tag_category (
                                   user_id          uuid NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                                   tag_category_id  uuid NOT NULL REFERENCES tag_category(id),
                                   PRIMARY KEY (user_id, tag_category_id)
);

CREATE TABLE post (
                      id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                      author_id   uuid         NOT NULL REFERENCES usuario(id),
                      post_type   varchar(10)  NOT NULL CHECK (post_type IN ('JOB', 'PROJECT', 'GENERAL')),
                      created_at  timestamptz  NOT NULL DEFAULT now(),
                      deleted_at  timestamptz  NULL,
                      UNIQUE (id, author_id)   -- needed by the composite FK in job
);

CREATE TABLE post_tag (
                          post_id  uuid NOT NULL REFERENCES post(id) ON DELETE CASCADE,
                          tag_id   uuid NOT NULL REFERENCES tag(id),
                          PRIMARY KEY (post_id, tag_id)
);

CREATE TABLE post_media (
                            post_id   uuid      NOT NULL REFERENCES post(id) ON DELETE CASCADE,
                            file_id   uuid      NOT NULL REFERENCES file(id),
                            position  smallint  NOT NULL DEFAULT 0,
                            PRIMARY KEY (post_id, file_id)
);

CREATE TABLE job (
                     post_id            uuid PRIMARY KEY REFERENCES post(id) ON DELETE CASCADE,
                     company_id         uuid          NOT NULL REFERENCES company(id),
                     title              varchar(200)  NOT NULL,
                     description        text          NOT NULL,
                     work_type          varchar(10)   NOT NULL CHECK (work_type IN ('INTERNSHIP', 'JOB', 'BOTH')),
                     application_start  date          NOT NULL,
                     application_end    date          NOT NULL,
                     start_date         date          NULL,
                     end_date           date          NULL,
                     status             varchar(10)   NOT NULL DEFAULT 'OPEN'
                         CHECK (status IN ('OPEN', 'CLOSED', 'EXPIRED')),
                     CHECK (application_end >= application_start),
                     CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
                     CHECK (work_type <> 'INTERNSHIP' OR end_date IS NOT NULL),
    -- only a company can be the author of a job post
                     FOREIGN KEY (post_id, company_id) REFERENCES post(id, author_id)
);

CREATE TABLE project (
                         post_id         uuid PRIMARY KEY REFERENCES post(id) ON DELETE CASCADE,
                         title           varchar(200)  NOT NULL,
                         description     text          NOT NULL,
                         repository_url  varchar(255)  NULL,
                         status          varchar(15)   NOT NULL DEFAULT 'IN_PROGRESS'
                             CHECK (status IN ('IN_PROGRESS', 'COMPLETED'))
);

CREATE TABLE general_post (
                              post_id  uuid PRIMARY KEY REFERENCES post(id) ON DELETE CASCADE,
                              content  text NOT NULL
);

CREATE TABLE follower (
                          follower_id  uuid         NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                          followed_id  uuid         NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                          since        timestamptz  NOT NULL DEFAULT now(),
                          PRIMARY KEY (follower_id, followed_id),
                          CHECK (follower_id <> followed_id)
);

CREATE TABLE post_like (
                           user_id     uuid         NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                           post_id     uuid         NOT NULL REFERENCES post(id) ON DELETE CASCADE,
                           created_at  timestamptz  NOT NULL DEFAULT now(),
                           PRIMARY KEY (user_id, post_id)
);

CREATE TABLE comment (
                         id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                         post_id            uuid           NOT NULL REFERENCES post(id) ON DELETE CASCADE,
                         author_id          uuid           NOT NULL REFERENCES usuario(id),
                         parent_comment_id  uuid           NULL REFERENCES comment(id) ON DELETE CASCADE,
                         content            varchar(1000)  NOT NULL,
                         created_at         timestamptz    NOT NULL DEFAULT now()
);

CREATE TABLE share (
                       id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                       sender_id     uuid          NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                       recipient_id  uuid          NULL REFERENCES usuario(id) ON DELETE CASCADE,
                       post_id       uuid          NOT NULL REFERENCES post(id) ON DELETE CASCADE,
                       channel       varchar(10)   NOT NULL CHECK (channel IN ('DIRECT', 'LINK', 'WHATSAPP', 'OTHER')),
                       note          varchar(300)  NULL,
                       created_at    timestamptz   NOT NULL DEFAULT now(),
    -- DIRECT = sent to a person inside the platform (needs a recipient); other channels have none
                       CHECK ((channel = 'DIRECT' AND recipient_id IS NOT NULL)
                           OR (channel <> 'DIRECT' AND recipient_id IS NULL)),
                       CHECK (recipient_id IS NULL OR recipient_id <> sender_id)
);

CREATE FUNCTION check_post_tag_limit() RETURNS trigger AS $$
BEGIN
    IF (SELECT count(*) FROM post_tag WHERE post_id = NEW.post_id) >= 5 THEN
        RAISE EXCEPTION 'A post can have at most 5 tags';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_post_tag_limit
    BEFORE INSERT ON post_tag
    FOR EACH ROW EXECUTE FUNCTION check_post_tag_limit();

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_tag_slug_trgm        ON tag USING gin (slug gin_trgm_ops);
CREATE INDEX idx_tag_category         ON tag (tag_category_id);
CREATE INDEX idx_course_tag_tag       ON course_tag (tag_id, course_id);
CREATE INDEX idx_user_tag_tag         ON user_tag (tag_id, user_id);
CREATE INDEX idx_post_tag_tag_post    ON post_tag (tag_id, post_id);
CREATE INDEX idx_post_created         ON post (created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_post_author          ON post (author_id, created_at DESC);
CREATE INDEX idx_job_status_end       ON job (status, application_end);
CREATE INDEX idx_follower_followed    ON follower (followed_id, follower_id);
CREATE INDEX idx_post_like_post       ON post_like (post_id);
CREATE INDEX idx_comment_post         ON comment (post_id, created_at);
CREATE INDEX idx_share_post           ON share (post_id);
CREATE INDEX idx_share_recipient      ON share (recipient_id, created_at DESC) WHERE recipient_id IS NOT NULL;
CREATE INDEX idx_message_room_sent    ON message (room_id, sent_at DESC);