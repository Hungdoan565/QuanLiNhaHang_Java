package com.restaurant.model;

import java.time.LocalDateTime;

/**
 * Reservation Model - Đặt bàn trước
 */
public class Reservation {
    
    public enum Status {
        PENDING("Đang chờ", "#FFC107"),
        CONFIRMED("Đã xác nhận", "#2196F3"),
        ARRIVED("Đã đến", "#4CAF50"),
        COMPLETED("Hoàn tất", "#607D8B"),
        CANCELLED("Đã hủy", "#9E9E9E"),
        NO_SHOW("Không đến", "#F44336");
        
        private final String displayName;
        private final String colorHex;
        
        Status(String displayName, String colorHex) {
            this.displayName = displayName;
            this.colorHex = colorHex;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColorHex() { return colorHex; }
    }
    
    private int id;
    private int tableId;
    private String customerName;
    private String customerPhone;
    private int guestCount;
    private LocalDateTime reservationTime;
    private String notes;
    private Status status;
    private boolean notified;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional field for display
    private String tableName;
    
    public Reservation() {
        this.status = Status.PENDING;
        this.guestCount = 2;
        this.notified = false;
    }
    
    public Reservation(int tableId, String customerName, String customerPhone, 
                       int guestCount, LocalDateTime reservationTime) {
        this();
        this.tableId = tableId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.guestCount = guestCount;
        this.reservationTime = reservationTime;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getTableId() { return tableId; }
    public void setTableId(int tableId) { this.tableId = tableId; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    
    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }
    
    public LocalDateTime getReservationTime() { return reservationTime; }
    public void setReservationTime(LocalDateTime reservationTime) { this.reservationTime = reservationTime; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }
    
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    
    /**
     * Check if reservation is upcoming (within next 30 minutes)
     */
    public boolean isUpcoming() {
        if (reservationTime == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return reservationTime.isAfter(now) && 
               reservationTime.isBefore(now.plusMinutes(30));
    }
    
    /**
     * Check if reservation needs reminder (15 minutes before)
     */
    public boolean needsReminder() {
        if (reservationTime == null || notified) return false;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = reservationTime.minusMinutes(15);
        return now.isAfter(reminderTime) && now.isBefore(reservationTime);
    }
    
    /**
     * Get formatted time for display
     */
    public String getFormattedTime() {
        if (reservationTime == null) return "";
        return reservationTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM"));
    }
    
    @Override
    public String toString() {
        return String.format("Reservation[%d] %s - %s @ %s", 
            id, customerName, customerPhone, getFormattedTime());
    }
}
