package com.restaurant.model;

/**
 * Recipe entity - Định lượng nguyên liệu cho món ăn
 */
public class Recipe {
    
    private int id;
    private int productId;
    private int ingredientId;
    private double quantityUsed;
    
    // Transient fields for display
    private String ingredientName;
    private String ingredientUnit;
    private double ingredientStock; // Current stock in inventory
    
    // ===========================================
    // Constructors
    // ===========================================
    public Recipe() {}
    
    public Recipe(int productId, int ingredientId, double quantityUsed) {
        this.productId = productId;
        this.ingredientId = ingredientId;
        this.quantityUsed = quantityUsed;
    }
    
    // ===========================================
    // Helper Methods
    // ===========================================
    
    /**
     * Check if enough stock for this recipe item
     */
    public boolean hasEnoughStock() {
        return ingredientStock >= quantityUsed;
    }
    
    /**
     * Get formatted display string (e.g., "200g Thịt bò")
     */
    public String getDisplayString() {
        return String.format("%.0f%s %s", quantityUsed, ingredientUnit, ingredientName);
    }
    
    /**
     * Get stock warning if low
     */
    public String getStockWarning() {
        if (!hasEnoughStock()) {
            return String.format("⚠️ Thiếu %.0f%s", quantityUsed - ingredientStock, ingredientUnit);
        }
        return null;
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

    public int getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(int ingredientId) {
        this.ingredientId = ingredientId;
    }

    public double getQuantityUsed() {
        return quantityUsed;
    }

    public void setQuantityUsed(double quantityUsed) {
        this.quantityUsed = quantityUsed;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public String getIngredientUnit() {
        return ingredientUnit;
    }

    public void setIngredientUnit(String ingredientUnit) {
        this.ingredientUnit = ingredientUnit;
    }

    public double getIngredientStock() {
        return ingredientStock;
    }

    public void setIngredientStock(double ingredientStock) {
        this.ingredientStock = ingredientStock;
    }
}
