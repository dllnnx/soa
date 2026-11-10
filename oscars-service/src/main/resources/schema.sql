CREATE TABLE IF NOT EXISTS oscar_job (
    id UUID PRIMARY KEY,
    type VARCHAR(32) NOT NULL CHECK (type IN ('REWARD_R', 'RESET_BY_GENRE')),
    arguments JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    lease_token UUID,
    lease_until TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_count BIGINT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    CHECK ((lease_token IS NULL) = (lease_until IS NULL)),
    CHECK ((status = 'COMPLETED' AND updated_count IS NOT NULL AND updated_count >= 0) OR
           (status <> 'COMPLETED' AND updated_count IS NULL))
);
CREATE INDEX IF NOT EXISTS oscar_job_ready ON oscar_job(next_attempt_at, created_at, id)
    WHERE status IN ('PENDING', 'RUNNING');
