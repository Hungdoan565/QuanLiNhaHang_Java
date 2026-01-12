package com.restaurant.dao.impl;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.dao.interfaces.IUserDAO;
import com.restaurant.model.Role;
import com.restaurant.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * User DAO Implementation
 */
public class UserDAOImpl implements IUserDAO {
    
    private static final Logger logger = LogManager.getLogger(UserDAOImpl.class);
    
    // SQL queries with JOIN to eager load Role
    private static final String SELECT_BASE = """
        SELECT u.id, u.username, u.password_hash, u.full_name, u.phone, u.email, 
               u.avatar_path, u.role_id, u.is_active, u.last_login, u.created_at, u.updated_at,
               r.name as role_name, r.description as role_description, r.permissions as role_permissions
        FROM users u
        LEFT JOIN roles r ON u.role_id = r.id
        """;
    
    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE u.is_active = TRUE ORDER BY u.full_name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching all users", e);
        }
        
        return users;
    }
    
    @Override
    public List<User> findAllIncludingInactive() {
        List<User> users = new ArrayList<>();
        String sql = SELECT_BASE + " ORDER BY u.is_active DESC, u.full_name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching all users including inactive", e);
        }
        
        return users;
    }
    
    @Override
    public Optional<User> findById(int id) {
        String sql = SELECT_BASE + " WHERE u.id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching user by id: {}", id, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<User> findByUsername(String username) {
        String sql = SELECT_BASE + " WHERE u.username = ? AND u.is_active = TRUE";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching user by username: {}", username, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<User> findByRoleId(int roleId) {
        List<User> users = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE u.role_id = ? AND u.is_active = TRUE ORDER BY u.full_name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, roleId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching users by role: {}", roleId, e);
        }
        
        return users;
    }
    
    @Override
    public List<User> search(String keyword) {
        List<User> users = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE u.is_active = TRUE AND (u.username LIKE ? OR u.full_name LIKE ?) ORDER BY u.full_name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error searching users: {}", keyword, e);
        }
        
        return users;
    }
    
    @Override
    public boolean insert(User user) {
        String sql = """
            INSERT INTO users (username, password_hash, full_name, phone, email, avatar_path, role_id, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getPhone());
            stmt.setString(5, user.getEmail());
            stmt.setString(6, user.getAvatarPath());
            stmt.setInt(7, user.getRoleId());
            stmt.setBoolean(8, user.isActive());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                logger.info("User created: {}", user.getUsername());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error inserting user: {}", user.getUsername(), e);
        }
        
        return false;
    }
    
    @Override
    public boolean update(User user) {
        String sql = """
            UPDATE users SET full_name = ?, phone = ?, email = ?, avatar_path = ?, role_id = ?
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getPhone());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getAvatarPath());
            stmt.setInt(5, user.getRoleId());
            stmt.setInt(6, user.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("User updated: {}", user.getUsername());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error updating user: {}", user.getId(), e);
        }
        
        return false;
    }
    
    @Override
    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, newPasswordHash);
            stmt.setInt(2, userId);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Password updated for user id: {}", userId);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error updating password for user: {}", userId, e);
        }
        
        return false;
    }
    
    @Override
    public boolean updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = NOW() WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating last login for user: {}", userId, e);
        }
        
        return false;
    }
    
    @Override
    public boolean deactivate(int id) {
        String sql = "UPDATE users SET is_active = FALSE WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("User deactivated: {}", id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error deactivating user: {}", id, e);
        }
        
        return false;
    }
    
    @Override
    public boolean activate(int id) {
        String sql = "UPDATE users SET is_active = TRUE WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("User activated: {}", id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error activating user: {}", id, e);
        }
        
        return false;
    }
    
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("User deleted: {}", id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error deleting user: {}", id, e);
        }
        
        return false;
    }
    
    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error checking username existence: {}", username, e);
        }
        
        return false;
    }
    
    /**
     * Map ResultSet row to User object (with eager loaded Role)
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setEmail(rs.getString("email"));
        user.setAvatarPath(rs.getString("avatar_path"));
        user.setRoleId(rs.getInt("role_id"));
        user.setActive(rs.getBoolean("is_active"));
        
        // Timestamps
        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        // Eager load Role
        int roleId = rs.getInt("role_id");
        if (roleId > 0) {
            Role role = new Role();
            role.setId(roleId);
            role.setName(rs.getString("role_name"));
            role.setDescription(rs.getString("role_description"));
            role.setPermissions(rs.getString("role_permissions"));
            user.setRole(role);
        }
        
        return user;
    }
}
