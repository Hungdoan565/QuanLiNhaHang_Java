package com.restaurant.dao.impl;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.dao.interfaces.IProductDAO;
import com.restaurant.model.Category;
import com.restaurant.model.Product;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Product DAO Implementation
 */
public class ProductDAOImpl implements IProductDAO {
    
    private static final Logger logger = LogManager.getLogger(ProductDAOImpl.class);
    
    private static final String SELECT_BASE = """
        SELECT p.id, p.name, p.description, p.category_id, p.price, p.cost_price,
               p.image_path, p.is_available, p.is_active, p.created_at, p.updated_at,
               c.name as category_name, c.icon as category_icon, c.printer_name as category_printer
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        """;
    
    @Override
    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE p.is_active = TRUE ORDER BY c.display_order, p.name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching all products", e);
        }
        
        return products;
    }
    
    @Override
    public List<Product> findAllIncludingInactive() {
        List<Product> products = new ArrayList<>();
        String sql = SELECT_BASE + " ORDER BY p.is_active DESC, c.display_order, p.name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching all products including inactive", e);
        }
        
        return products;
    }
    
    @Override
    public List<Product> findByCategory(int categoryId) {
        List<Product> products = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE p.category_id = ? AND p.is_active = TRUE ORDER BY p.name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, categoryId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching products by category: {}", categoryId, e);
        }
        
        return products;
    }
    
    @Override
    public Optional<Product> findById(int id) {
        String sql = SELECT_BASE + " WHERE p.id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching product by id: {}", id, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<Product> findByName(String name) {
        String sql = SELECT_BASE + " WHERE p.name = ? AND p.is_active = TRUE";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching product by name: {}", name, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<Product> search(String keyword) {
        List<Product> products = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE p.is_active = TRUE AND (p.name LIKE ? OR p.description LIKE ?) ORDER BY p.name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error searching products: {}", keyword, e);
        }
        
        return products;
    }
    
    @Override
    public List<Product> findAvailable() {
        List<Product> products = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE p.is_active = TRUE AND p.is_available = TRUE ORDER BY c.display_order, p.name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching available products", e);
        }
        
        return products;
    }
    
    @Override
    public boolean insert(Product product) {
        String sql = """
            INSERT INTO products (name, description, category_id, price, cost_price, image_path, is_available, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getDescription());
            stmt.setInt(3, product.getCategoryId());
            stmt.setBigDecimal(4, product.getPrice());
            stmt.setBigDecimal(5, product.getCostPrice());
            stmt.setString(6, product.getImagePath());
            stmt.setBoolean(7, product.isAvailable());
            stmt.setBoolean(8, product.isActive());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        product.setId(generatedKeys.getInt(1));
                    }
                }
                logger.info("Product created: {}", product.getName());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error inserting product: {}", product.getName(), e);
        }
        
        return false;
    }
    
    @Override
    public boolean update(Product product) {
        String sql = """
            UPDATE products SET name = ?, description = ?, category_id = ?, price = ?, 
                   cost_price = ?, image_path = ?, is_available = ?
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getDescription());
            stmt.setInt(3, product.getCategoryId());
            stmt.setBigDecimal(4, product.getPrice());
            stmt.setBigDecimal(5, product.getCostPrice());
            stmt.setString(6, product.getImagePath());
            stmt.setBoolean(7, product.isAvailable());
            stmt.setInt(8, product.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Product updated: {}", product.getName());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error updating product: {}", product.getId(), e);
        }
        
        return false;
    }
    
    @Override
    public boolean updateAvailability(int productId, boolean available) {
        String sql = "UPDATE products SET is_available = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, available);
            stmt.setInt(2, productId);
            
            if (stmt.executeUpdate() > 0) {
                logger.info("Product {} availability: {}", productId, available);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error updating product availability: {}", productId, e);
        }
        
        return false;
    }
    
    @Override
    public boolean deactivate(int id) {
        String sql = "UPDATE products SET is_active = FALSE WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            if (stmt.executeUpdate() > 0) {
                logger.info("Product deactivated: {}", id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error deactivating product: {}", id, e);
        }
        
        return false;
    }
    
    @Override
    public boolean activate(int id) {
        String sql = "UPDATE products SET is_active = TRUE WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            if (stmt.executeUpdate() > 0) {
                logger.info("Product activated: {}", id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error activating product: {}", id, e);
        }
        
        return false;
    }
    
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            if (stmt.executeUpdate() > 0) {
                logger.info("Product deleted: {}", id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error deleting product: {}", id, e);
        }
        
        return false;
    }
    
    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getInt("id"));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        product.setCategoryId(rs.getInt("category_id"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setCostPrice(rs.getBigDecimal("cost_price"));
        product.setImagePath(rs.getString("image_path"));
        product.setAvailable(rs.getBoolean("is_available"));
        product.setActive(rs.getBoolean("is_active"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            product.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            product.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        // Eager load Category
        int categoryId = rs.getInt("category_id");
        if (categoryId > 0) {
            Category category = new Category();
            category.setId(categoryId);
            category.setName(rs.getString("category_name"));
            category.setIcon(rs.getString("category_icon"));
            category.setPrinterName(rs.getString("category_printer"));
            product.setCategory(category);
        }
        
        return product;
    }
}
