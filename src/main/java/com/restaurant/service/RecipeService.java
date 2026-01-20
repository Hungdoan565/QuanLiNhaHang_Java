package com.restaurant.service;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.CookingStep;
import com.restaurant.model.Recipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * RecipeService - Quản lý công thức và nguyên liệu
 */
public class RecipeService {
    
    private static final Logger logger = LogManager.getLogger(RecipeService.class);
    private static RecipeService instance;
    
    private RecipeService() {}
    
    public static synchronized RecipeService getInstance() {
        if (instance == null) {
            instance = new RecipeService();
        }
        return instance;
    }
    
    // ===========================================
    // Recipe Methods
    // ===========================================
    
    /**
     * Get all recipe items (ingredients) for a product
     */
    public List<Recipe> getRecipeByProductId(int productId) {
        List<Recipe> recipes = new ArrayList<>();
        String sql = """
            SELECT r.id, r.product_id, r.ingredient_id, r.quantity_used,
                   i.name AS ingredient_name, i.unit AS ingredient_unit, 
                   i.quantity AS ingredient_stock
            FROM recipes r
            JOIN ingredients i ON r.ingredient_id = i.id
            WHERE r.product_id = ?
            ORDER BY i.name
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Recipe recipe = new Recipe();
                recipe.setId(rs.getInt("id"));
                recipe.setProductId(rs.getInt("product_id"));
                recipe.setIngredientId(rs.getInt("ingredient_id"));
                recipe.setQuantityUsed(rs.getDouble("quantity_used"));
                recipe.setIngredientName(rs.getString("ingredient_name"));
                recipe.setIngredientUnit(rs.getString("ingredient_unit"));
                recipe.setIngredientStock(rs.getDouble("ingredient_stock"));
                recipes.add(recipe);
            }
            
        } catch (SQLException e) {
            logger.error("Error getting recipe for product {}: {}", productId, e.getMessage());
        }
        
        return recipes;
    }
    
    /**
     * Check if all ingredients are available for a product
     */
    public boolean hasAllIngredients(int productId, int quantity) {
        List<Recipe> recipes = getRecipeByProductId(productId);
        for (Recipe recipe : recipes) {
            double needed = recipe.getQuantityUsed() * quantity;
            if (recipe.getIngredientStock() < needed) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get missing ingredients for a product
     */
    public List<Recipe> getMissingIngredients(int productId, int quantity) {
        List<Recipe> missing = new ArrayList<>();
        List<Recipe> recipes = getRecipeByProductId(productId);
        
        for (Recipe recipe : recipes) {
            double needed = recipe.getQuantityUsed() * quantity;
            if (recipe.getIngredientStock() < needed) {
                missing.add(recipe);
            }
        }
        
        return missing;
    }
    
    // ===========================================
    // Cooking Steps Methods
    // ===========================================
    
    /**
     * Get all cooking steps for a product
     */
    public List<CookingStep> getCookingSteps(int productId) {
        List<CookingStep> steps = new ArrayList<>();
        String sql = """
            SELECT id, product_id, step_order, title, description, duration_seconds, icon
            FROM cooking_steps
            WHERE product_id = ?
            ORDER BY step_order
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                CookingStep step = new CookingStep();
                step.setId(rs.getInt("id"));
                step.setProductId(rs.getInt("product_id"));
                step.setStepOrder(rs.getInt("step_order"));
                step.setTitle(rs.getString("title"));
                step.setDescription(rs.getString("description"));
                step.setDurationSeconds(rs.getInt("duration_seconds"));
                step.setIcon(rs.getString("icon"));
                steps.add(step);
            }
            
        } catch (SQLException e) {
            logger.error("Error getting cooking steps for product {}: {}", productId, e.getMessage());
        }
        
        return steps;
    }
    
    /**
     * Get total cooking time for a product (sum of all steps)
     */
    public int getTotalCookingTime(int productId) {
        List<CookingStep> steps = getCookingSteps(productId);
        return steps.stream().mapToInt(CookingStep::getDurationSeconds).sum();
    }
    
    /**
     * Update current step for an order item
     */
    public boolean updateCurrentStep(int orderDetailId, int currentStep) {
        String sql = "UPDATE order_details SET current_step = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, currentStep);
            stmt.setInt(2, orderDetailId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating current step: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Mark cooking started for an order item
     */
    public boolean startCooking(int orderDetailId) {
        String sql = """
            UPDATE order_details 
            SET current_step = 1, 
                cooking_started_at = NOW(),
                status = 'COOKING'
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderDetailId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logger.error("Error starting cooking: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Mark item as ready
     */
    public boolean completeItem(int orderDetailId) {
        String sql = """
            UPDATE order_details 
            SET status = 'READY',
                completed_at = NOW()
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderDetailId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logger.error("Error completing item: {}", e.getMessage());
            return false;
        }
    }
}
