CREATE  TABLE file (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES usuario(id),
    storage_key VARCHAR(300) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK(size_bytes > 0),
    duration_seconds INTEGER NULL,
    visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC' CHECK ( visibility IN ('PUBLIC', 'PRIVATE') ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE  usuario ADD COLUMN avatar_file_id UUID REFERENCES file(id);