-- Split Bill Feature - Database Schema
-- Version: 1.0
-- Date: 2026-01-20

-- =====================================================
-- SPLIT BILLS TABLE
-- Stores the split bill session for an order
-- =====================================================
CREATE TABLE IF NOT EXISTS split_bills (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    split_type ENUM('EQUAL', 'BY_ITEM', 'CUSTOM') NOT NULL DEFAULT 'EQUAL',
    total_splits INT NOT NULL DEFAULT 2,
    total_amount DECIMAL(15,2) NOT NULL,
    status ENUM('PENDING', 'PARTIAL', 'COMPLETED') DEFAULT 'PENDING',
    created_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- =====================================================
-- SPLIT BILL PARTS TABLE
-- Each part of the split (one per person paying)
-- =====================================================
CREATE TABLE IF NOT EXISTS split_bill_parts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    split_bill_id INT NOT NULL,
    part_number INT NOT NULL,               -- 1, 2, 3... (số thứ tự phần)
    payer_name VARCHAR(50),                 -- Tên người trả (tùy chọn)
    amount DECIMAL(15,2) NOT NULL,          -- Số tiền phần này
    paid BOOLEAN DEFAULT FALSE,
    payment_method ENUM('CASH', 'CARD', 'TRANSFER', 'EWALLET'),
    paid_at DATETIME,
    notes VARCHAR(255),
    FOREIGN KEY (split_bill_id) REFERENCES split_bills(id) ON DELETE CASCADE,
    UNIQUE KEY unique_part (split_bill_id, part_number)
);

-- =====================================================
-- SPLIT BILL ITEM ASSIGNMENTS TABLE
-- For BY_ITEM mode: which items belong to which part
-- Supports shared items (item split across multiple parts)
-- =====================================================
CREATE TABLE IF NOT EXISTS split_bill_item_assignments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    split_bill_id INT NOT NULL,
    order_detail_id INT NOT NULL,
    part_number INT NOT NULL,               -- Thuộc phần nào
    share_count INT DEFAULT 1,              -- Chia cho bao nhiêu người (shared item)
    share_amount DECIMAL(15,2) NOT NULL,    -- Số tiền sau khi chia
    FOREIGN KEY (split_bill_id) REFERENCES split_bills(id) ON DELETE CASCADE,
    FOREIGN KEY (order_detail_id) REFERENCES order_details(id) ON DELETE CASCADE
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================
CREATE INDEX idx_split_bills_order ON split_bills(order_id);
CREATE INDEX idx_split_bills_status ON split_bills(status);
CREATE INDEX idx_split_parts_paid ON split_bill_parts(paid);
CREATE INDEX idx_split_items_part ON split_bill_item_assignments(part_number);

-- =====================================================
-- SAMPLE USAGE COMMENTS
-- =====================================================
-- 
-- EQUAL SPLIT (Chia đều):
-- 1. INSERT INTO split_bills (order_id, split_type, total_splits, total_amount) VALUES (1, 'EQUAL', 3, 300000);
-- 2. INSERT INTO split_bill_parts (split_bill_id, part_number, amount) VALUES (1, 1, 100000), (1, 2, 100000), (1, 3, 100000);
--
-- BY_ITEM SPLIT (Theo món):
-- 1. CREATE split_bill with split_type = 'BY_ITEM'
-- 2. Assign items to parts via split_bill_item_assignments
-- 3. Calculate each part's amount based on assigned items
--
-- SHARED ITEM (Món chia chung):
-- If Pizza costs 200,000₫ and shared by 4 people:
-- INSERT INTO split_bill_item_assignments (split_bill_id, order_item_id, part_number, share_count, share_amount)
-- VALUES (1, 5, 1, 4, 50000), (1, 5, 2, 4, 50000), (1, 5, 3, 4, 50000), (1, 5, 4, 4, 50000);
