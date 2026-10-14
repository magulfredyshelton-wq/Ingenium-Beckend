CREATE TABLE verification_code (
                                   id UUID PRIMARY KEY,
                                   email VARCHAR(255) NOT NULL,
                                   purpose VARCHAR(20) NOT NULL
                                       CHECK (purpose IN ('REGISTER', 'PASSWORD_RESET')),
                                   code_hash VARCHAR(255) NOT NULL,
                                   attempts SMALLINT NOT NULL DEFAULT 0
                                       CHECK (attempts BETWEEN 0 AND 5),
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                   expires_at TIMESTAMPTZ NOT NULL,
                                   used_at TIMESTAMPTZ,
                                   CHECK (expires_at > created_at)
);

CREATE INDEX idx_verification_code_history
    ON verification_code (email, purpose, created_at DESC);

CREATE UNIQUE INDEX idx_verification_code_active
    ON verification_code (email, purpose)
    WHERE used_at IS NULL;