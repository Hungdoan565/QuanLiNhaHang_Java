-- ==============================================
-- RestaurantPOS Seed Data
-- Sample data for development/testing
-- ==============================================

USE restaurant_db;

-- ==============================================
-- 1. ROLES
-- ==============================================
INSERT INTO roles (name, description, permissions) VALUES
('ADMIN', 'Quản lý toàn quyền hệ thống', '{"all": true}'),
('CASHIER', 'Thu ngân - Thanh toán, chốt ca', '{"pos": true, "billing": true, "shift": true}'),
('WAITER', 'Phục vụ - Order món, quản lý bàn', '{"pos": true, "table": true, "order": true}'),
('CHEF', 'Bếp - Xem và xử lý order', '{"kitchen": true}');

-- ==============================================
-- 2. USERS (password: 123456)
-- BCrypt hash for "123456": $2a$12$LQv3c1yqBWVHxkd0LHAkCeYKjCLnBJmO2TXWP6KpaR1wVh0vqFbGi
-- ==============================================
INSERT INTO users (username, password_hash, full_name, phone, role_id, is_active) VALUES
('admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCeYKjCLnBJmO2TXWP6KpaR1wVh0vqFbGi', 'Administrator', '0901234567', 1, TRUE),
('casher01', '$2a$12$LQv3c1yqBWVHxkd0LHAkCeYKjCLnBJmO2TXWP6KpaR1wVh0vqFbGi', 'Nguyễn Thu Ngân', '0901234568', 2, TRUE),
('waiter01', '$2a$12$LQv3c1yqBWVHxkd0LHAkCeYKjCLnBJmO2TXWP6KpaR1wVh0vqFbGi', 'Trần Phục Vụ', '0901234569', 3, TRUE),
('chef01', '$2a$12$LQv3c1yqBWVHxkd0LHAkCeYKjCLnBJmO2TXWP6KpaR1wVh0vqFbGi', 'Lê Đầu Bếp', '0901234570', 4, TRUE);

-- ==============================================
-- 3. TABLES (Bàn ăn)
-- ==============================================
INSERT INTO tables (name, capacity, area, position_x, position_y, status) VALUES
-- Tầng 1
('Bàn 1', 4, 'Tầng 1', 50, 50, 'AVAILABLE'),
('Bàn 2', 4, 'Tầng 1', 150, 50, 'AVAILABLE'),
('Bàn 3', 4, 'Tầng 1', 250, 50, 'AVAILABLE'),
('Bàn 4', 4, 'Tầng 1', 350, 50, 'AVAILABLE'),
('Bàn 5', 6, 'Tầng 1', 50, 150, 'AVAILABLE'),
('Bàn 6', 6, 'Tầng 1', 150, 150, 'AVAILABLE'),
('Bàn 7', 2, 'Tầng 1', 250, 150, 'AVAILABLE'),
('Bàn 8', 2, 'Tầng 1', 350, 150, 'AVAILABLE'),
-- Phòng VIP
('VIP 1', 10, 'Phòng VIP', 50, 50, 'AVAILABLE'),
('VIP 2', 12, 'Phòng VIP', 200, 50, 'AVAILABLE'),
-- Sân vườn
('Sân 1', 4, 'Sân vườn', 50, 50, 'AVAILABLE'),
('Sân 2', 4, 'Sân vườn', 150, 50, 'AVAILABLE');

-- ==============================================
-- 4. CATEGORIES (Danh mục)
-- ==============================================
INSERT INTO categories (name, icon, display_order, printer_name) VALUES
('Đồ uống', '🥤', 1, 'Bar_Printer'),
('Bia & Rượu', '🍺', 2, 'Bar_Printer'),
('Khai vị', '🥗', 3, 'Kitchen_Printer'),
('Món chính', '🍲', 4, 'Kitchen_Printer'),
('Lẩu', '🍜', 5, 'Kitchen_Printer'),
('Hải sản', '🦐', 6, 'Kitchen_Printer'),
('Tráng miệng', '🍰', 7, 'Kitchen_Printer'),
('Combo', '🎁', 8, 'Kitchen_Printer');

-- ==============================================
-- 5. PRODUCTS (Món ăn)
-- ==============================================
INSERT INTO products (name, description, category_id, price, cost_price, is_available) VALUES
-- Đồ uống
('Cà phê sữa đá', 'Cà phê pha phin truyền thống với sữa đặc', 1, 25000, 8000, TRUE),
('Cà phê đen đá', 'Cà phê đen nguyên chất', 1, 20000, 6000, TRUE),
('Trà đào cam sả', 'Trà đào tươi với cam và sả', 1, 35000, 12000, TRUE),
('Nước ép cam', 'Cam tươi nguyên chất', 1, 30000, 10000, TRUE),
('Sinh tố bơ', 'Bơ sáp xay nhuyễn với sữa', 1, 40000, 15000, TRUE),
('Nước suối', 'Nước khoáng đóng chai', 1, 10000, 3000, TRUE),

-- Bia & Rượu
('Bia Tiger', 'Bia Tiger lon 330ml', 2, 25000, 15000, TRUE),
('Bia Heineken', 'Bia Heineken lon 330ml', 2, 30000, 18000, TRUE),
('Bia Sài Gòn', 'Bia Sài Gòn chai 450ml', 2, 20000, 12000, TRUE),

-- Khai vị
('Gỏi cuốn', 'Gỏi cuốn tôm thịt (2 cuốn)', 3, 35000, 15000, TRUE),
('Chả giò', 'Chả giò chiên giòn (4 cuốn)', 3, 40000, 18000, TRUE),
('Salad trộn', 'Salad rau củ với sốt mè rang', 3, 45000, 20000, TRUE),

-- Món chính
('Phở bò tái', 'Phở bò tái nạm gầu', 4, 55000, 25000, TRUE),
('Cơm rang dương châu', 'Cơm chiên với tôm, trứng, lạp xưởng', 4, 50000, 22000, TRUE),
('Bún bò Huế', 'Bún bò Huế đặc biệt', 4, 55000, 25000, TRUE),
('Cơm tấm sườn bì', 'Cơm tấm sườn nướng, bì, chả', 4, 55000, 25000, TRUE),
('Mì xào hải sản', 'Mì xào với tôm, mực, nghêu', 4, 65000, 30000, TRUE),

-- Lẩu
('Lẩu thái hải sản', 'Lẩu chua cay kiểu Thái', 5, 350000, 150000, TRUE),
('Lẩu gà lá é', 'Lẩu gà nấu với lá é', 5, 280000, 120000, TRUE),

-- Hải sản
('Tôm sú nướng muối ớt', 'Tôm sú tươi nướng (500g)', 6, 250000, 150000, TRUE),
('Cua rang me', 'Cua biển rang với sốt me', 6, 350000, 200000, TRUE),
('Mực chiên giòn', 'Mực tươi chiên giòn', 6, 180000, 100000, TRUE),

-- Tráng miệng
('Chè thái', 'Chè thái với nhiều topping', 7, 30000, 12000, TRUE),
('Bánh flan', 'Bánh flan caramen', 7, 25000, 10000, TRUE),
('Kem dừa', 'Kem dừa tươi', 7, 35000, 15000, TRUE);

-- ==============================================
-- 6. INGREDIENTS (Nguyên liệu mẫu)
-- ==============================================
INSERT INTO ingredients (name, unit, quantity, min_quantity, cost_per_unit) VALUES
('Cà phê hạt', 'g', 5000, 500, 0.3),
('Sữa đặc', 'ml', 10000, 1000, 0.05),
('Đường', 'g', 10000, 1000, 0.02),
('Đá viên', 'g', 50000, 5000, 0.01),
('Thịt bò', 'g', 10000, 1000, 0.25),
('Tôm sú', 'g', 5000, 500, 0.35),
('Gạo', 'g', 50000, 5000, 0.02),
('Rau xà lách', 'g', 2000, 200, 0.03),
('Bánh phở', 'g', 5000, 500, 0.04);

-- ==============================================
-- 7. RECIPES (Định lượng mẫu)
-- ==============================================
INSERT INTO recipes (product_id, ingredient_id, quantity_used) VALUES
-- Cà phê sữa đá
(1, 1, 20),   -- 20g cà phê
(1, 2, 30),   -- 30ml sữa đặc
(1, 4, 100),  -- 100g đá

-- Cà phê đen đá
(2, 1, 20),   -- 20g cà phê
(2, 4, 100),  -- 100g đá

-- Phở bò tái
(13, 5, 150), -- 150g thịt bò
(13, 9, 200); -- 200g bánh phở

-- ==============================================
-- End of Seed Data
-- ==============================================
