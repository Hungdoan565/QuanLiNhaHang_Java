-- ==============================================
-- Kitchen Cooking Game Schema Extension
-- Version: 1.1 (fixed)
-- ==============================================

USE restaurant_db;

-- ==============================================
-- 1. COOKING_STEPS (Các bước nấu cho mỗi món)
-- ==============================================
CREATE TABLE IF NOT EXISTS cooking_steps (
    id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    step_order INT NOT NULL,
    title VARCHAR(100) NOT NULL COMMENT 'Sơ chế, Nấu, Trình bày',
    description TEXT COMMENT 'Hướng dẫn chi tiết',
    duration_seconds INT DEFAULT 0 COMMENT 'Thời gian ước tính (giây)',
    icon VARCHAR(50) DEFAULT '🔥' COMMENT 'Emoji hoặc icon name',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE KEY unique_product_step (product_id, step_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 2. ALTER ORDER_DETAILS - Thêm tracking fields
-- Chạy từng ALTER riêng để tránh lỗi nếu column đã tồn tại
-- ==============================================
-- Add current_step column
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = 'restaurant_db' 
    AND table_name = 'order_details' 
    AND column_name = 'current_step');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE order_details ADD COLUMN current_step INT DEFAULT 0 COMMENT ''Bước hiện tại đang làm''',
    'SELECT ''Column current_step already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add cooking_started_at column
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = 'restaurant_db' 
    AND table_name = 'order_details' 
    AND column_name = 'cooking_started_at');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE order_details ADD COLUMN cooking_started_at DATETIME COMMENT ''Thời điểm bắt đầu nấu''',
    'SELECT ''Column cooking_started_at already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add is_training_mode column  
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = 'restaurant_db' 
    AND table_name = 'order_details' 
    AND column_name = 'is_training_mode');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE order_details ADD COLUMN is_training_mode BOOLEAN DEFAULT FALSE COMMENT ''Order test/training''',
    'SELECT ''Column is_training_mode already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==============================================
-- 3. SAMPLE RECIPE DATA
-- Sử dụng product ID thực tế từ database
-- ==============================================
DELETE FROM recipes WHERE 1=1;

-- Insert recipes bằng cách join với products và ingredients thật
-- Ví dụ: cho mỗi sản phẩm, thêm nguyên liệu mẫu

-- Insert recipe dựa trên ingredient có sẵn
-- Mỗi product sẽ có 3-5 nguyên liệu ngẫu nhiên
INSERT INTO recipes (product_id, ingredient_id, quantity_used)
SELECT p.id, i.id, 
    CASE 
        WHEN i.unit = 'g' THEN FLOOR(50 + RAND() * 200)
        WHEN i.unit = 'ml' THEN FLOOR(20 + RAND() * 50)
        ELSE FLOOR(1 + RAND() * 5)
    END
FROM products p
CROSS JOIN (
    SELECT id, unit FROM ingredients WHERE is_active = TRUE ORDER BY RAND() LIMIT 5
) i
WHERE p.is_active = TRUE
LIMIT 100
ON DUPLICATE KEY UPDATE quantity_used = VALUES(quantity_used);

-- ==============================================
-- 4. SAMPLE COOKING STEPS
-- Thêm 3 bước cơ bản cho mỗi sản phẩm có sẵn
-- ==============================================
DELETE FROM cooking_steps WHERE 1=1;

-- Bước 1: Sơ chế cho tất cả sản phẩm
INSERT INTO cooking_steps (product_id, step_order, title, description, duration_seconds, icon)
SELECT id, 1, 'Sơ chế', 'Chuẩn bị nguyên liệu, cắt thái, ướp gia vị', 180, '🔪'
FROM products WHERE is_active = TRUE;

-- Bước 2: Nấu/Chế biến
INSERT INTO cooking_steps (product_id, step_order, title, description, duration_seconds, icon)
SELECT id, 2, 'Chế biến', 'Nấu, xào, chiên hoặc hấp theo công thức', 600, '🔥'
FROM products WHERE is_active = TRUE;

-- Bước 3: Trình bày
INSERT INTO cooking_steps (product_id, step_order, title, description, duration_seconds, icon)
SELECT id, 3, 'Trình bày', 'Xếp đĩa, trang trí và hoàn thiện món ăn', 60, '🍽️'
FROM products WHERE is_active = TRUE;

-- ==============================================
-- 5. INDEXES
-- ==============================================
CREATE INDEX IF NOT EXISTS idx_cooking_steps_product ON cooking_steps(product_id);

-- ==============================================
-- 6. ADD TRAINING MODE SETTING
-- ==============================================
INSERT INTO settings (setting_key, setting_value, setting_type, description)
VALUES ('kitchen_training_mode', 'false', 'BOOLEAN', 'Chế độ training - không trừ kho khi nấu')
ON DUPLICATE KEY UPDATE setting_value = setting_value;

-- ==============================================
-- 7. VERIFY
-- ==============================================
SELECT 'Cooking steps created:' AS info, COUNT(*) AS count FROM cooking_steps;
SELECT 'Recipes created:' AS info, COUNT(*) AS count FROM recipes;
SELECT 'Training mode setting:' AS info, setting_value FROM settings WHERE setting_key = 'kitchen_training_mode';

-- ==============================================
-- End of Cooking Game Schema
-- ==============================================
