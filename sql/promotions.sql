-- ==============================================
-- PROMOTIONS & COUPON SYSTEM
-- Khuyến mãi, mã giảm giá, combo, happy hour
-- ==============================================

USE restaurant_db;

-- Bảng khuyến mãi
CREATE TABLE IF NOT EXISTS promotions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) UNIQUE,           -- NULL = auto-apply (không cần nhập code)
    name VARCHAR(100) NOT NULL,
    description TEXT,
    type ENUM('PERCENT', 'FIXED', 'BUY_X_GET_Y', 'COMBO') NOT NULL,
    value DECIMAL(10,2) NOT NULL,      -- Giá trị: % hoặc số tiền cố định
    min_order_value DECIMAL(15,2) DEFAULT 0,  -- Đơn hàng tối thiểu
    max_discount DECIMAL(15,2),        -- Giảm tối đa (cho PERCENT)
    
    -- Thời gian áp dụng
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    
    -- Điều kiện áp dụng
    applicable_days VARCHAR(30) DEFAULT '*',     -- "MON,TUE,WED" hoặc "*" = tất cả
    applicable_hours VARCHAR(30) DEFAULT '*',    -- "11:00-14:00" hoặc "*" = cả ngày
    applicable_categories TEXT,                   -- Category IDs hoặc NULL = tất cả
    applicable_products TEXT,                     -- Product IDs hoặc NULL = tất cả
    
    -- Giới hạn sử dụng
    usage_limit INT,                  -- Tổng số lần sử dụng tối đa (NULL = không giới hạn)
    usage_limit_per_customer INT,     -- Số lần / khách hàng (NULL = không giới hạn)
    used_count INT DEFAULT 0,
    
    -- Tier requirement
    min_customer_tier ENUM('REGULAR', 'SILVER', 'GOLD', 'VIP'),
    
    is_active BOOLEAN DEFAULT TRUE,
    created_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Lịch sử sử dụng khuyến mãi
CREATE TABLE IF NOT EXISTS promotion_usage (
    id INT PRIMARY KEY AUTO_INCREMENT,
    promotion_id INT NOT NULL,
    order_id INT NOT NULL,
    customer_id INT,
    discount_amount DECIMAL(15,2) NOT NULL,
    used_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Link orders to promotions
ALTER TABLE orders ADD COLUMN promotion_id INT NULL;
ALTER TABLE orders ADD COLUMN discount_amount DECIMAL(15,2) DEFAULT 0;
ALTER TABLE orders ADD FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE SET NULL;

-- Indexes
CREATE INDEX idx_promotions_code ON promotions(code);
CREATE INDEX idx_promotions_dates ON promotions(start_date, end_date);
CREATE INDEX idx_promotions_active ON promotions(is_active);
CREATE INDEX idx_promotion_usage_order ON promotion_usage(order_id);

-- Seed sample promotions
INSERT INTO promotions (code, name, description, type, value, min_order_value, max_discount, start_date, end_date, applicable_hours) VALUES
('WELCOME10', 'Chào mừng khách mới', 'Giảm 10% cho đơn đầu tiên', 'PERCENT', 10, 100000, 50000, NOW(), DATE_ADD(NOW(), INTERVAL 3 MONTH), '*'),
('HAPPYHOUR', 'Happy Hour', 'Giảm 15% từ 14h-17h', 'PERCENT', 15, 0, 100000, NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), '14:00-17:00'),
('LUNCH50K', 'Ưu đãi trưa', 'Giảm 50k cho đơn từ 300k', 'FIXED', 50000, 300000, NULL, NOW(), DATE_ADD(NOW(), INTERVAL 6 MONTH), '11:00-14:00'),
(NULL, 'Ưu đãi VIP', 'Tự động giảm 10% cho khách VIP', 'PERCENT', 10, 0, NULL, NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), '*');

-- Update sample VIP promotion to require VIP tier
UPDATE promotions SET min_customer_tier = 'VIP' WHERE name = 'Ưu đãi VIP';

SELECT '✅ Promotions tables created!' AS status;
