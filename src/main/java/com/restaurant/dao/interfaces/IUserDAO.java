package com.restaurant.dao.interfaces;

import com.restaurant.model.User;
import java.util.List;
import java.util.Optional;

/**
 * User Data Access Object Interface
 */
public interface IUserDAO {
    
    /**
     * Get all active users
     */
    List<User> findAll();
    
    /**
     * Get all users (including inactive)
     */
    List<User> findAllIncludingInactive();
    
    /**
     * Find user by ID
     */
    Optional<User> findById(int id);
    
    /**
     * Find user by username (for login)
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find users by role
     */
    List<User> findByRoleId(int roleId);
    
    /**
     * Search users by name or username
     */
    List<User> search(String keyword);
    
    /**
     * Create new user
     */
    boolean insert(User user);
    
    /**
     * Update existing user (without password)
     */
    boolean update(User user);
    
    /**
     * Update user password
     */
    boolean updatePassword(int userId, String newPasswordHash);
    
    /**
     * Update last login time
     */
    boolean updateLastLogin(int userId);
    
    /**
     * Soft delete (set is_active = false)
     */
    boolean deactivate(int id);
    
    /**
     * Reactivate user
     */
    boolean activate(int id);
    
    /**
     * Hard delete (only for admin)
     */
    boolean delete(int id);
    
    /**
     * Check if username already exists
     */
    boolean existsByUsername(String username);
}
