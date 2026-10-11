-- =========================================================
-- V4__add_hidden_to_items.sql
-- Add hidden flag to items table for community moderation
-- =========================================================

ALTER TABLE items ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE;
