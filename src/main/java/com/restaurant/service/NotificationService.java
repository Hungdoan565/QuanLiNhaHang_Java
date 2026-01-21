package com.restaurant.service;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.Notification;
import com.restaurant.model.Notification.NotificationType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Notification Service - Quản lý thông báo trong ứng dụng
 */
public class NotificationService {
    
    private static final Logger logger = LogManager.getLogger(NotificationService.class);
    private static NotificationService instance;
    
    private NotificationService() {}
    
    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }
    
    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(int userId) {
        return getNotifications(userId, false, 20);
    }
    
    /**
     * Get all notifications for a user
     */
    public List<Notification> getAllNotifications(int userId, int limit) {
        return getNotifications(userId, null, limit);
    }
    
    private List<Notification> getNotifications(int userId, Boolean isRead, int limit) {
        List<Notification> notifications = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT * FROM notifications 
            WHERE user_id = ?
            """);
        
        if (isRead != null) {
            sql.append(" AND is_read = ?");
        }
        sql.append(" ORDER BY created_at DESC LIMIT ?");
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            stmt.setInt(paramIndex++, userId);
            if (isRead != null) {
                stmt.setBoolean(paramIndex++, isRead);
            }
            stmt.setInt(paramIndex, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapNotification(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading notifications", e);
        }
        return notifications;
    }
    
    /**
     * Get unread count
     */
    public int getUnreadCount(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting unread count", e);
        }
        return 0;
    }
    
    /**
     * Create a notification
     */
    public boolean createNotification(Notification notification) {
        String sql = """
            INSERT INTO notifications (user_id, title, message, type, related_id)
            VALUES (?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, notification.getUserId());
            stmt.setString(2, notification.getTitle());
            stmt.setString(3, notification.getMessage());
            stmt.setString(4, notification.getType().name());
            
            if (notification.getRelatedId() != null) {
                stmt.setInt(5, notification.getRelatedId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating notification", e);
            return false;
        }
    }
    
    /**
     * Mark notification as read
     */
    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, notificationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error marking notification as read", e);
            return false;
        }
    }
    
    /**
     * Mark all notifications as read for a user
     */
    public boolean markAllAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            return stmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            logger.error("Error marking all as read", e);
            return false;
        }
    }
    
    /**
     * Send leave approval notification
     */
    public void notifyLeaveApproved(int userId, String leaveDetails) {
        Notification n = new Notification(userId, 
            "✅ Đơn nghỉ phép được duyệt", 
            leaveDetails,
            NotificationType.LEAVE_APPROVED);
        createNotification(n);
    }
    
    /**
     * Send leave rejection notification
     */
    public void notifyLeaveRejected(int userId, String leaveDetails, String reason) {
        Notification n = new Notification(userId, 
            "❌ Đơn nghỉ phép bị từ chối", 
            leaveDetails + "\nLý do: " + reason,
            NotificationType.LEAVE_REJECTED);
        createNotification(n);
    }
    
    /**
     * Send schedule notification
     */
    public void notifyScheduleChange(int userId, String message) {
        Notification n = new Notification(userId, 
            "📅 Lịch làm việc thay đổi", 
            message,
            NotificationType.SCHEDULE);
        createNotification(n);
    }
    
    /**
     * Notify all waiters that food is ready for pickup
     * @param tableName The table name with ready food
     * @param itemCount Number of ready items
     */
    public void notifyWaiters(String tableName, int itemCount) {
        // Get all active waiters (role_id = 4 for WAITER)
        String sql = "SELECT id FROM users WHERE role_id = 4 AND is_active = TRUE";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                int waiterId = rs.getInt("id");
                Notification n = new Notification(waiterId,
                    "🍽️ Món sẵn sàng",
                    tableName + " có " + itemCount + " món cần lấy",
                    NotificationType.ORDER);
                createNotification(n);
            }
            logger.info("Notified waiters about ready food for table {}", tableName);
        } catch (SQLException e) {
            logger.error("Error notifying waiters", e);
        }
    }
    
    private Notification mapNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(NotificationType.valueOf(rs.getString("type")));
        n.setRead(rs.getBoolean("is_read"));
        
        int relatedId = rs.getInt("related_id");
        if (!rs.wasNull()) {
            n.setRelatedId(relatedId);
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            n.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return n;
    }
}
