package com.restaurant.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Work Schedule - Lịch làm việc của nhân viên
 */
public class WorkSchedule {
    
    public enum ScheduleStatus {
        SCHEDULED,    // Đã xếp lịch
        CHECKED_IN,   // Đã vào ca
        CHECKED_OUT,  // Đã kết thúc ca
        ABSENT,       // Vắng mặt
        ON_LEAVE      // Nghỉ phép
    }
    
    private int id;
    private int userId;
    private User user; // Eager loaded
    private LocalDate workDate;
    private Integer shiftTemplateId;
    private ShiftTemplate shiftTemplate; // Eager loaded
    private LocalTime customStartTime;
    private LocalTime customEndTime;
    private ScheduleStatus status;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String notes;
    private int createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public WorkSchedule() {
        this.status = ScheduleStatus.SCHEDULED;
    }
    
    public WorkSchedule(int userId, LocalDate workDate, ShiftTemplate template) {
        this.userId = userId;
        this.workDate = workDate;
        this.shiftTemplate = template;
        this.shiftTemplateId = template != null ? template.getId() : null;
        this.status = ScheduleStatus.SCHEDULED;
    }
    
    // Helper methods
    public LocalTime getEffectiveStartTime() {
        if (shiftTemplate != null) {
            return shiftTemplate.getStartTime();
        }
        return customStartTime;
    }
    
    public LocalTime getEffectiveEndTime() {
        if (shiftTemplate != null) {
            return shiftTemplate.getEndTime();
        }
        return customEndTime;
    }
    
    public String getShiftName() {
        if (shiftTemplate != null) {
            return shiftTemplate.getName();
        }
        return "Custom";
    }
    
    public String getColor() {
        if (shiftTemplate != null) {
            return shiftTemplate.getColor();
        }
        return "#95A5A6";
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
    
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    
    public Integer getShiftTemplateId() { return shiftTemplateId; }
    public void setShiftTemplateId(Integer shiftTemplateId) { this.shiftTemplateId = shiftTemplateId; }
    
    public ShiftTemplate getShiftTemplate() { return shiftTemplate; }
    public void setShiftTemplate(ShiftTemplate shiftTemplate) { 
        this.shiftTemplate = shiftTemplate;
        if (shiftTemplate != null) this.shiftTemplateId = shiftTemplate.getId();
    }
    
    public LocalTime getCustomStartTime() { return customStartTime; }
    public void setCustomStartTime(LocalTime customStartTime) { this.customStartTime = customStartTime; }
    
    public LocalTime getCustomEndTime() { return customEndTime; }
    public void setCustomEndTime(LocalTime customEndTime) { this.customEndTime = customEndTime; }
    
    public ScheduleStatus getStatus() { return status; }
    public void setStatus(ScheduleStatus status) { this.status = status; }
    
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
