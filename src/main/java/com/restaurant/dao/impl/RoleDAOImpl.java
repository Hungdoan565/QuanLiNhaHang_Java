package com.restaurant.dao.impl;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.dao.interfaces.IRoleDAO;
import com.restaurant.model.Role;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Role DAO Implementation
 */
public class RoleDAOImpl implements IRoleDAO {
    
    private static final Logger logger = LogManager.getLogger(RoleDAOImpl.class);
    
    @Override
    public List<Role> findAll() {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT id, name, description, permissions, created_at FROM roles ORDER BY id";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                roles.add(mapResultSetToRole(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching all roles", e);
        }
        
        return roles;
    }
    
    @Override
    public Optional<Role> findById(int id) {
        String sql = "SELECT id, name, description, permissions, created_at FROM roles WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRole(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching role by id: {}", id, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<Role> findByName(String name) {
        String sql = "SELECT id, name, description, permissions, created_at FROM roles WHERE name = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRole(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching role by name: {}", name, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public boolean insert(Role role) {
        String sql = "INSERT INTO roles (name, description, permissions) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, role.getName());
            stmt.setString(2, role.getDescription());
            stmt.setString(3, role.getPermissions());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        role.setId(generatedKeys.getInt(1));
                    }
                }
                logger.info("Role created: {}", role.getName());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error inserting role: {}", role.getName(), e);
        }
        
        return false;
    }
    
    @Override
    public boolean update(Role role) {
        String sql = "UPDATE roles SET name = ?, description = ?, permissions = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, role.getName());
            stmt.setString(2, role.getDescription());
            stmt.setString(3, role.getPermissions());
            stmt.setInt(4, role.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Role updated: {}", role.getName());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error updating role: {}", role.getId(), e);
        }
        
        return false;
    }
    
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM roles WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Role deleted: {}", id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error deleting role: {}", id, e);
        }
        
        return false;
    }
    
    /**
     * Map ResultSet row to Role object
     */
    private Role mapResultSetToRole(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId(rs.getInt("id"));
        role.setName(rs.getString("name"));
        role.setDescription(rs.getString("description"));
        role.setPermissions(rs.getString("permissions"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            role.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return role;
    }
}
