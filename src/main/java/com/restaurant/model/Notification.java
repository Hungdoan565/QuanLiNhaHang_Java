package com.restaurant.model;

import java.time.LocalDateTime;

/**
 * Notification - Thông báo trong ứng dụng
 */
public class Notification {
    
    public enum NotificationType {
        INFO("Thông báo"),
        SUCCESS("Thành công"),
        WARNING("Cảnh báo"),
        LEAVE_APPROVED("Duyệt nghỉ phép"),
        LEAVE_REJECTED("Từ chối nghỉ phép"),
        SCHEDULE("Lịch làm việc"),
        ORDER("Đơn hàng"),
        SYSTEM("Hệ thống");
        
        private final String displayName;
        NotificationType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    
    private int id;
    private int userId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private Integer relatedId;
    private LocalDateTime createdAt;
    
    // Constructors
    public Notification() {
        this.type = NotificationType.INFO;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }
    
    public Notification(int userId, String title, String message, NotificationType type) {
        this();
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
    }
    
    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    public Integer getRelatedId() { return relatedId; }
    public void setRelatedId(Integer relatedId) { this.relatedId = relatedId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getIcon() {
        return switch (type) {
            case SUCCESS, LEAVE_APPROVED -> "✅";
            case WARNING, LEAVE_REJECTED -> "⚠️";
            case SCHEDULE -> "📅";
            case SYSTEM -> "🔔";
            default -> "📌";
        };
    }
}
