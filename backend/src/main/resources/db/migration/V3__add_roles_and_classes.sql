-- ============================================================
-- V3__add_roles_and_classes.sql — User Roles & Class Memberships
-- ============================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(16) NOT NULL DEFAULT 'student'
        CHECK (role IN ('admin', 'ta', 'student'));

CREATE TABLE IF NOT EXISTS classes (
    id         INT          PRIMARY KEY,
    code       VARCHAR(64)  NOT NULL UNIQUE,
    name       VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS class_members (
    class_id    INT         NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    member_role VARCHAR(16) NOT NULL CHECK (member_role IN ('ta', 'student')),
    PRIMARY KEY (class_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_class_members_user_id ON class_members(user_id);
