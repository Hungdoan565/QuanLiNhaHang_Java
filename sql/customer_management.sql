-- ==============================================
-- CUSTOMER MANAGEMENT SYSTEM
-- Quản lý khách hàng, tích điểm thưởng
-- ==============================================

USE restaurant_db;

-- Bảng khách hàng
CREATE TABLE IF NOT EXISTS customers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(100),
    birthday DATE,
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    address TEXT,
    tier ENUM('REGULAR', 'SILVER', 'GOLD', 'VIP') DEFAULT 'REGULAR',
    loyalty_points INT DEFAULT 0,
    total_spent DECIMAL(15,2) DEFAULT 0,
    visit_count INT DEFAULT 0,
    last_visit DATE,
    notes TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Lịch sử tích điểm
CREATE TABLE IF NOT EXISTS loyalty_transactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    order_id INT,
    points_change INT NOT NULL,  -- Positive = earn, Negative = redeem
    balance_after INT NOT NULL,
    transaction_type ENUM('EARN', 'REDEEM', 'ADJUST', 'EXPIRE', 'BONUS') NOT NULL,
    description VARCHAR(200),
    created_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_tier ON customers(tier);
CREATE INDEX idx_loyalty_customer ON loyalty_transactions(customer_id);
CREATE INDEX idx_loyalty_date ON loyalty_transactions(created_at);

-- Link orders to customers (add column to orders table)
ALTER TABLE orders ADD COLUMN customer_id INT NULL AFTER id;
ALTER TABLE orders ADD COLUMN loyalty_points_earned INT DEFAULT 0;
ALTER TABLE orders ADD COLUMN loyalty_points_used INT DEFAULT 0;
ALTER TABLE orders ADD FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;

-- Seed sample customers
INSERT INTO customers (full_name, phone, email, birthday, tier, loyalty_points, total_spent, visit_count) VALUES
('Nguyễn Văn An', '0901111111', 'an@email.com', '1990-05-15', 'GOLD', 5200, 8500000, 25),
('Trần Thị Bình', '0902222222', 'binh@email.com', '1985-08-20', 'SILVER', 1800, 3200000, 12),
('Lê Hoàng Cường', '0903333333', 'cuong@email.com', '1995-12-10', 'VIP', 12500, 18000000, 45),
('Phạm Minh Dũng', '0904444444', null, '1988-03-25', 'REGULAR', 350, 650000, 3),
('Hoàng Thị Em', '0905555555', 'em@email.com', '1992-07-08', 'SILVER', 2100, 4100000, 15);

SELECT '✅ Customer management tables created!' AS status;
