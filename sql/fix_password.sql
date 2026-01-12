-- FIX: Update password hash for all users
-- Password: 123456
-- This hash was generated using BCrypt with cost factor 12

UPDATE users SET password_hash = '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.' WHERE username = 'admin';
UPDATE users SET password_hash = '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.' WHERE username = 'casher01';
UPDATE users SET password_hash = '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.' WHERE username = 'waiter01';
UPDATE users SET password_hash = '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.' WHERE username = 'chef01';

-- Or update all at once:
-- UPDATE users SET password_hash = '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.';
