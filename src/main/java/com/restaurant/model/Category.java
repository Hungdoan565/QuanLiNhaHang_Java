package com.restaurant.model;

import java.time.LocalDateTime;

/**
 * Category entity - Danh mục món ăn
 */
public class Category {
    
    private int id;
    private String name;
    private String icon;
    private int displayOrder;
    private String printerName;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed field
    private int productCount;
    
    // ===========================================
    // Constructors
    // ===========================================
    public Category() {
        this.active = true;
        this.displayOrder = 0;
        this.printerName = "Kitchen_Printer";
    }
    
    public Category(int id, String name) {
        this();
        this.id = id;
        this.name = name;
    }
    
    public Category(int id, String name, String icon, String printerName) {
        this(id, name);
        this.icon = icon;
        this.printerName = printerName;
    }
    
    // ===========================================
    // Getters & Setters
    // ===========================================
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    
    public String getPrinterName() { return printerName; }
    public void setPrinterName(String printerName) { this.printerName = printerName; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public int getProductCount() { return productCount; }
    public void setProductCount(int productCount) { this.productCount = productCount; }
    
    @Override
    public String toString() {
        return name; // For JComboBox display
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Category category = (Category) obj;
        return id == category.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
