package com.restaurant.dao.impl;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.dao.interfaces.ITableDAO;
import com.restaurant.model.Table;
import com.restaurant.model.Table.TableStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Table DAO Implementation
 */
public class TableDAOImpl implements ITableDAO {
    
    private static final Logger logger = LogManager.getLogger(TableDAOImpl.class);
    
    private static final String SELECT_BASE = """
        SELECT t.id, t.name, t.capacity, t.status, t.area, t.position_x, t.position_y,
               t.is_active, t.current_order_id, t.guest_count, t.occupied_since, 
               t.created_at, t.updated_at,
               o.order_code as current_order_code
        FROM tables t
        LEFT JOIN orders o ON t.current_order_id = o.id
        """;
    
    @Override
    public List<Table> findAll() {
        List<Table> tables = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE t.is_active = TRUE ORDER BY t.area, t.name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                tables.add(mapResultSetToTable(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching all tables", e);
        }
        
        return tables;
    }
    
    @Override
    public List<Table> findAllIncludingInactive() {
        List<Table> tables = new ArrayList<>();
        String sql = SELECT_BASE + " ORDER BY t.is_active DESC, t.area, t.name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                tables.add(mapResultSetToTable(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching all tables including inactive", e);
        }
        
        return tables;
    }
    
    @Override
    public List<Table> findByArea(String area) {
        List<Table> tables = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE t.area = ? AND t.is_active = TRUE ORDER BY t.name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, area);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(mapResultSetToTable(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching tables by area: {}", area, e);
        }
        
        return tables;
    }
    
    @Override
    public List<Table> findByStatus(TableStatus status) {
        List<Table> tables = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE t.status = ? AND t.is_active = TRUE ORDER BY t.area, t.name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(mapResultSetToTable(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching tables by status: {}", status, e);
        }
        
        return tables;
    }
    
    @Override
    public Optional<Table> findById(int id) {
        String sql = SELECT_BASE + " WHERE t.id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTable(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching table by id: {}", id, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<Table> findByName(String name) {
        String sql = SELECT_BASE + " WHERE t.name = ? AND t.is_active = TRUE";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTable(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching table by name: {}", name, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public boolean insert(Table table) {
        String sql = """
            INSERT INTO tables (name, capacity, status, area, position_x, position_y, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, table.getName());
            stmt.setInt(2, table.getCapacity());
            stmt.setString(3, table.getStatus().name());
            stmt.setString(4, table.getArea());
            stmt.setInt(5, table.getPositionX());
            stmt.setInt(6, table.getPositionY());
            stmt.setBoolean(7, table.isActive());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        table.setId(generatedKeys.getInt(1));
                    }
                }
                logger.info("Table created: {}", table.getName());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error inserting table: {}", table.getName(), e);
        }
        
        return false;
    }
    
    @Override
    public boolean update(Table table) {
        String sql = """
            UPDATE tables SET name = ?, capacity = ?, area = ?, position_x = ?, position_y = ?
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, table.getName());
            stmt.setInt(2, table.getCapacity());
            stmt.setString(3, table.getArea());
            stmt.setInt(4, table.getPositionX());
            stmt.setInt(5, table.getPositionY());
            stmt.setInt(6, table.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Table updated: {}", table.getName());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error updating table: {}", table.getId(), e);
        }
        
        return false;
    }
    
    @Override
    public boolean updateStatus(int tableId, TableStatus status) {
        String sql = "UPDATE tables SET status = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            stmt.setInt(2, tableId);
            
            if (stmt.executeUpdate() > 0) {
                logger.info("Table {} status: {}", tableId, status);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error updating table status: {}", tableId, e);
        }
        
        return false;
    }
    
    @Override
    public boolean openTable(int tableId, int orderId, int guestCount) {
        String sql = """
            UPDATE tables SET status = 'OCCUPIED', current_order_id = ?, 
                   guest_count = ?, occupied_since = NOW()
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            stmt.setInt(2, guestCount);
            stmt.setInt(3, tableId);
            
            if (stmt.executeUpdate() > 0) {
                logger.info("Table {} opened with order {}", tableId, orderId);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error opening table: {}", tableId, e);
        }
        
        return false;
    }
    
    @Override
    public boolean closeTable(int tableId) {
        String sql = """
            UPDATE tables SET status = 'AVAILABLE', current_order_id = NULL, 
                   guest_count = 0, occupied_since = NULL
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, tableId);
            
            if (stmt.executeUpdate() > 0) {
                logger.info("Table {} closed", tableId);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error closing table: {}", tableId, e);
        }
        
        return false;
    }
    
    @Override
    public boolean deactivate(int id) {
        String sql = "UPDATE tables SET is_active = FALSE WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            if (stmt.executeUpdate() > 0) {
                logger.info("Table deactivated: {}", id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error deactivating table: {}", id, e);
        }
        
        return false;
    }
    
    @Override
    public boolean activate(int id) {
        String sql = "UPDATE tables SET is_active = TRUE WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            if (stmt.executeUpdate() > 0) {
                logger.info("Table activated: {}", id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error activating table: {}", id, e);
        }
        
        return false;
    }
    
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM tables WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            if (stmt.executeUpdate() > 0) {
                logger.info("Table deleted: {}", id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error deleting table: {}", id, e);
        }
        
        return false;
    }
    
    @Override
    public List<String> getAreas() {
        List<String> areas = new ArrayList<>();
        String sql = "SELECT DISTINCT area FROM tables WHERE is_active = TRUE ORDER BY area";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                areas.add(rs.getString("area"));
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching table areas", e);
        }
        
        return areas;
    }
    
    @Override
    public int countAvailable() {
        return countByStatus(TableStatus.AVAILABLE);
    }
    
    @Override
    public int countOccupied() {
        return countByStatus(TableStatus.OCCUPIED);
    }
    
    private int countByStatus(TableStatus status) {
        String sql = "SELECT COUNT(*) FROM tables WHERE status = ? AND is_active = TRUE";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error counting tables by status: {}", status, e);
        }
        
        return 0;
    }
    
    private Table mapResultSetToTable(ResultSet rs) throws SQLException {
        Table table = new Table();
        table.setId(rs.getInt("id"));
        table.setName(rs.getString("name"));
        table.setCapacity(rs.getInt("capacity"));
        
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            table.setStatus(TableStatus.valueOf(statusStr));
        }
        
        table.setArea(rs.getString("area"));
        table.setPositionX(rs.getInt("position_x"));
        table.setPositionY(rs.getInt("position_y"));
        table.setActive(rs.getBoolean("is_active"));
        
        // Order info
        int orderId = rs.getInt("current_order_id");
        if (!rs.wasNull()) {
            table.setCurrentOrderId(orderId);
            table.setCurrentOrderCode(rs.getString("current_order_code"));
        }
        table.setGuestCount(rs.getInt("guest_count"));
        
        Timestamp occupiedSince = rs.getTimestamp("occupied_since");
        if (occupiedSince != null) {
            table.setOccupiedSince(occupiedSince.toLocalDateTime());
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            table.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            table.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return table;
    }
}
