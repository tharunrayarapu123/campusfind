-- =========================================================
-- V5__claims_audit_settings.sql
-- Add email_notifications to users, is_office_claim to claims,
-- and create audit_logs table.
-- =========================================================

-- 1. Add email notifications preference to users
ALTER TABLE users ADD COLUMN email_notifications BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. Add is_office_claim flag to claims
ALTER TABLE claims ADD COLUMN is_office_claim BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. Create audit_logs table for admin tracking
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT,
    details TEXT,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_audit_logs_admin FOREIGN KEY (admin_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_logs_admin ON audit_logs (admin_id, created_at);
