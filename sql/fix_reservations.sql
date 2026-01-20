-- Fix reservations table to match Java model
-- Run this script if you get "Column 'notified' not found" error

USE restaurant_db;

-- Add notified column if not exists
SET @dbname = DATABASE();
SET @tablename = 'reservations';
SET @columnname = 'notified';
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) = 0,
    CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN notified BOOLEAN DEFAULT FALSE'),
    'SELECT 1'
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Update status enum to match Java model if needed
-- First check current enum values
-- If your status column uses SEATED instead of ARRIVED, you may need to update:
-- ALTER TABLE reservations MODIFY COLUMN status ENUM('PENDING', 'CONFIRMED', 'ARRIVED', 'CANCELLED', 'NO_SHOW', 'SEATED', 'COMPLETED') DEFAULT 'PENDING';

-- Update any SEATED status to ARRIVED for consistency
UPDATE reservations SET status = 'ARRIVED' WHERE status = 'SEATED';

SELECT 'Reservations table fixed!' AS message;
