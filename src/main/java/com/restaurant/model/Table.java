package com.restaurant.model;

import java.time.LocalDateTime;

/**
 * Table entity - Bàn trong nhà hàng
 */
public class Table {
    
    private int id;
    private String name;
    private int capacity;
    private TableStatus status;
    private String area;
    private int positionX;
    private int positionY;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Current order info (if occupied)
    private Integer currentOrderId;
    private String currentOrderCode;
    private int guestCount;
    private LocalDateTime occupiedSince;
    
    // ===========================================
    // Enums
    // ===========================================
    public enum TableStatus {
        AVAILABLE("Trống", "#00B894"),      // Green
        OCCUPIED("Có khách", "#E74C3C"),    // Red
        RESERVED("Đã đặt", "#FDCB6E"),      // Yellow
        CLEANING("Đang dọn", "#F39C12");    // Orange
        
        private final String displayName;
        private final String colorHex;
        
        TableStatus(String displayName, String colorHex) {
            this.displayName = displayName;
            this.colorHex = colorHex;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColorHex() { return colorHex; }
    }
    
    // ===========================================
    // Constructors
    // ===========================================
    public Table() {
        this.status = TableStatus.AVAILABLE;
        this.active = true;
        this.capacity = 4;
    }
    
    public Table(int id, String name, int capacity) {
        this();
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }
    
    public Table(int id, String name, int capacity, TableStatus status, String area) {
        this(id, name, capacity);
        this.status = status;
        this.area = area;
    }
    
    // ===========================================
    // Helper Methods
    // ===========================================
    
    /**
     * Check if table is available for seating
     */
    public boolean isAvailable() {
        return status == TableStatus.AVAILABLE;
    }
    
    /**
     * Check if table has current order (occupied with guests)
     */
    public boolean hasActiveOrder() {
        return status == TableStatus.OCCUPIED || guestCount > 0;
    }
    
    /**
     * Get display text for table card
     */
    public String getStatusDisplay() {
        if (status == TableStatus.OCCUPIED && guestCount > 0) {
            return status.getDisplayName() + " (" + guestCount + " khách)";
        }
        return status.getDisplayName();
    }
    
    /**
     * Get time since occupied (for occupied tables)
     */
    public String getOccupiedDuration() {
        if (occupiedSince == null) return "";
        
        long minutes = java.time.Duration.between(occupiedSince, LocalDateTime.now()).toMinutes();
        if (minutes < 60) {
            return minutes + " phút";
        }
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
    
    // ===========================================
    // Getters & Setters
    // ===========================================
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    
    public TableStatus getStatus() { return status; }
    public void setStatus(TableStatus status) { this.status = status; }
    
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    
    public int getPositionX() { return positionX; }
    public void setPositionX(int positionX) { this.positionX = positionX; }
    
    public int getPositionY() { return positionY; }
    public void setPositionY(int positionY) { this.positionY = positionY; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Integer getCurrentOrderId() { return currentOrderId; }
    public void setCurrentOrderId(Integer currentOrderId) { this.currentOrderId = currentOrderId; }
    
    public String getCurrentOrderCode() { return currentOrderCode; }
    public void setCurrentOrderCode(String currentOrderCode) { this.currentOrderCode = currentOrderCode; }
    
    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }
    
    public LocalDateTime getOccupiedSince() { return occupiedSince; }
    public void setOccupiedSince(LocalDateTime occupiedSince) { this.occupiedSince = occupiedSince; }
    
    @Override
    public String toString() {
        return String.format("Table{id=%d, name='%s', status=%s, capacity=%d}", 
            id, name, status, capacity);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Table table = (Table) obj;
        return id == table.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
