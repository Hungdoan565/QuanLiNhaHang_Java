-- ==============================================
-- Shift & Leave Management Tables
-- Run after schema.sql
-- ==============================================

USE restaurant_db;

-- ==============================================
-- 1. SHIFT TEMPLATES - Ca làm cố định
-- ==============================================
CREATE TABLE IF NOT EXISTS shift_templates (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    color VARCHAR(7) DEFAULT '#3498DB' COMMENT 'Hex color for calendar',
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default shift templates
INSERT INTO shift_templates (name, code, start_time, end_time, color) VALUES
('Ca Sáng', 'MORNING', '07:00:00', '14:00:00', '#3498DB'),
('Ca Chiều', 'AFTERNOON', '14:00:00', '21:00:00', '#F39C12'),
('Ca Tối', 'EVENING', '17:00:00', '23:00:00', '#9B59B6'),
('Ca Full', 'FULL_DAY', '09:00:00', '21:00:00', '#E74C3C')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ==============================================
-- 2. WORK SCHEDULES - Lịch làm việc
-- ==============================================
CREATE TABLE IF NOT EXISTS work_schedules (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    work_date DATE NOT NULL,
    shift_template_id INT COMMENT 'NULL if custom time',
    custom_start_time TIME COMMENT 'Custom time if not using template',
    custom_end_time TIME,
    status ENUM('SCHEDULED', 'CHECKED_IN', 'CHECKED_OUT', 'ABSENT', 'ON_LEAVE') DEFAULT 'SCHEDULED',
    check_in_time DATETIME,
    check_out_time DATETIME,
    notes TEXT,
    created_by INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (shift_template_id) REFERENCES shift_templates(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    UNIQUE KEY unique_user_date (user_id, work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_work_schedules_date ON work_schedules(work_date);
CREATE INDEX idx_work_schedules_user ON work_schedules(user_id);
CREATE INDEX idx_work_schedules_status ON work_schedules(status);

-- ==============================================
-- 3. LEAVE REQUESTS - Xin nghỉ phép  
-- ==============================================
CREATE TABLE IF NOT EXISTS leave_requests (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    leave_type ENUM('ANNUAL', 'SICK', 'PERSONAL', 'EMERGENCY') NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    reviewed_by INT,
    reviewed_at DATETIME,
    rejection_reason VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_leave_requests_user ON leave_requests(user_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_leave_requests_dates ON leave_requests(start_date, end_date);

-- ==============================================
-- 4. LEAVE BALANCE - Số ngày nghỉ còn lại
-- ==============================================
CREATE TABLE IF NOT EXISTS leave_balances (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL UNIQUE,
    annual_leave_days INT DEFAULT 12 COMMENT 'Nghỉ phép năm',
    sick_leave_days INT DEFAULT 5 COMMENT 'Nghỉ ốm',
    used_annual_days INT DEFAULT 0,
    used_sick_days INT DEFAULT 0,
    year INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Initialize leave balance for existing users
INSERT INTO leave_balances (user_id, year)
SELECT id, YEAR(CURRENT_DATE) FROM users
ON DUPLICATE KEY UPDATE year = YEAR(CURRENT_DATE);

-- ==============================================
-- VERIFICATION
-- ==============================================
SELECT '✅ Shift Management tables created successfully!' AS status;
