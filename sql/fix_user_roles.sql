-- ==============================================
-- FIX: Correct user-role mapping
-- Problem: Role IDs might be out of order
-- ==============================================

USE restaurant_db;

-- 1. First check current roles
SELECT id, name FROM roles ORDER BY id;

-- 2. Fix by updating users with correct role_id based on role name
UPDATE users u
JOIN roles r ON r.name = 'ADMIN'
SET u.role_id = r.id
WHERE u.username = 'admin';

UPDATE users u
JOIN roles r ON r.name = 'MANAGER'
SET u.role_id = r.id
WHERE u.username = 'manager';

UPDATE users u
JOIN roles r ON r.name = 'CASHIER'
SET u.role_id = r.id
WHERE u.username = 'cashier1';

UPDATE users u
JOIN roles r ON r.name = 'WAITER'
SET u.role_id = r.id
WHERE u.username = 'waiter1';

UPDATE users u
JOIN roles r ON r.name = 'CHEF'
SET u.role_id = r.id
WHERE u.username = 'chef1';

-- 3. Verify fix
SELECT u.username, u.full_name, r.name as role_name, r.id as role_id
FROM users u
JOIN roles r ON u.role_id = r.id
ORDER BY r.id;

SELECT '✅ User roles fixed!' AS status;
