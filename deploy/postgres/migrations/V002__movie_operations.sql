CREATE TABLE IF NOT EXISTS movie_operation (
    job_id UUID PRIMARY KEY,
    type VARCHAR(32) NOT NULL CHECK (type IN ('REWARD_R', 'RESET_BY_GENRE')),
    genre VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    updated_count BIGINT,
    error_code VARCHAR(64),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((type = 'REWARD_R' AND genre IS NULL) OR
           (type = 'RESET_BY_GENRE' AND genre IS NOT NULL AND genre IN ('WESTERN', 'ADVENTURE', 'TRAGEDY', 'SCIENCE_FICTION'))),
    CHECK ((status = 'COMPLETED' AND updated_count IS NOT NULL AND updated_count >= 0) OR
           (status <> 'COMPLETED' AND updated_count IS NULL))
);
