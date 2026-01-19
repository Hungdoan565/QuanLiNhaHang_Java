-- ==============================================
-- NOTIFICATIONS TABLE
-- For in-app user notifications
-- ==============================================

USE restaurant_db;

CREATE TABLE IF NOT EXISTS notifications (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('INFO', 'SUCCESS', 'WARNING', 'LEAVE_APPROVED', 'LEAVE_REJECTED', 'SCHEDULE', 'SYSTEM') DEFAULT 'INFO',
    is_read BOOLEAN DEFAULT FALSE,
    related_id INT COMMENT 'ID of related entity (leave_request_id, etc)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read);

SELECT '✅ Notifications table created!' AS status;
