-- ============================================================
-- V1__init_user_auth.sql — LeeTKOB User & Auth Schema
-- ============================================================

CREATE SCHEMA IF NOT EXISTS public;
SET search_path TO public;

-- ---------- ENUMS ----------
CREATE TYPE oauth_provider AS ENUM ('GOOGLE', 'FACEBOOK');

-- ---------- USERS & AUTH ----------
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    full_name     VARCHAR(150),
    avatar_url    TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE oauth_accounts (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider         oauth_provider NOT NULL,
    provider_user_id VARCHAR(255)   NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_oauth_provider_user UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts(user_id);
