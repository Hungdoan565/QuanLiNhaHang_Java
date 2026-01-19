package com.restaurant.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Leave Request - Yêu cầu nghỉ phép
 */
public class LeaveRequest {
    
    public enum LeaveType {
        ANNUAL("Nghỉ phép năm"),
        SICK("Nghỉ ốm"),
        PERSONAL("Việc riêng"),
        EMERGENCY("Khẩn cấp");
        
        private final String displayName;
        LeaveType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    
    public enum LeaveStatus {
        PENDING("Chờ duyệt"),
        APPROVED("Đã duyệt"),
        REJECTED("Từ chối");
        
        private final String displayName;
        LeaveStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    
    private int id;
    private int userId;
    private User user; // Eager loaded
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private LeaveStatus status;
    private Integer reviewedBy;
    private User reviewer; // Eager loaded
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public LeaveRequest() {
        this.status = LeaveStatus.PENDING;
    }
    
    public LeaveRequest(int userId, LeaveType leaveType, LocalDate startDate, LocalDate endDate, String reason) {
        this.userId = userId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = LeaveStatus.PENDING;
    }
    
    // Helper methods
    public int getDaysCount() {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
    
    public boolean isValidRequest() {
        // SICK and EMERGENCY can be requested same day
        if (leaveType == LeaveType.SICK || leaveType == LeaveType.EMERGENCY) {
            return startDate != null && !startDate.isBefore(LocalDate.now());
        }
        // ANNUAL and PERSONAL must request at least 1 day in advance
        long daysUntilStart = ChronoUnit.DAYS.between(LocalDate.now(), startDate);
        return daysUntilStart >= 1;
    }
    
    public boolean isPending() {
        return status == LeaveStatus.PENDING;
    }
    
    public boolean isApproved() {
        return status == LeaveStatus.APPROVED;
    }
    
    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public User getUser() { return user; }
    public void setUser(User user) { 
        this.user = user;
        if (user != null) this.userId = user.getId();
    }
    
    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public LeaveStatus getStatus() { return status; }
    public void setStatus(LeaveStatus status) { this.status = status; }
    
    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }
    
    public User getReviewer() { return reviewer; }
    public void setReviewer(User reviewer) { 
        this.reviewer = reviewer;
        if (reviewer != null) this.reviewedBy = reviewer.getId();
    }
    
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
