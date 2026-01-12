package com.restaurant.dao.interfaces;

import com.restaurant.model.Role;
import java.util.List;
import java.util.Optional;

/**
 * Role Data Access Object Interface
 */
public interface IRoleDAO {
    
    /**
     * Get all roles
     */
    List<Role> findAll();
    
    /**
     * Find role by ID
     */
    Optional<Role> findById(int id);
    
    /**
     * Find role by name
     */
    Optional<Role> findByName(String name);
    
    /**
     * Create new role
     */
    boolean insert(Role role);
    
    /**
     * Update existing role
     */
    boolean update(Role role);
    
    /**
     * Delete role by ID
     */
    boolean delete(int id);
}
