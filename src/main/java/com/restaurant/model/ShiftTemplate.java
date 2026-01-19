package com.restaurant.model;

import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Shift Template - Ca làm cố định
 */
public class ShiftTemplate {
    
    private int id;
    private String name;
    private String code;
    private LocalTime startTime;
    private LocalTime endTime;
    private String color;
    private boolean active;
    private LocalDateTime createdAt;
    
    // Constructors
    public ShiftTemplate() {}
    
    public ShiftTemplate(int id, String name, String code, LocalTime startTime, LocalTime endTime) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = true;
    }
    
    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getDisplayTime() {
        return String.format("%s - %s", startTime, endTime);
    }
    
    @Override
    public String toString() {
        return name + " (" + getDisplayTime() + ")";
    }
}
