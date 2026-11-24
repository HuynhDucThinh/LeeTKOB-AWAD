-- ============================================================
-- V4__pa3_full_app_schema.sql — LeeTKOB Full App & Pipeline Schema
-- Extends PA#2/PA#3 tables additively without breaking PA#2 contracts.
-- ============================================================

-- 1. Extend labs with PA#3 metadata and auto-increment identity default
CREATE SEQUENCE IF NOT EXISTS labs_id_seq;
SELECT setval('labs_id_seq', COALESCE((SELECT MAX(id) FROM labs), 0) + 1, false);
ALTER TABLE labs ALTER COLUMN id SET DEFAULT nextval('labs_id_seq');

ALTER TABLE labs
    ADD COLUMN IF NOT EXISTS title VARCHAR(200) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS statement_md TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS cost_cap_usd NUMERIC(10, 4) NOT NULL DEFAULT 5.0000,
    ADD COLUMN IF NOT EXISTS created_by UUID NULL REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- 2. Lab test cases (F2)
CREATE TABLE IF NOT EXISTS lab_tests (
    id              BIGSERIAL    PRIMARY KEY,
    lab_id          INT          NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    input_data      TEXT         NOT NULL DEFAULT '',
    expected_output TEXT         NOT NULL DEFAULT '',
    time_limit_ms   INT          NOT NULL DEFAULT 2000 CHECK (time_limit_ms > 0),
    memory_limit_mb INT          NOT NULL DEFAULT 256  CHECK (memory_limit_mb > 0),
    is_sample       BOOLEAN      NOT NULL DEFAULT FALSE,
    order_index     INT          NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_lab_tests_lab_id ON lab_tests(lab_id);

-- 3. Lab constraints (F2, F5, F6, G5)
CREATE TABLE IF NOT EXISTS lab_constraints (
    lab_id           INT         NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    constraint_id    VARCHAR(64) NOT NULL,
    constraint_type  VARCHAR(16) NOT NULL CHECK (constraint_type IN ('hard', 'semantic')),
    description      TEXT        NOT NULL,
    deduction_tenths INT         NOT NULL CHECK (deduction_tenths BETWEEN 0 AND 100),
    PRIMARY KEY (lab_id, constraint_id)
);

-- 4. Student submissions (F3, F4, Q1, F11)
CREATE TABLE IF NOT EXISTS submissions (
    id                SERIAL       PRIMARY KEY,
    lab_id            INT          NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    student_id        UUID         NOT NULL REFERENCES users(id),
    source_code       TEXT         NOT NULL,
    stripped_code     TEXT         NULL,
    judge_status      VARCHAR(32)  NOT NULL DEFAULT 'queued'
        CHECK (judge_status IN ('queued', 'running', 'AC', 'WA', 'TLE', 'MLE', 'RE', 'CE')),
    score_tenths      INT          NOT NULL DEFAULT 0 CHECK (score_tenths BETWEEN 0 AND 100),
    passed_tests      INT          NOT NULL DEFAULT 0,
    total_tests       INT          NOT NULL DEFAULT 0,
    execution_time_ms INT          NULL,
    memory_kb         INT          NULL,
    compile_error     TEXT         NULL,
    trace_id          VARCHAR(64)  NOT NULL,
    idempotency_key   VARCHAR(128) NULL,
    submitted_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    judged_at         TIMESTAMPTZ  NULL,
    CONSTRAINT uq_submissions_idempotency UNIQUE (lab_id, student_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_submissions_lab_student ON submissions(lab_id, student_id);
CREATE INDEX IF NOT EXISTS idx_submissions_trace_id ON submissions(trace_id);

-- 5. Durable PostgreSQL queue for Separate Worker (Q1 exact-once + PA#5 trace propagation)
CREATE TABLE IF NOT EXISTS judge_jobs (
    id            BIGSERIAL   PRIMARY KEY,
    submission_id INT         NOT NULL UNIQUE REFERENCES submissions(id) ON DELETE CASCADE,
    status        VARCHAR(16) NOT NULL DEFAULT 'queued'
        CHECK (status IN ('queued', 'running', 'done', 'failed')),
    attempts      INT         NOT NULL DEFAULT 0,
    locked_by     VARCHAR(64) NULL,
    locked_at     TIMESTAMPTZ NULL,
    trace_parent  VARCHAR(128) NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at   TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_judge_jobs_status_id ON judge_jobs(status, id);

-- 6. Extend review_items with checker verdicts, evidence, guardrails & 10% pass sampling (F5-F8)
ALTER TABLE review_items
    ADD COLUMN IF NOT EXISTS verdict          VARCHAR(16) NOT NULL DEFAULT 'unsure'
        CHECK (verdict IN ('pass', 'violation', 'unsure')),
    ADD COLUMN IF NOT EXISTS source           VARCHAR(16) NOT NULL DEFAULT 'llm'
        CHECK (source IN ('hard_rule', 'llm', 'guardrail')),
    ADD COLUMN IF NOT EXISTS evidence_json    TEXT        NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS guardrail_reason VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS in_queue         BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS sampled_pass     BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS decided_by       VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS decided_at       TIMESTAMPTZ NULL;

-- 7. LLM call logs for cost cap (G4), audit & PA#3/PA#5 observability
CREATE TABLE IF NOT EXISTS llm_call_logs (
    id                  BIGSERIAL      PRIMARY KEY,
    submission_id       INT            NULL REFERENCES submissions(id) ON DELETE SET NULL,
    lab_id              INT            NULL REFERENCES labs(id) ON DELETE SET NULL,
    constraint_id       VARCHAR(64)    NOT NULL,
    provider            VARCHAR(64)    NOT NULL,
    model               VARCHAR(128)   NOT NULL,
    provider_request_id VARCHAR(128)   NULL,
    prompt_tokens       INT            NOT NULL DEFAULT 0,
    completion_tokens   INT            NOT NULL DEFAULT 0,
    latency_ms          INT            NOT NULL DEFAULT 0,
    cost_usd            NUMERIC(12, 6) NOT NULL DEFAULT 0,
    sanitized_prompt    TEXT           NULL,
    raw_response        TEXT           NULL,
    verdict             VARCHAR(16)    NOT NULL,
    guardrail_reason    VARCHAR(64)    NULL,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_llm_call_logs_lab_id ON llm_call_logs(lab_id);
CREATE INDEX IF NOT EXISTS idx_llm_call_logs_submission_id ON llm_call_logs(submission_id);

-- 8. Appeals (F9, A1-A3)
CREATE TABLE IF NOT EXISTS appeals (
    id              BIGSERIAL   PRIMARY KEY,
    lab_id          INT         NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    submission_id   INT         NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    student_id      UUID        NOT NULL REFERENCES users(id),
    constraint_id   VARCHAR(64) NOT NULL,
    reason          TEXT        NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'accepted', 'rejected')),
    decision_reason TEXT        NULL,
    decided_by      UUID        NULL REFERENCES users(id),
    decided_at      TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_appeals_submission_constraint UNIQUE (submission_id, constraint_id)
);

CREATE INDEX IF NOT EXISTS idx_appeals_lab_id ON appeals(lab_id);
