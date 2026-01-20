package com.restaurant.service;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.Recipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.List;

/**
 * InventoryService - Quản lý kho nguyên liệu
 * Handles stock deduction when cooking and low-stock warnings
 */
public class InventoryService {
    
    private static final Logger logger = LogManager.getLogger(InventoryService.class);
    private static InventoryService instance;
    
    private InventoryService() {}
    
    public static synchronized InventoryService getInstance() {
        if (instance == null) {
            instance = new InventoryService();
        }
        return instance;
    }
    
    /**
     * Check if training mode is enabled
     */
    public boolean isTrainingMode() {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = 'kitchen_training_mode'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return "true".equalsIgnoreCase(rs.getString("setting_value"));
            }
        } catch (SQLException e) {
            logger.error("Error checking training mode: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Deduct ingredients from inventory when item is cooked
     * @param productId The product being cooked
     * @param quantity Number of portions
     * @param orderId Reference order ID for transaction log
     * @return true if successful, false if not enough stock or error
     */
    public boolean deductIngredients(int productId, int quantity, int orderId) {
        // Skip if training mode
        if (isTrainingMode()) {
            logger.info("Training mode - skipping inventory deduction for product {}", productId);
            return true;
        }
        
        RecipeService recipeService = RecipeService.getInstance();
        List<Recipe> recipes = recipeService.getRecipeByProductId(productId);
        
        if (recipes.isEmpty()) {
            logger.warn("No recipe found for product {}", productId);
            return true; // No recipe = no deduction needed
        }
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            for (Recipe recipe : recipes) {
                double amountToDeduct = recipe.getQuantityUsed() * quantity;
                
                // Deduct from ingredients
                String updateSql = """
                    UPDATE ingredients 
                    SET quantity = quantity - ?
                    WHERE id = ? AND quantity >= ?
                    """;
                
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setDouble(1, amountToDeduct);
                    stmt.setInt(2, recipe.getIngredientId());
                    stmt.setDouble(3, amountToDeduct);
                    
                    int updated = stmt.executeUpdate();
                    if (updated == 0) {
                        // Not enough stock
                        logger.warn("Not enough stock for ingredient {} (need {}, have {})",
                            recipe.getIngredientName(), amountToDeduct, recipe.getIngredientStock());
                        conn.rollback();
                        return false;
                    }
                }
                
                // Log transaction
                String logSql = """
                    INSERT INTO stock_transactions 
                    (ingredient_id, type, quantity, reference_type, reference_id, note)
                    VALUES (?, 'SALE', ?, 'ORDER', ?, ?)
                    """;
                
                try (PreparedStatement stmt = conn.prepareStatement(logSql)) {
                    stmt.setInt(1, recipe.getIngredientId());
                    stmt.setDouble(2, -amountToDeduct);
                    stmt.setInt(3, orderId);
                    stmt.setString(4, "Auto deduct for product ID " + productId);
                    stmt.executeUpdate();
                }
            }
            
            conn.commit();
            logger.info("Deducted ingredients for product {} x{}", productId, quantity);
            return true;
            
        } catch (SQLException e) {
            logger.error("Error deducting ingredients: {}", e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back: {}", ex.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error closing connection: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Get low-stock ingredients (below min_quantity)
     */
    public List<String> getLowStockWarnings() {
        List<String> warnings = new java.util.ArrayList<>();
        String sql = """
            SELECT name, quantity, min_quantity, unit
            FROM ingredients
            WHERE quantity <= min_quantity AND is_active = TRUE
            ORDER BY (min_quantity - quantity) DESC
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String warning = String.format("⚠️ %s: còn %.0f%s (min: %.0f)",
                    rs.getString("name"),
                    rs.getDouble("quantity"),
                    rs.getString("unit"),
                    rs.getDouble("min_quantity"));
                warnings.add(warning);
            }
            
        } catch (SQLException e) {
            logger.error("Error getting low stock warnings: {}", e.getMessage());
        }
        
        return warnings;
    }
    
    /**
     * Set training mode on/off
     */
    public boolean setTrainingMode(boolean enabled) {
        String sql = "UPDATE settings SET setting_value = ? WHERE setting_key = 'kitchen_training_mode'";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, String.valueOf(enabled));
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logger.error("Error setting training mode: {}", e.getMessage());
            return false;
        }
    }
}
