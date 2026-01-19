package com.restaurant.service;

import com.restaurant.dao.impl.UserDAOImpl;
import com.restaurant.dao.interfaces.IUserDAO;
import com.restaurant.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * User Service - Business logic layer for user management
 */
public class UserService {
    
    private static final Logger logger = LogManager.getLogger(UserService.class);
    private static UserService instance;
    private final IUserDAO userDAO;
    
    private UserService() {
        this.userDAO = new UserDAOImpl();
    }
    
    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }
    
    /**
     * Get all active users
     */
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }
    
    /**
     * Get all users including inactive
     */
    public List<User> getAllUsersIncludingInactive() {
        return userDAO.findAllIncludingInactive();
    }
    
    /**
     * Get user by ID
     */
    public Optional<User> getUserById(int id) {
        return userDAO.findById(id);
    }
    
    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return userDAO.findByUsername(username);
    }
    
    /**
     * Get users by role
     */
    public List<User> getUsersByRole(int roleId) {
        return userDAO.findByRoleId(roleId);
    }
    
    /**
     * Search users
     */
    public List<User> searchUsers(String keyword) {
        return userDAO.search(keyword);
    }
    
    /**
     * Create new user
     */
    public boolean createUser(User user) {
        // Validate username doesn't exist
        if (userDAO.existsByUsername(user.getUsername())) {
            logger.warn("Username already exists: {}", user.getUsername());
            return false;
        }
        
        boolean result = userDAO.insert(user);
        if (result) {
            logger.info("User created successfully: {}", user.getUsername());
        }
        return result;
    }
    
    /**
     * Update user
     */
    public boolean updateUser(User user) {
        boolean result = userDAO.update(user);
        if (result) {
            logger.info("User updated successfully: {}", user.getId());
        }
        return result;
    }
    
    /**
     * Update user password
     */
    public boolean updatePassword(int userId, String newPasswordHash) {
        boolean result = userDAO.updatePassword(userId, newPasswordHash);
        if (result) {
            logger.info("Password updated for user: {}", userId);
        }
        return result;
    }
    
    /**
     * Update last login time
     */
    public boolean updateLastLogin(int userId) {
        return userDAO.updateLastLogin(userId);
    }
    
    /**
     * Deactivate user (soft delete)
     */
    public boolean deactivateUser(int id) {
        boolean result = userDAO.deactivate(id);
        if (result) {
            logger.info("User deactivated: {}", id);
        }
        return result;
    }
    
    /**
     * Activate user
     */
    public boolean activateUser(int id) {
        boolean result = userDAO.activate(id);
        if (result) {
            logger.info("User activated: {}", id);
        }
        return result;
    }
    
    /**
     * Delete user permanently
     */
    public boolean deleteUser(int id) {
        boolean result = userDAO.delete(id);
        if (result) {
            logger.info("User deleted permanently: {}", id);
        }
        return result;
    }
    
    /**
     * Check if username exists
     */
    public boolean usernameExists(String username) {
        return userDAO.existsByUsername(username);
    }
}
