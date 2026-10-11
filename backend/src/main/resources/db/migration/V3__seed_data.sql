-- =========================================================
-- V3__seed_data.sql
-- Seed Initial Campus Blocks, Rooms, and Default Admin
-- =========================================================

-- 1. Insert Blocks
INSERT INTO blocks (id, name) VALUES
(1, 'Block A'),
(2, 'Block B'),
(3, 'Block C'),
(4, 'Library'),
(5, 'Canteen');

-- 2. Insert 5 Rooms for Each Block
-- Block A
INSERT INTO rooms (block_id, name) VALUES
(1, 'Room 101'),
(1, 'Room 102'),
(1, 'Room 103'),
(1, 'Room 104'),
(1, 'Room 105');

-- Block B
INSERT INTO rooms (block_id, name) VALUES
(2, 'Room 201'),
(2, 'Room 202'),
(2, 'Room 203'),
(2, 'Room 204'),
(2, 'Room 205');

-- Block C
INSERT INTO rooms (block_id, name) VALUES
(3, 'Room 301'),
(3, 'Room 302'),
(3, 'Room 303'),
(3, 'Room 304'),
(3, 'Room 305');

-- Library
INSERT INTO rooms (block_id, name) VALUES
(4, 'Reading Hall'),
(4, 'Periodicals Section'),
(4, 'Reference Hall'),
(4, 'Digital Lab'),
(4, 'Discussion Room');

-- Canteen
INSERT INTO rooms (block_id, name) VALUES
(5, 'Main Dining Area'),
(5, 'Food Court A'),
(5, 'Food Court B'),
(5, 'Cafe Counter'),
(5, 'Faculty Lounge');

-- 3. Insert Admin User (Password is 'Admin@123' hashed with BCrypt)
INSERT INTO users (full_name, email, password_hash, department, role, karma_points, banned, created_at)
VALUES (
    'System Administrator',
    'admin@college.edu',
    '$2a$10$32D3/bzLGBa4ISzZ3n9ifOYoAuuMT9dkVUBkf4R3o/9Uy.CaMMGhG',
    'Campus Administration',
    'ADMIN',
    100,
    FALSE,
    CURRENT_TIMESTAMP(6)
);
