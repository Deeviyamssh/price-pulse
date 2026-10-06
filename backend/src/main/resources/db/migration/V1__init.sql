-- ============================================================
-- V1__init.sql  — PricePulse initial schema
-- ============================================================

-- Users
-- Stores registered accounts. password_hash is a BCrypt digest (60 chars).
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(60)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Products
-- One product per URL per user (UNIQUE constraint enforced at DB level — Req 4.2).
-- Deleting a user cascades to all their products.
CREATE TABLE products (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    url          TEXT         NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    status       VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE', 'PAUSED')),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_url UNIQUE (user_id, url)
);

-- Speeds up "list products for a user" queries
CREATE INDEX idx_products_user_id ON products(user_id);

-- Price records
-- Append-only log; never updated. price/currency are NULL for FAILED checks.
-- Deleting a product cascades to all its price records (Req 6.1).
CREATE TABLE price_records (
    id            BIGSERIAL     PRIMARY KEY,
    product_id    BIGINT        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    price         NUMERIC(15,2),
    currency      VARCHAR(10),
    checked_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    check_status  VARCHAR(10)   NOT NULL CHECK (check_status IN ('SUCCESS', 'FAILED')),
    error_message TEXT
);

-- Composite index supports the most common query: latest N records for a product
CREATE INDEX idx_price_records_product_checked
    ON price_records(product_id, checked_at DESC);

-- Price alerts
-- At most one alert per product — enforced by UNIQUE on product_id.
-- target_price must be positive (Req 12.2). Deleting a product cascades (Req 6.1).
CREATE TABLE price_alerts (
    id               BIGSERIAL     PRIMARY KEY,
    product_id       BIGINT        NOT NULL UNIQUE
                         REFERENCES products(id) ON DELETE CASCADE,
    target_price     NUMERIC(15,2) NOT NULL CHECK (target_price > 0),
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    last_notified_at TIMESTAMPTZ
);

-- Notification logs
-- Audit trail for every email send attempt (success or failure).
-- Deleting an alert cascades to its logs (Req 6.1).
CREATE TABLE notification_logs (
    id             BIGSERIAL   PRIMARY KEY,
    price_alert_id BIGINT      NOT NULL REFERENCES price_alerts(id) ON DELETE CASCADE,
    sent_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    channel        VARCHAR(10) NOT NULL DEFAULT 'EMAIL',
    success        BOOLEAN     NOT NULL,
    error_message  TEXT
);

-- Supports queries like "all notifications for a given alert"
CREATE INDEX idx_notification_logs_alert_id ON notification_logs(price_alert_id);
