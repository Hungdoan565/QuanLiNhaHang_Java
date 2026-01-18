package com.restaurant.dao.interfaces;

import com.restaurant.model.Category;
import java.util.List;
import java.util.Optional;

/**
 * Category DAO Interface
 */
public interface ICategoryDAO {
    
    /**
     * Find all active categories
     */
    List<Category> findAll();
    
    /**
     * Find all categories including inactive
     */
    List<Category> findAllIncludingInactive();
    
    /**
     * Find category by ID
     */
    Optional<Category> findById(int id);
    
    /**
     * Find category by name
     */
    Optional<Category> findByName(String name);
    
    /**
     * Insert new category
     */
    boolean insert(Category category);
    
    /**
     * Update category
     */
    boolean update(Category category);
    
    /**
     * Delete category (soft delete - set inactive)
     */
    boolean deactivate(int id);
    
    /**
     * Activate category
     */
    boolean activate(int id);
    
    /**
     * Hard delete category
     */
    boolean delete(int id);
    
    /**
     * Check if category has products
     */
    boolean hasProducts(int categoryId);
    
    /**
     * Get product count for each category
     */
    int getProductCount(int categoryId);
}
