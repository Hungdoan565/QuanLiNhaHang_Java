package com.restaurant.service;

import com.restaurant.dao.impl.TableDAOImpl;
import com.restaurant.dao.interfaces.ITableDAO;
import com.restaurant.model.Table;
import com.restaurant.model.Table.TableStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Table operations
 */
public class TableService {
    
    private static final Logger logger = LogManager.getLogger(TableService.class);
    private static TableService instance;
    
    private final ITableDAO tableDAO;
    
    private TableService() {
        this.tableDAO = new TableDAOImpl();
    }
    
    public static synchronized TableService getInstance() {
        if (instance == null) {
            instance = new TableService();
        }
        return instance;
    }
    
    /**
     * Get all active tables
     */
    public List<Table> getAllTables() {
        return tableDAO.findAll();
    }
    
    /**
     * Get all tables including inactive
     */
    public List<Table> getAllTablesIncludingInactive() {
        return tableDAO.findAllIncludingInactive();
    }
    
    /**
     * Get tables by area
     */
    public List<Table> getTablesByArea(String area) {
        if (area == null || area.equals("Tất cả")) {
            return tableDAO.findAll();
        }
        return tableDAO.findByArea(area);
    }
    
    /**
     * Get tables by status
     */
    public List<Table> getTablesByStatus(TableStatus status) {
        return tableDAO.findByStatus(status);
    }
    
    /**
     * Get available tables
     */
    public List<Table> getAvailableTables() {
        return tableDAO.findByStatus(TableStatus.AVAILABLE);
    }
    
    /**
     * Get distinct areas
     */
    public List<String> getAreas() {
        return tableDAO.getAreas();
    }
    
    /**
     * Get table by ID
     */
    public Optional<Table> getTableById(int id) {
        return tableDAO.findById(id);
    }
    
    /**
     * Create new table
     */
    public ServiceResult<Table> createTable(Table table) {
        // Validation
        if (table.getName() == null || table.getName().trim().isEmpty()) {
            return ServiceResult.error("Tên bàn không được để trống");
        }
        
        if (table.getCapacity() <= 0) {
            return ServiceResult.error("Sức chứa phải lớn hơn 0");
        }
        
        // Check duplicate
        Optional<Table> existing = tableDAO.findByName(table.getName().trim());
        if (existing.isPresent()) {
            return ServiceResult.error("Bàn '" + table.getName() + "' đã tồn tại");
        }
        
        // Set defaults
        table.setName(table.getName().trim());
        table.setStatus(TableStatus.AVAILABLE);
        table.setActive(true);
        if (table.getArea() == null || table.getArea().isEmpty()) {
            table.setArea("Tầng 1");
        }
        
        boolean success = tableDAO.insert(table);
        if (success) {
            logger.info("Table created: {}", table.getName());
            return ServiceResult.success(table, "Đã tạo bàn: " + table.getName());
        }
        
        return ServiceResult.error("Không thể tạo bàn");
    }
    
    /**
     * Update table
     */
    public ServiceResult<Table> updateTable(Table table) {
        // Validation
        if (table.getName() == null || table.getName().trim().isEmpty()) {
            return ServiceResult.error("Tên bàn không được để trống");
        }
        
        // Check exists
        Optional<Table> existing = tableDAO.findById(table.getId());
        if (existing.isEmpty()) {
            return ServiceResult.error("Không tìm thấy bàn");
        }
        
        // Check duplicate name (exclude self)
        Optional<Table> duplicate = tableDAO.findByName(table.getName().trim());
        if (duplicate.isPresent() && duplicate.get().getId() != table.getId()) {
            return ServiceResult.error("Bàn '" + table.getName() + "' đã tồn tại");
        }
        
        table.setName(table.getName().trim());
        
        boolean success = tableDAO.update(table);
        if (success) {
            logger.info("Table updated: {}", table.getName());
            return ServiceResult.success(table, "Đã cập nhật bàn");
        }
        
        return ServiceResult.error("Không thể cập nhật bàn");
    }
    
    /**
     * Open table (set to OCCUPIED)
     */
    public ServiceResult<Table> openTable(int tableId, int orderId, int guestCount) {
        Optional<Table> existing = tableDAO.findById(tableId);
        if (existing.isEmpty()) {
            return ServiceResult.error("Không tìm thấy bàn");
        }
        
        Table table = existing.get();
        if (!table.isAvailable()) {
            return ServiceResult.error("Bàn " + table.getName() + " đang " + table.getStatus().getDisplayName());
        }
        
        if (guestCount > table.getCapacity()) {
            return ServiceResult.error("Số khách vượt quá sức chứa (tối đa " + table.getCapacity() + ")");
        }
        
        boolean success = tableDAO.openTable(tableId, orderId, guestCount);
        if (success) {
            logger.info("Table {} opened with {} guests", table.getName(), guestCount);
            return ServiceResult.success(table, "Đã mở " + table.getName());
        }
        
        return ServiceResult.error("Không thể mở bàn");
    }
    
    /**
     * Close table (set to AVAILABLE)
     */
    public ServiceResult<Void> closeTable(int tableId) {
        Optional<Table> existing = tableDAO.findById(tableId);
        if (existing.isEmpty()) {
            return ServiceResult.error("Không tìm thấy bàn");
        }
        
        boolean success = tableDAO.closeTable(tableId);
        if (success) {
            logger.info("Table {} closed", tableId);
            return ServiceResult.success(null, "Đã đóng bàn");
        }
        
        return ServiceResult.error("Không thể đóng bàn");
    }
    
    /**
     * Update table status
     */
    public ServiceResult<Void> updateStatus(int tableId, TableStatus status) {
        Optional<Table> existing = tableDAO.findById(tableId);
        if (existing.isEmpty()) {
            return ServiceResult.error("Không tìm thấy bàn");
        }
        
        boolean success = tableDAO.updateStatus(tableId, status);
        if (success) {
            logger.info("Table {} status: {}", tableId, status);
            return ServiceResult.success(null, "Đã cập nhật trạng thái");
        }
        
        return ServiceResult.error("Không thể cập nhật trạng thái");
    }
    
    /**
     * Delete table (soft delete)
     */
    public ServiceResult<Void> deleteTable(int tableId) {
        Optional<Table> existing = tableDAO.findById(tableId);
        if (existing.isEmpty()) {
            return ServiceResult.error("Không tìm thấy bàn");
        }
        
        // Check if table is occupied
        if (existing.get().getStatus() == TableStatus.OCCUPIED) {
            return ServiceResult.error("Không thể xóa bàn đang có khách");
        }
        
        boolean success = tableDAO.deactivate(tableId);
        if (success) {
            logger.info("Table deleted: {}", tableId);
            return ServiceResult.success(null, "Đã xóa bàn");
        }
        
        return ServiceResult.error("Không thể xóa bàn");
    }
    
    /**
     * Get statistics
     */
    public TableStats getStats() {
        return new TableStats(
            tableDAO.countAvailable(),
            tableDAO.countOccupied()
        );
    }
    
    /**
     * Table statistics
     */
    public record TableStats(int available, int occupied) {
        public int total() { return available + occupied; }
    }
}
