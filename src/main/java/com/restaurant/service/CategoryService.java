package com.restaurant.service;

import com.restaurant.dao.impl.CategoryDAOImpl;
import com.restaurant.dao.interfaces.ICategoryDAO;
import com.restaurant.model.Category;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Category operations
 */
public class CategoryService {
    
    private static final Logger logger = LogManager.getLogger(CategoryService.class);
    private static CategoryService instance;
    
    private final ICategoryDAO categoryDAO;
    
    private CategoryService() {
        this.categoryDAO = new CategoryDAOImpl();
    }
    
    public static synchronized CategoryService getInstance() {
        if (instance == null) {
            instance = new CategoryService();
        }
        return instance;
    }
    
    /**
     * Get all active categories
     */
    public List<Category> getAllCategories() {
        return categoryDAO.findAll();
    }
    
    /**
     * Get all categories including inactive
     */
    public List<Category> getAllCategoriesIncludingInactive() {
        return categoryDAO.findAllIncludingInactive();
    }
    
    /**
     * Get category by ID
     */
    public Optional<Category> getCategoryById(int id) {
        return categoryDAO.findById(id);
    }
    
    /**
     * Create new category
     */
    public ServiceResult<Category> createCategory(Category category) {
        // Validation
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            return ServiceResult.error("Tên danh mục không được để trống");
        }
        
        // Check duplicate
        Optional<Category> existing = categoryDAO.findByName(category.getName().trim());
        if (existing.isPresent()) {
            return ServiceResult.error("Danh mục '" + category.getName() + "' đã tồn tại");
        }
        
        // Set defaults
        category.setName(category.getName().trim());
        if (category.getIcon() == null || category.getIcon().isEmpty()) {
            category.setIcon("🍽️");
        }
        if (category.getPrinterName() == null || category.getPrinterName().isEmpty()) {
            category.setPrinterName("Kitchen_Printer");
        }
        category.setActive(true);
        
        boolean success = categoryDAO.insert(category);
        if (success) {
            logger.info("Category created: {}", category.getName());
            return ServiceResult.success(category, "Đã tạo danh mục: " + category.getName());
        }
        
        return ServiceResult.error("Không thể tạo danh mục");
    }
    
    /**
     * Update category
     */
    public ServiceResult<Category> updateCategory(Category category) {
        // Validation
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            return ServiceResult.error("Tên danh mục không được để trống");
        }
        
        // Check exists
        Optional<Category> existing = categoryDAO.findById(category.getId());
        if (existing.isEmpty()) {
            return ServiceResult.error("Không tìm thấy danh mục");
        }
        
        // Check duplicate name (exclude self)
        Optional<Category> duplicate = categoryDAO.findByName(category.getName().trim());
        if (duplicate.isPresent() && duplicate.get().getId() != category.getId()) {
            return ServiceResult.error("Danh mục '" + category.getName() + "' đã tồn tại");
        }
        
        category.setName(category.getName().trim());
        
        boolean success = categoryDAO.update(category);
        if (success) {
            logger.info("Category updated: {}", category.getName());
            return ServiceResult.success(category, "Đã cập nhật danh mục");
        }
        
        return ServiceResult.error("Không thể cập nhật danh mục");
    }
    
    /**
     * Delete category (soft delete)
     */
    public ServiceResult<Void> deleteCategory(int categoryId) {
        // Check exists
        Optional<Category> existing = categoryDAO.findById(categoryId);
        if (existing.isEmpty()) {
            return ServiceResult.error("Không tìm thấy danh mục");
        }
        
        // Check has products
        if (categoryDAO.hasProducts(categoryId)) {
            int count = categoryDAO.getProductCount(categoryId);
            return ServiceResult.error("Không thể xóa danh mục có " + count + " món");
        }
        
        boolean success = categoryDAO.deactivate(categoryId);
        if (success) {
            logger.info("Category deleted: {}", categoryId);
            return ServiceResult.success(null, "Đã xóa danh mục");
        }
        
        return ServiceResult.error("Không thể xóa danh mục");
    }
    
    /**
     * Get product count for category
     */
    public int getProductCount(int categoryId) {
        return categoryDAO.getProductCount(categoryId);
    }
}
