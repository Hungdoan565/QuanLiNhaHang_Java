package com.restaurant.service;

import com.restaurant.dao.impl.ProductDAOImpl;
import com.restaurant.dao.interfaces.IProductDAO;
import com.restaurant.model.Product;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Product operations
 */
public class ProductService {
    
    private static final Logger logger = LogManager.getLogger(ProductService.class);
    private static ProductService instance;
    
    private final IProductDAO productDAO;
    
    private ProductService() {
        this.productDAO = new ProductDAOImpl();
    }
    
    public static synchronized ProductService getInstance() {
        if (instance == null) {
            instance = new ProductService();
        }
        return instance;
    }
    
    /**
     * Get all active products
     */
    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }
    
    /**
     * Get all products including inactive
     */
    public List<Product> getAllProductsIncludingInactive() {
        return productDAO.findAllIncludingInactive();
    }
    
    /**
     * Get products by category
     */
    public List<Product> getProductsByCategory(int categoryId) {
        return productDAO.findByCategory(categoryId);
    }
    
    /**
     * Get available products only (for POS)
     */
    public List<Product> getAvailableProducts() {
        return productDAO.findAvailable();
    }
    
    /**
     * Search products
     */
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productDAO.findAll();
        }
        return productDAO.search(keyword.trim());
    }
    
    /**
     * Get product by ID
     */
    public Optional<Product> getProductById(int id) {
        return productDAO.findById(id);
    }
    
    /**
     * Create new product
     */
    public ServiceResult<Product> createProduct(Product product) {
        // Validation
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            return ServiceResult.error("Tên món không được để trống");
        }
        
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return ServiceResult.error("Giá bán phải lớn hơn 0");
        }
        
        if (product.getCategoryId() <= 0) {
            return ServiceResult.error("Vui lòng chọn danh mục");
        }
        
        // Check duplicate
        Optional<Product> existing = productDAO.findByName(product.getName().trim());
        if (existing.isPresent()) {
            return ServiceResult.error("Món '" + product.getName() + "' đã tồn tại");
        }
        
        // Set defaults
        product.setName(product.getName().trim());
        if (product.getCostPrice() == null) {
            product.setCostPrice(BigDecimal.ZERO);
        }
        product.setActive(true);
        product.setAvailable(true);
        
        boolean success = productDAO.insert(product);
        if (success) {
            logger.info("Product created: {}", product.getName());
            return ServiceResult.success(product, "Đã tạo món: " + product.getName());
        }
        
        return ServiceResult.error("Không thể tạo món");
    }
    
    /**
     * Update product
     */
    public ServiceResult<Product> updateProduct(Product product) {
        // Validation
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            return ServiceResult.error("Tên món không được để trống");
        }
        
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return ServiceResult.error("Giá bán phải lớn hơn 0");
        }
        
        // Check exists
        Optional<Product> existing = productDAO.findById(product.getId());
        if (existing.isEmpty()) {
            return ServiceResult.error("Không tìm thấy món");
        }
        
        // Check duplicate name (exclude self)
        Optional<Product> duplicate = productDAO.findByName(product.getName().trim());
        if (duplicate.isPresent() && duplicate.get().getId() != product.getId()) {
            return ServiceResult.error("Món '" + product.getName() + "' đã tồn tại");
        }
        
        product.setName(product.getName().trim());
        
        boolean success = productDAO.update(product);
        if (success) {
            logger.info("Product updated: {}", product.getName());
            return ServiceResult.success(product, "Đã cập nhật món");
        }
        
        return ServiceResult.error("Không thể cập nhật món");
    }
    
    /**
     * Toggle product availability
     */
    public ServiceResult<Void> setAvailability(int productId, boolean available) {
        Optional<Product> existing = productDAO.findById(productId);
        if (existing.isEmpty()) {
            return ServiceResult.error("Không tìm thấy món");
        }
        
        boolean success = productDAO.updateAvailability(productId, available);
        if (success) {
            String status = available ? "còn hàng" : "hết hàng";
            logger.info("Product {} set to {}", productId, status);
            return ServiceResult.success(null, "Đã đánh dấu " + status);
        }
        
        return ServiceResult.error("Không thể cập nhật trạng thái");
    }
    
    /**
     * Delete product (soft delete)
     */
    public ServiceResult<Void> deleteProduct(int productId) {
        Optional<Product> existing = productDAO.findById(productId);
        if (existing.isEmpty()) {
            return ServiceResult.error("Không tìm thấy món");
        }
        
        boolean success = productDAO.deactivate(productId);
        if (success) {
            logger.info("Product deleted: {}", productId);
            return ServiceResult.success(null, "Đã xóa món");
        }
        
        return ServiceResult.error("Không thể xóa món");
    }
}
