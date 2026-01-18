package com.restaurant.dao.interfaces;

import com.restaurant.model.Product;
import java.util.List;
import java.util.Optional;

/**
 * Product DAO Interface
 */
public interface IProductDAO {
    
    /**
     * Find all active products
     */
    List<Product> findAll();
    
    /**
     * Find all products including inactive
     */
    List<Product> findAllIncludingInactive();
    
    /**
     * Find products by category
     */
    List<Product> findByCategory(int categoryId);
    
    /**
     * Find product by ID
     */
    Optional<Product> findById(int id);
    
    /**
     * Find product by name
     */
    Optional<Product> findByName(String name);
    
    /**
     * Search products by keyword
     */
    List<Product> search(String keyword);
    
    /**
     * Find available products only
     */
    List<Product> findAvailable();
    
    /**
     * Insert new product
     */
    boolean insert(Product product);
    
    /**
     * Update product
     */
    boolean update(Product product);
    
    /**
     * Update product availability
     */
    boolean updateAvailability(int productId, boolean available);
    
    /**
     * Deactivate product (soft delete)
     */
    boolean deactivate(int id);
    
    /**
     * Activate product
     */
    boolean activate(int id);
    
    /**
     * Hard delete product
     */
    boolean delete(int id);
}
