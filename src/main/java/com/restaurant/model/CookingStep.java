package com.restaurant.model;

import java.time.LocalDateTime;

/**
 * CookingStep entity - Bước nấu cho mỗi món
 */
public class CookingStep {
    
    private int id;
    private int productId;
    private int stepOrder;
    private String title;
    private String description;
    private int durationSeconds;
    private String icon;
    private LocalDateTime createdAt;
    
    // ===========================================
    // Constructors
    // ===========================================
    public CookingStep() {}
    
    public CookingStep(int productId, int stepOrder, String title, String description, int durationSeconds, String icon) {
        this.productId = productId;
        this.stepOrder = stepOrder;
        this.title = title;
        this.description = description;
        this.durationSeconds = durationSeconds;
        this.icon = icon;
    }
    
    // ===========================================
    // Helper Methods
    // ===========================================
    
    /**
     * Get formatted duration string (e.g., "5 phút", "1h 30 phút")
     */
    public String getFormattedDuration() {
        if (durationSeconds < 60) {
            return durationSeconds + " giây";
        } else if (durationSeconds < 3600) {
            return (durationSeconds / 60) + " phút";
        } else {
            int hours = durationSeconds / 3600;
            int minutes = (durationSeconds % 3600) / 60;
            return hours + "h " + (minutes > 0 ? minutes + " phút" : "");
        }
    }
    
    // ===========================================
    // Getters & Setters
    // ===========================================
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return icon + " " + title;
    }
}
