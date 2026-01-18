package com.restaurant.dao.impl;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.dao.interfaces.ICategoryDAO;
import com.restaurant.model.Category;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Category DAO Implementation
 */
public class CategoryDAOImpl implements ICategoryDAO {
    
    private static final Logger logger = LogManager.getLogger(CategoryDAOImpl.class);
    
    private static final String SELECT_BASE = """
        SELECT id, name, icon, display_order, printer_name, is_active, created_at, updated_at
        FROM categories
        """;
    
    @Override
    public List<Category> findAll() {
        List<Category> categories = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE is_active = TRUE ORDER BY display_order, name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching all categories", e);
        }
        
        return categories;
    }
    
    @Override
    public List<Category> findAllIncludingInactive() {
        List<Category> categories = new ArrayList<>();
        String sql = SELECT_BASE + " ORDER BY is_active DESC, display_order, name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching all categories including inactive", e);
        }
        
        return categories;
    }
    
    @Override
    public Optional<Category> findById(int id) {
        String sql = SELECT_BASE + " WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCategory(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching category by id: {}", id, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<Category> findByName(String name) {
        String sql = SELECT_BASE + " WHERE name = ? AND is_active = TRUE";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCategory(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching category by name: {}", name, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public boolean insert(Category category) {
        String sql = """
            INSERT INTO categories (name, icon, display_order, printer_name, is_active)
            VALUES (?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getIcon());
            stmt.setInt(3, category.getDisplayOrder());
            stmt.setString(4, category.getPrinterName());
            stmt.setBoolean(5, category.isActive());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        category.setId(generatedKeys.getInt(1));
                    }
                }
                logger.info("Category created: {}", category.getName());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error inserting category: {}", category.getName(), e);
        }
        
        return false;
    }
    
    @Override
    public boolean update(Category category) {
        String sql = """
            UPDATE categories SET name = ?, icon = ?, display_order = ?, printer_name = ?
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getIcon());
            stmt.setInt(3, category.getDisplayOrder());
            stmt.setString(4, category.getPrinterName());
            stmt.setInt(5, category.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Category updated: {}", category.getName());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error updating category: {}", category.getId(), e);
        }
        
        return false;
    }
    
    @Override
    public boolean deactivate(int id) {
        String sql = "UPDATE categories SET is_active = FALSE WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            if (stmt.executeUpdate() > 0) {
                logger.info("Category deactivated: {}", id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error deactivating category: {}", id, e);
        }
        
        return false;
    }
    
    @Override
    public boolean activate(int id) {
        String sql = "UPDATE categories SET is_active = TRUE WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            if (stmt.executeUpdate() > 0) {
                logger.info("Category activated: {}", id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error activating category: {}", id, e);
        }
        
        return false;
    }
    
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM categories WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            if (stmt.executeUpdate() > 0) {
                logger.info("Category deleted: {}", id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error deleting category: {}", id, e);
        }
        
        return false;
    }
    
    @Override
    public boolean hasProducts(int categoryId) {
        return getProductCount(categoryId) > 0;
    }
    
    @Override
    public int getProductCount(int categoryId) {
        String sql = "SELECT COUNT(*) FROM products WHERE category_id = ? AND is_active = TRUE";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, categoryId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error counting products for category: {}", categoryId, e);
        }
        
        return 0;
    }
    
    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getInt("id"));
        category.setName(rs.getString("name"));
        category.setIcon(rs.getString("icon"));
        category.setDisplayOrder(rs.getInt("display_order"));
        category.setPrinterName(rs.getString("printer_name"));
        category.setActive(rs.getBoolean("is_active"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            category.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            category.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return category;
    }
}
