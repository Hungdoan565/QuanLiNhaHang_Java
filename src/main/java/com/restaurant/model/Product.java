package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product entity - Món ăn/đồ uống trong menu
 */
public class Product {
    
    private int id;
    private String name;
    private String description;
    private int categoryId;
    private Category category; // Eager loaded
    private BigDecimal price;
    private BigDecimal costPrice;
    private String imagePath;
    private boolean available;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // ===========================================
    // Constructors
    // ===========================================
    public Product() {
        this.available = true;
        this.active = true;
        this.price = BigDecimal.ZERO;
        this.costPrice = BigDecimal.ZERO;
    }
    
    public Product(int id, String name, BigDecimal price) {
        this();
        this.id = id;
        this.name = name;
        this.price = price;
    }
    
    public Product(int id, String name, BigDecimal price, int categoryId) {
        this(id, name, price);
        this.categoryId = categoryId;
    }
    
    // ===========================================
    // Helper Methods
    // ===========================================
    
    /**
     * Get formatted price string
     */
    public String getFormattedPrice() {
        if (price == null) return "0 ₫";
        return String.format("%,.0f ₫", price);
    }
    
    /**
     * Get category name
     */
    public String getCategoryName() {
        return category != null ? category.getName() : "Không phân loại";
    }
    
    /**
     * Calculate profit margin
     */
    public BigDecimal getProfitMargin() {
        if (price == null || costPrice == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return price.subtract(costPrice).divide(price, 2, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
    
    // ===========================================
    // Getters & Setters
    // ===========================================
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { 
        this.category = category;
        if (category != null) {
            this.categoryId = category.getId();
        }
    }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public String toString() {
        return String.format("Product{id=%d, name='%s', price=%s}", id, name, getFormattedPrice());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Product product = (Product) obj;
        return id == product.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
