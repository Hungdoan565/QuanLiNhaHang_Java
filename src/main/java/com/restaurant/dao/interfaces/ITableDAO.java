package com.restaurant.dao.interfaces;

import com.restaurant.model.Table;
import java.util.List;
import java.util.Optional;

/**
 * Table DAO Interface
 */
public interface ITableDAO {
    
    /**
     * Find all active tables
     */
    List<Table> findAll();
    
    /**
     * Find all tables including inactive
     */
    List<Table> findAllIncludingInactive();
    
    /**
     * Find tables by area
     */
    List<Table> findByArea(String area);
    
    /**
     * Find tables by status
     */
    List<Table> findByStatus(Table.TableStatus status);
    
    /**
     * Find table by ID
     */
    Optional<Table> findById(int id);
    
    /**
     * Find table by name
     */
    Optional<Table> findByName(String name);
    
    /**
     * Insert new table
     */
    boolean insert(Table table);
    
    /**
     * Update table
     */
    boolean update(Table table);
    
    /**
     * Update table status
     */
    boolean updateStatus(int tableId, Table.TableStatus status);
    
    /**
     * Open table (set to OCCUPIED with order info)
     */
    boolean openTable(int tableId, int orderId, int guestCount);
    
    /**
     * Close table (set to AVAILABLE, clear order info)
     */
    boolean closeTable(int tableId);
    
    /**
     * Deactivate table
     */
    boolean deactivate(int id);
    
    /**
     * Activate table
     */
    boolean activate(int id);
    
    /**
     * Delete table
     */
    boolean delete(int id);
    
    /**
     * Get distinct areas
     */
    List<String> getAreas();
    
    /**
     * Count available tables
     */
    int countAvailable();
    
    /**
     * Count occupied tables
     */
    int countOccupied();
}
