-- ==============================================
-- RestaurantPOS - Schema Update & Seed Data
-- Run this after schema.sql
-- ==============================================

USE restaurant_db;

-- ==============================================
-- UPDATE TABLES TABLE
-- Thêm các cột cần thiết cho POS
-- ==============================================
ALTER TABLE tables 
    ADD COLUMN IF NOT EXISTS current_order_id INT NULL COMMENT 'Order hiện tại đang phục vụ',
    ADD COLUMN IF NOT EXISTS guest_count INT DEFAULT 0 COMMENT 'Số khách hiện tại',
    ADD COLUMN IF NOT EXISTS occupied_since DATETIME NULL COMMENT 'Thời điểm bắt đầu phục vụ',
    ADD COLUMN IF NOT EXISTS updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP;

-- Update columns for categories
ALTER TABLE categories 
    ADD COLUMN IF NOT EXISTS updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP;

-- ==============================================
-- CLEAN OLD DATA (để tránh trùng lặp)
-- ==============================================
DELETE FROM products WHERE TRUE;
DELETE FROM categories WHERE TRUE;

-- ==============================================
-- SEED DATA - ROLES
-- ==============================================
INSERT INTO roles (name, description, permissions) VALUES
('ADMIN', 'Quản trị viên - Toàn quyền', '["*"]'),
('MANAGER', 'Quản lý - Quản lý nhân viên, báo cáo', '["dashboard","pos","menu","inventory","staff","reports","settings"]'),
('CASHIER', 'Thu ngân - POS, thanh toán', '["dashboard","pos"]'),
('WAITER', 'Phục vụ - Gọi món, phục vụ bàn', '["pos"]'),
('CHEF', 'Đầu bếp - Xem đơn bếp', '["kitchen"]')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- ==============================================
-- SEED DATA - USERS
-- Password: 123456 (bcrypt hash)
-- ==============================================
INSERT INTO users (username, password_hash, full_name, phone, email, role_id, is_active) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'Nguyễn Văn Admin', '0901234567', 'admin@restaurant.com', 1, TRUE),
('manager', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'Trần Thị Manager', '0902234567', 'manager@restaurant.com', 2, TRUE),
('cashier1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'Lê Văn Thu Ngân', '0903234567', 'cashier@restaurant.com', 3, TRUE),
('waiter1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'Phạm Thị Phục Vụ', '0904234567', 'waiter@restaurant.com', 4, TRUE),
('chef1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'Ngô Văn Bếp', '0905234567', 'chef@restaurant.com', 5, TRUE)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- ==============================================
-- SEED DATA - CATEGORIES (Đã sửa - không trùng)
-- ==============================================
INSERT INTO categories (id, name, icon, display_order, printer_name, is_active) VALUES
(1, 'Đồ uống', '🥤', 1, 'Bar_Printer', TRUE),
(2, 'Khai vị', '🥗', 2, 'Kitchen_Printer', TRUE),
(3, 'Món chính', '🍲', 3, 'Kitchen_Printer', TRUE),
(4, 'Lẩu', '🍲', 4, 'Kitchen_Printer', TRUE),
(5, 'Hải sản', '🦐', 5, 'Kitchen_Printer', TRUE),
(6, 'Tráng miệng', '🍰', 6, 'Kitchen_Printer', TRUE),
(7, 'Bia & Rượu', '🍺', 7, 'Bar_Printer', TRUE);

-- ==============================================
-- SEED DATA - PRODUCTS (Đã sửa category)
-- ==============================================

-- Đồ uống (category_id = 1)
INSERT INTO products (name, description, category_id, price, cost_price, is_available, is_active) VALUES
('Cà phê sữa đá', 'Cà phê pha phin với sữa đặc', 1, 29000, 8000, TRUE, TRUE),
('Trà đào cam sả', 'Trà với đào, cam và sả thơm', 1, 35000, 10000, TRUE, TRUE),
('Sinh tố bơ', 'Sinh tố bơ sáp béo ngậy', 1, 45000, 15000, TRUE, TRUE),
('Nước ép cam', 'Cam tươi ép nguyên chất', 1, 40000, 12000, TRUE, TRUE),
('Coca Cola', 'Lon 330ml', 1, 20000, 12000, TRUE, TRUE),
('Trà sữa trân châu', 'Trà sữa với trân châu đen', 1, 35000, 10000, TRUE, TRUE),
('Nước suối', 'Chai 500ml', 1, 12000, 5000, TRUE, TRUE);

-- Khai vị (category_id = 2)
INSERT INTO products (name, description, category_id, price, cost_price, is_available, is_active) VALUES
('Gỏi cuốn', 'Gỏi cuốn tôm thịt (2 cuốn)', 2, 35000, 12000, TRUE, TRUE),
('Chả giò chiên', 'Chả giò giòn (4 cái)', 2, 45000, 15000, TRUE, TRUE),
('Súp bào ngư', 'Súp bào ngư hải sản', 2, 85000, 40000, TRUE, TRUE),
('Salad trộn', 'Salad rau củ tươi', 2, 55000, 18000, TRUE, TRUE),
('Khoai tây chiên', 'Khoai tây chiên giòn', 2, 35000, 10000, TRUE, TRUE),
('Đậu hũ chiên', 'Đậu hũ chiên giòn sốt cay', 2, 40000, 12000, TRUE, TRUE);

-- Món chính (category_id = 3)
INSERT INTO products (name, description, category_id, price, cost_price, is_available, is_active) VALUES
('Phở bò tái', 'Phở bò với thịt bò tái, nạm', 3, 55000, 18000, TRUE, TRUE),
('Bún bò Huế', 'Bún bò Huế cay đặc trưng', 3, 60000, 22000, TRUE, TRUE),
('Cơm rang dương châu', 'Cơm rang với tôm, lạp xưởng', 3, 65000, 20000, TRUE, TRUE),
('Cơm sườn nướng', 'Cơm với sườn nướng mật ong', 3, 75000, 28000, TRUE, TRUE),
('Mì xào hải sản', 'Mì xào với tôm, mực, nghêu', 3, 85000, 35000, TRUE, TRUE),
('Cá kho tộ', 'Cá lóc kho tộ đậm đà', 3, 120000, 50000, TRUE, TRUE),
('Gà nướng mật ong', 'Đùi gà nướng mật ong', 3, 95000, 40000, TRUE, TRUE);

-- Lẩu (category_id = 4)
INSERT INTO products (name, description, category_id, price, cost_price, is_available, is_active) VALUES
('Lẩu gà lá é', 'Lẩu gà thơm lá é (2-3 người)', 4, 280000, 120000, TRUE, TRUE),
('Lẩu bò', 'Lẩu bò nhúng (2-3 người)', 4, 320000, 140000, TRUE, TRUE),
('Lẩu thái', 'Lẩu thái chua cay (2-3 người)', 4, 350000, 150000, TRUE, TRUE),
('Lẩu hải sản', 'Lẩu hải sản tươi sống (2-3 người)', 4, 450000, 200000, TRUE, TRUE),
('Lẩu nấm', 'Lẩu nấm chay (2-3 người)', 4, 250000, 100000, TRUE, TRUE);

-- Hải sản (category_id = 5)
INSERT INTO products (name, description, category_id, price, cost_price, is_available, is_active) VALUES
('Tôm hùm nướng', 'Tôm hùm nướng bơ tỏi (1 con)', 5, 850000, 500000, TRUE, TRUE),
('Cua rang me', 'Cua biển rang me (1kg)', 5, 650000, 350000, TRUE, TRUE),
('Ghẹ hấp', 'Ghẹ hấp sả (1kg)', 5, 450000, 250000, TRUE, TRUE),
('Mực nướng', 'Mực nướng sa tế', 5, 180000, 80000, TRUE, TRUE),
('Nghêu hấp xả', 'Nghêu hấp xả ớt', 5, 120000, 50000, TRUE, TRUE),
('Ốc hương', 'Ốc hương xào tỏi (500g)', 5, 220000, 100000, TRUE, TRUE);

-- Tráng miệng (category_id = 6)
INSERT INTO products (name, description, category_id, price, cost_price, is_available, is_active) VALUES
('Chè thái', 'Chè với hoa quả nhiệt đới', 6, 35000, 10000, TRUE, TRUE),
('Bánh flan', 'Bánh flan caramel mềm mịn', 6, 25000, 8000, TRUE, TRUE),
('Kem dừa', 'Kem dừa thơm béo', 6, 30000, 10000, TRUE, TRUE),
('Trái cây thập cẩm', 'Đĩa trái cây tươi', 6, 55000, 20000, TRUE, TRUE),
('Sữa chua dẻo', 'Sữa chua dẻo mát lạnh', 6, 20000, 6000, TRUE, TRUE);

-- Bia & Rượu (category_id = 7)
INSERT INTO products (name, description, category_id, price, cost_price, is_available, is_active) VALUES
('Bia Tiger', 'Lon 330ml', 7, 25000, 15000, TRUE, TRUE),
('Bia Heineken', 'Lon 330ml', 7, 30000, 18000, TRUE, TRUE),
('Bia Sài Gòn', 'Lon 330ml', 7, 22000, 13000, TRUE, TRUE),
('Rượu vang đỏ', 'Chai 750ml (Chile)', 7, 350000, 180000, TRUE, TRUE),
('Rượu vang trắng', 'Chai 750ml (Italy)', 7, 380000, 200000, TRUE, TRUE),
('Whisky Chivas', '1 ly', 7, 120000, 60000, TRUE, TRUE);

-- ==============================================
-- SEED DATA - TABLES
-- ==============================================
-- Sân (outdoor)
INSERT INTO tables (name, capacity, status, area, position_x, position_y, is_active) VALUES
('Sân 1', 4, 'AVAILABLE', 'Tầng 1', 0, 0, TRUE),
('Sân 2', 4, 'AVAILABLE', 'Tầng 1', 1, 0, TRUE)
ON DUPLICATE KEY UPDATE capacity = VALUES(capacity);

-- Tầng 1
INSERT INTO tables (name, capacity, status, area, position_x, position_y, is_active) VALUES
('Bàn 01', 4, 'AVAILABLE', 'Tầng 1', 0, 1, TRUE),
('Bàn 02', 4, 'AVAILABLE', 'Tầng 1', 1, 1, TRUE),
('Bàn 03', 4, 'AVAILABLE', 'Tầng 1', 2, 1, TRUE),
('Bàn 04', 4, 'AVAILABLE', 'Tầng 1', 0, 2, TRUE),
('Bàn 05', 4, 'AVAILABLE', 'Tầng 1', 1, 2, TRUE),
('Bàn 06', 4, 'AVAILABLE', 'Tầng 1', 2, 2, TRUE),
('Bàn 07', 6, 'AVAILABLE', 'Tầng 1', 0, 3, TRUE),
('Bàn 08', 6, 'AVAILABLE', 'Tầng 1', 1, 3, TRUE)
ON DUPLICATE KEY UPDATE capacity = VALUES(capacity);

-- Tầng 2
INSERT INTO tables (name, capacity, status, area, position_x, position_y, is_active) VALUES
('Bàn 09', 6, 'AVAILABLE', 'Tầng 2', 0, 0, TRUE),
('Bàn 10', 6, 'AVAILABLE', 'Tầng 2', 1, 0, TRUE),
('Bàn 11', 8, 'AVAILABLE', 'Tầng 2', 0, 1, TRUE),
('Bàn 12', 8, 'AVAILABLE', 'Tầng 2', 1, 1, TRUE)
ON DUPLICATE KEY UPDATE capacity = VALUES(capacity);

-- Phòng VIP
INSERT INTO tables (name, capacity, status, area, position_x, position_y, is_active) VALUES
('VIP 01', 10, 'AVAILABLE', 'Phòng VIP', 0, 0, TRUE),
('VIP 02', 10, 'AVAILABLE', 'Phòng VIP', 1, 0, TRUE),
('VIP 03', 12, 'AVAILABLE', 'Phòng VIP', 0, 1, TRUE)
ON DUPLICATE KEY UPDATE capacity = VALUES(capacity);

-- ==============================================
-- SEED DATA - INGREDIENTS (Nguyên liệu kho)
-- ==============================================
INSERT INTO ingredients (name, unit, quantity, min_quantity, cost_per_unit, is_active) VALUES
('Cà phê hạt rang', 'kg', 10.0, 2.0, 350000, TRUE),
('Sữa tươi', 'lít', 20.0, 10.0, 32000, TRUE),
('Đường', 'kg', 15.0, 3.0, 22000, TRUE),
('Bột mì', 'kg', 8.0, 5.0, 18000, TRUE),
('Trứng gà', 'quả', 200, 50, 3500, TRUE),
('Thịt bò', 'kg', 5.0, 2.0, 280000, TRUE),
('Thịt heo', 'kg', 8.0, 3.0, 120000, TRUE),
('Tôm', 'kg', 3.0, 1.0, 250000, TRUE),
('Rau xà lách', 'kg', 5.0, 1.0, 25000, TRUE),
('Nước mắm', 'lít', 8.0, 2.0, 45000, TRUE),
('Dầu ăn', 'lít', 10.0, 3.0, 35000, TRUE),
('Gạo', 'kg', 25.0, 10.0, 18000, TRUE)
ON DUPLICATE KEY UPDATE quantity = VALUES(quantity);

-- ==============================================
-- DONE
-- ==============================================
SELECT '✅ Seed data imported successfully!' AS status;
SELECT 
    (SELECT COUNT(*) FROM roles) as roles_count,
    (SELECT COUNT(*) FROM users) as users_count,
    (SELECT COUNT(*) FROM categories) as categories_count,
    (SELECT COUNT(*) FROM products) as products_count,
    (SELECT COUNT(*) FROM tables) as tables_count,
    (SELECT COUNT(*) FROM ingredients) as ingredients_count;
