-- ==============================================
-- RestaurantPOS Database Schema
-- Version: 1.0
-- MySQL 8.0+
-- ==============================================

-- Tạo Database
CREATE DATABASE IF NOT EXISTS restaurant_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE restaurant_db;

-- ==============================================
-- 1. ROLES TABLE
-- ==============================================
CREATE TABLE IF NOT EXISTS roles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    permissions JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 2. USERS TABLE
-- ==============================================
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    avatar_path VARCHAR(255),
    role_id INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    last_login DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 3. SHIFTS TABLE (Ca làm việc)
-- ==============================================
CREATE TABLE IF NOT EXISTS shifts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    start_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    end_time DATETIME,
    start_cash DECIMAL(15,2) DEFAULT 0 COMMENT 'Tiền đầu ca (floating)',
    end_cash DECIMAL(15,2) DEFAULT 0 COMMENT 'Tiền thực tế đếm được',
    total_sales DECIMAL(15,2) DEFAULT 0 COMMENT 'Tổng doanh thu ca',
    cash_sales DECIMAL(15,2) DEFAULT 0 COMMENT 'Doanh thu tiền mặt',
    card_sales DECIMAL(15,2) DEFAULT 0 COMMENT 'Doanh thu thẻ',
    transfer_sales DECIMAL(15,2) DEFAULT 0 COMMENT 'Doanh thu chuyển khoản',
    variance DECIMAL(15,2) DEFAULT 0 COMMENT 'Chênh lệch (end_cash - expected)',
    status ENUM('OPEN', 'CLOSED') DEFAULT 'OPEN',
    note TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 4. TABLES (Bàn ăn)
-- ==============================================
CREATE TABLE IF NOT EXISTS tables (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL UNIQUE,
    capacity INT DEFAULT 4,
    status ENUM('AVAILABLE', 'OCCUPIED', 'RESERVED', 'CLEANING') DEFAULT 'AVAILABLE',
    area VARCHAR(50) COMMENT 'Khu vực: Tầng 1, VIP, Sân vườn...',
    position_x INT DEFAULT 0,
    position_y INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 5. CATEGORIES (Danh mục món)
-- ==============================================
CREATE TABLE IF NOT EXISTS categories (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    icon VARCHAR(50) COMMENT 'Icon emoji hoặc tên file',
    display_order INT DEFAULT 0,
    printer_name VARCHAR(50) DEFAULT 'Kitchen_Printer' COMMENT 'Tên máy in gắn với danh mục',
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 6. PRODUCTS (Món ăn / Sản phẩm)
-- ==============================================
CREATE TABLE IF NOT EXISTS products (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id INT,
    price DECIMAL(15,2) NOT NULL DEFAULT 0,
    cost_price DECIMAL(15,2) DEFAULT 0 COMMENT 'Giá vốn',
    image_path VARCHAR(255),
    is_available BOOLEAN DEFAULT TRUE COMMENT 'Còn hàng / Hết hàng',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Hiển thị trên menu',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 7. ORDERS (Đơn hàng / Hóa đơn)
-- ==============================================
CREATE TABLE IF NOT EXISTS orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_code VARCHAR(20) NOT NULL UNIQUE,
    table_id INT,
    user_id INT COMMENT 'Nhân viên tạo order',
    shift_id INT COMMENT 'Ca làm việc',
    guest_count INT DEFAULT 1 COMMENT 'Số lượng khách',
    status ENUM('OPEN', 'COMPLETED', 'CANCELLED') DEFAULT 'OPEN',
    subtotal DECIMAL(15,2) DEFAULT 0,
    discount_percent DECIMAL(5,2) DEFAULT 0,
    discount_amount DECIMAL(15,2) DEFAULT 0,
    tax_percent DECIMAL(5,2) DEFAULT 0,
    tax_amount DECIMAL(15,2) DEFAULT 0,
    service_charge DECIMAL(15,2) DEFAULT 0,
    total_amount DECIMAL(15,2) DEFAULT 0,
    notes TEXT,
    cancel_reason VARCHAR(255),
    cancelled_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    completed_at DATETIME,
    FOREIGN KEY (table_id) REFERENCES tables(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE SET NULL,
    FOREIGN KEY (cancelled_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 8. ORDER_DETAILS (Chi tiết đơn hàng)
-- ==============================================
CREATE TABLE IF NOT EXISTS order_details (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT DEFAULT 1,
    original_price DECIMAL(15,2) NOT NULL COMMENT 'Giá gốc món',
    unit_price DECIMAL(15,2) NOT NULL COMMENT 'Giá sau khi + topping/modifier',
    subtotal DECIMAL(15,2) NOT NULL,
    modifiers JSON COMMENT '[{"name":"Size L","price":10000}]',
    notes VARCHAR(255) COMMENT 'Ghi chú: ít cay, không hành...',
    status ENUM('PENDING', 'COOKING', 'READY', 'SERVED', 'CANCELLED') DEFAULT 'PENDING',
    sent_to_kitchen_at DATETIME,
    completed_at DATETIME,
    cancelled_by INT,
    cancel_reason VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    FOREIGN KEY (cancelled_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 9. INGREDIENTS (Nguyên liệu)
-- ==============================================
CREATE TABLE IF NOT EXISTS ingredients (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(20) NOT NULL COMMENT 'Đơn vị cơ bản: g, ml, pcs',
    quantity DECIMAL(15,3) DEFAULT 0 COMMENT 'Số lượng tồn kho',
    min_quantity DECIMAL(15,3) DEFAULT 0 COMMENT 'Mức cảnh báo thấp',
    cost_per_unit DECIMAL(15,2) DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 10. RECIPES (Định lượng nguyên liệu)
-- ==============================================
CREATE TABLE IF NOT EXISTS recipes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    ingredient_id INT NOT NULL,
    quantity_used DECIMAL(15,3) NOT NULL COMMENT 'Lượng nguyên liệu cần cho 1 sản phẩm',
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id) ON DELETE RESTRICT,
    UNIQUE KEY unique_recipe (product_id, ingredient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 11. PAYMENTS (Thanh toán)
-- ==============================================
CREATE TABLE IF NOT EXISTS payments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    method ENUM('CASH', 'TRANSFER', 'CARD') NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    amount_received DECIMAL(15,2) COMMENT 'Số tiền khách đưa (tiền mặt)',
    change_amount DECIMAL(15,2) COMMENT 'Tiền thối',
    transaction_code VARCHAR(50) COMMENT 'Mã giao dịch ngân hàng',
    status ENUM('PENDING', 'COMPLETED', 'REFUNDED') DEFAULT 'COMPLETED',
    user_id INT COMMENT 'Nhân viên thu tiền',
    note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 12. AUDIT_LOGS (Nhật ký hệ thống)
-- ==============================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    action VARCHAR(50) NOT NULL COMMENT 'LOGIN, LOGOUT, CREATE, UPDATE, DELETE, CANCEL, etc.',
    table_name VARCHAR(50),
    record_id INT,
    old_values JSON,
    new_values JSON,
    ip_address VARCHAR(45),
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 13. RESERVATIONS (Đặt bàn trước)
-- ==============================================
CREATE TABLE IF NOT EXISTS reservations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    table_id INT NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20),
    guest_count INT DEFAULT 2,
    reservation_time DATETIME NOT NULL,
    duration_minutes INT DEFAULT 120 COMMENT 'Thời gian dự kiến (phút)',
    status ENUM('PENDING', 'CONFIRMED', 'SEATED', 'COMPLETED', 'CANCELLED', 'NO_SHOW') DEFAULT 'PENDING',
    notes TEXT,
    created_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (table_id) REFERENCES tables(id) ON DELETE RESTRICT,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- INDEXES for Performance
-- ==============================================
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role_id);
CREATE INDEX idx_shifts_user ON shifts(user_id);
CREATE INDEX idx_shifts_status ON shifts(status);
CREATE INDEX idx_orders_table ON orders(table_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_shift ON orders(shift_id);
CREATE INDEX idx_orders_created ON orders(created_at);
CREATE INDEX idx_order_details_order ON order_details(order_id);
CREATE INDEX idx_order_details_status ON order_details(status);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_active ON products(is_active, is_available);
CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
CREATE INDEX idx_reservations_table ON reservations(table_id);
CREATE INDEX idx_reservations_time ON reservations(reservation_time);

-- ==============================================
-- End of Schema
-- ==============================================

-- ==============================================
-- 14. MODIFIER_GROUPS (Nhóm tùy chọn: Size, Topping...)
-- ==============================================
CREATE TABLE IF NOT EXISTS modifier_groups (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT 'Tên nhóm: Chọn Size, Topping, Mức đá',
    selection_type ENUM('SINGLE', 'MULTIPLE') DEFAULT 'SINGLE' COMMENT 'Chọn 1 hoặc nhiều',
    is_required BOOLEAN DEFAULT FALSE COMMENT 'Bắt buộc phải chọn?',
    max_selections INT DEFAULT NULL COMMENT 'Giới hạn số lượng chọn (NULL = không giới hạn)',
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 15. MODIFIERS (Chi tiết tùy chọn)
-- ==============================================
CREATE TABLE IF NOT EXISTS modifiers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    group_id INT NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT 'Size S, Size M, Thêm trân châu',
    price_adjustment DECIMAL(15,2) DEFAULT 0 COMMENT 'Giá cộng thêm: +5000, +10000',
    is_default BOOLEAN DEFAULT FALSE COMMENT 'Option mặc định',
    display_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (group_id) REFERENCES modifier_groups(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 16. PRODUCT_MODIFIER_GROUPS (Liên kết sản phẩm - modifier)
-- ==============================================
CREATE TABLE IF NOT EXISTS product_modifier_groups (
    id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    modifier_group_id INT NOT NULL,
    display_order INT DEFAULT 0,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (modifier_group_id) REFERENCES modifier_groups(id) ON DELETE CASCADE,
    UNIQUE KEY unique_product_modifier (product_id, modifier_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 17. STOCK_TRANSACTIONS (Nhật ký nhập/xuất kho)
-- ==============================================
CREATE TABLE IF NOT EXISTS stock_transactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ingredient_id INT NOT NULL,
    type ENUM('IN', 'OUT', 'ADJUSTMENT', 'SALE') NOT NULL COMMENT 'Nhập/Xuất/Điều chỉnh/Bán',
    quantity DECIMAL(15,3) NOT NULL COMMENT 'Số lượng thay đổi (+ hoặc -)',
    unit_cost DECIMAL(15,2) COMMENT 'Giá nhập mỗi đơn vị (cho type=IN)',
    reference_type VARCHAR(50) COMMENT 'ORDER, MANUAL, STOCKTAKE',
    reference_id INT COMMENT 'ID của order hoặc phiếu kiểm kê',
    note TEXT,
    user_id INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id) ON DELETE RESTRICT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- 18. SETTINGS (Cấu hình hệ thống)
-- ==============================================
CREATE TABLE IF NOT EXISTS settings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    setting_type ENUM('STRING', 'NUMBER', 'BOOLEAN', 'JSON') DEFAULT 'STRING',
    description VARCHAR(255),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by INT,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- NEW INDEXES
-- ==============================================
CREATE INDEX idx_modifiers_group ON modifiers(group_id);
CREATE INDEX idx_product_modifiers_product ON product_modifier_groups(product_id);
CREATE INDEX idx_stock_trans_ingredient ON stock_transactions(ingredient_id);
CREATE INDEX idx_stock_trans_type ON stock_transactions(type);
CREATE INDEX idx_stock_trans_created ON stock_transactions(created_at);
CREATE INDEX idx_settings_key ON settings(setting_key);

-- ==============================================
-- DEFAULT SETTINGS DATA
-- ==============================================
INSERT INTO settings (setting_key, setting_value, setting_type, description) VALUES
('restaurant_name', 'Nhà Hàng ABC', 'STRING', 'Tên nhà hàng'),
('restaurant_address', '123 Đường ABC, Quận 1, TP.HCM', 'STRING', 'Địa chỉ nhà hàng'),
('restaurant_phone', '0901234567', 'STRING', 'Số điện thoại'),
('bank_name', 'Vietcombank', 'STRING', 'Tên ngân hàng'),
('bank_account_number', '1234567890', 'STRING', 'Số tài khoản ngân hàng'),
('bank_account_name', 'NGUYEN VAN A', 'STRING', 'Tên chủ tài khoản'),
('bank_qr_image', '', 'STRING', 'Path đến ảnh QR chuyển khoản'),
('vat_percent', '8', 'NUMBER', 'Phần trăm thuế VAT'),
('service_charge_percent', '5', 'NUMBER', 'Phần trăm phí dịch vụ'),
('receipt_footer', 'Cảm ơn quý khách! Hẹn gặp lại.', 'STRING', 'Dòng chữ cuối hóa đơn'),
('currency_symbol', 'VNĐ', 'STRING', 'Ký hiệu tiền tệ'),
('kitchen_auto_print', 'true', 'BOOLEAN', 'Tự động in xuống bếp khi order');

-- ==============================================
-- SAMPLE MODIFIER DATA
-- ==============================================
INSERT INTO modifier_groups (name, selection_type, is_required) VALUES
('Chọn Size', 'SINGLE', TRUE),
('Topping', 'MULTIPLE', FALSE),
('Mức đá', 'SINGLE', FALSE),
('Mức đường', 'SINGLE', FALSE);

INSERT INTO modifiers (group_id, name, price_adjustment, is_default, display_order) VALUES
-- Size (group_id = 1)
(1, 'Size S', 0, TRUE, 1),
(1, 'Size M', 5000, FALSE, 2),
(1, 'Size L', 10000, FALSE, 3),
-- Topping (group_id = 2)
(2, 'Trân châu đen', 8000, FALSE, 1),
(2, 'Trân châu trắng', 8000, FALSE, 2),
(2, 'Thạch dừa', 6000, FALSE, 3),
(2, 'Pudding', 10000, FALSE, 4),
-- Mức đá (group_id = 3)
(3, 'Đá bình thường', 0, TRUE, 1),
(3, 'Ít đá', 0, FALSE, 2),
(3, 'Không đá', 0, FALSE, 3),
-- Mức đường (group_id = 4)
(4, '100% đường', 0, TRUE, 1),
(4, '70% đường', 0, FALSE, 2),
(4, '50% đường', 0, FALSE, 3),
(4, '30% đường', 0, FALSE, 4),
(4, 'Không đường', 0, FALSE, 5);

-- ==============================================
-- RESERVATIONS (Đặt bàn trước)
-- ==============================================
CREATE TABLE IF NOT EXISTS reservations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    table_id INT NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    guest_count INT NOT NULL DEFAULT 2,
    reservation_time DATETIME NOT NULL,
    notes TEXT,
    status ENUM('PENDING', 'CONFIRMED', 'ARRIVED', 'CANCELLED', 'NO_SHOW') DEFAULT 'PENDING',
    notified BOOLEAN DEFAULT FALSE COMMENT 'Đã gửi thông báo nhắc chưa',
    created_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (table_id) REFERENCES tables(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_reservation_time (reservation_time),
    INDEX idx_customer_phone (customer_phone),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==============================================
-- End of Additional Schema
-- ==============================================

