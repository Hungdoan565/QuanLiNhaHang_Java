package com.restaurant.dao;

import com.restaurant.dao.interfaces.IReservationDAO;
import com.restaurant.model.Reservation;
import com.restaurant.config.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reservation DAO Implementation
 */
public class ReservationDAO implements IReservationDAO {
    
    private static final Logger logger = LogManager.getLogger(ReservationDAO.class);
    
    @Override
    public int create(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (table_id, customer_name, customer_phone, guest_count, 
                                       reservation_time, notes, status, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, reservation.getTableId());
            stmt.setString(2, reservation.getCustomerName());
            stmt.setString(3, reservation.getCustomerPhone());
            stmt.setInt(4, reservation.getGuestCount());
            stmt.setTimestamp(5, Timestamp.valueOf(reservation.getReservationTime()));
            stmt.setString(6, reservation.getNotes());
            stmt.setString(7, reservation.getStatus().name());
            if (reservation.getCreatedBy() != null) {
                stmt.setInt(8, reservation.getCreatedBy());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    reservation.setId(id);
                    logger.info("Created reservation: {} for table {}", id, reservation.getTableId());
                    return id;
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating reservation: {}", e.getMessage(), e);
        }
        return -1;
    }
    
    @Override
    public Optional<Reservation> findById(int id) {
        String sql = """
            SELECT r.*, t.name as table_name 
            FROM reservations r 
            LEFT JOIN tables t ON r.table_id = t.id 
            WHERE r.id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding reservation by id {}: {}", id, e.getMessage(), e);
        }
        return Optional.empty();
    }
    
    @Override
    public List<Reservation> findByTableId(int tableId) {
        String sql = """
            SELECT r.*, t.name as table_name 
            FROM reservations r 
            LEFT JOIN tables t ON r.table_id = t.id 
            WHERE r.table_id = ? 
            ORDER BY r.reservation_time DESC
            """;
        
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, tableId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding reservations by table {}: {}", tableId, e.getMessage(), e);
        }
        return list;
    }
    
    @Override
    public List<Reservation> findByDate(LocalDate date) {
        String sql = """
            SELECT r.*, t.name as table_name 
            FROM reservations r 
            LEFT JOIN tables t ON r.table_id = t.id 
            WHERE DATE(r.reservation_time) = ? 
            ORDER BY r.reservation_time
            """;
        
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding reservations by date {}: {}", date, e.getMessage(), e);
        }
        return list;
    }
    
    @Override
    public List<Reservation> findByPhone(String phone) {
        String sql = """
            SELECT r.*, t.name as table_name 
            FROM reservations r 
            LEFT JOIN tables t ON r.table_id = t.id 
            WHERE r.customer_phone LIKE ? 
            ORDER BY r.reservation_time DESC
            """;
        
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + phone + "%");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding reservations by phone {}: {}", phone, e.getMessage(), e);
        }
        return list;
    }
    
    @Override
    public List<Reservation> findNeedingReminder() {
        String sql = """
            SELECT r.*, t.name as table_name 
            FROM reservations r 
            LEFT JOIN tables t ON r.table_id = t.id 
            WHERE r.status IN ('PENDING', 'CONFIRMED')
              AND r.notified = FALSE
              AND r.reservation_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 15 MINUTE)
            ORDER BY r.reservation_time
            """;
        
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding reservations needing reminder: {}", e.getMessage(), e);
        }
        return list;
    }
    
    @Override
    public List<Reservation> findUpcomingToday() {
        String sql = """
            SELECT r.*, t.name as table_name 
            FROM reservations r 
            LEFT JOIN tables t ON r.table_id = t.id 
            WHERE r.status IN ('PENDING', 'CONFIRMED')
              AND DATE(r.reservation_time) = CURDATE()
              AND r.reservation_time >= NOW()
            ORDER BY r.reservation_time
            """;
        
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding upcoming reservations: {}", e.getMessage(), e);
        }
        return list;
    }
    
    @Override
    public boolean update(Reservation reservation) {
        String sql = """
            UPDATE reservations 
            SET customer_name = ?, customer_phone = ?, guest_count = ?, 
                reservation_time = ?, notes = ?, status = ?
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, reservation.getCustomerName());
            stmt.setString(2, reservation.getCustomerPhone());
            stmt.setInt(3, reservation.getGuestCount());
            stmt.setTimestamp(4, Timestamp.valueOf(reservation.getReservationTime()));
            stmt.setString(5, reservation.getNotes());
            stmt.setString(6, reservation.getStatus().name());
            stmt.setInt(7, reservation.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating reservation {}: {}", reservation.getId(), e.getMessage(), e);
        }
        return false;
    }
    
    @Override
    public boolean updateStatus(int id, Reservation.Status status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            stmt.setInt(2, id);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating reservation status {}: {}", id, e.getMessage(), e);
        }
        return false;
    }
    
    @Override
    public boolean markNotified(int id) {
        String sql = "UPDATE reservations SET notified = TRUE WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error marking reservation notified {}: {}", id, e.getMessage(), e);
        }
        return false;
    }
    
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM reservations WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting reservation {}: {}", id, e.getMessage(), e);
        }
        return false;
    }
    
    @Override
    public Optional<Reservation> findActiveForTable(int tableId) {
        // Find reservations that are:
        // 1. For this table
        // 2. PENDING, CONFIRMED, or ARRIVED status
        // 3. Today or in the future (not past reservations)
        String sql = """
            SELECT r.*, t.name as table_name 
            FROM reservations r 
            LEFT JOIN tables t ON r.table_id = t.id 
            WHERE r.table_id = ?
              AND r.status IN ('PENDING', 'CONFIRMED', 'ARRIVED')
              AND r.reservation_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
            ORDER BY r.reservation_time
            LIMIT 1
            """;
        
        logger.info("findActiveForTable: Looking for table_id={}", tableId);
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, tableId);
            logger.info("Executing query for table_id={}", tableId);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Reservation r = mapResultSet(rs);
                logger.info("Found reservation: id={}, customer={}, time={}, status={}", 
                    r.getId(), r.getCustomerName(), r.getReservationTime(), r.getStatus());
                return Optional.of(r);
            } else {
                logger.warn("No active reservation found for table_id={}", tableId);
                
                // Debug: Check what reservations exist for this table
                String debugSql = "SELECT id, status, reservation_time FROM reservations WHERE table_id = ? ORDER BY reservation_time DESC LIMIT 3";
                try (PreparedStatement debugStmt = conn.prepareStatement(debugSql)) {
                    debugStmt.setInt(1, tableId);
                    ResultSet debugRs = debugStmt.executeQuery();
                    while (debugRs.next()) {
                        logger.info("DEBUG - Reservation id={}, status={}, time={}", 
                            debugRs.getInt("id"), 
                            debugRs.getString("status"), 
                            debugRs.getTimestamp("reservation_time"));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding active reservation for table {}: {}", tableId, e.getMessage(), e);
        }
        return Optional.empty();
    }
    
    private Reservation mapResultSet(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setTableId(rs.getInt("table_id"));
        r.setCustomerName(rs.getString("customer_name"));
        r.setCustomerPhone(rs.getString("customer_phone"));
        r.setGuestCount(rs.getInt("guest_count"));
        
        Timestamp ts = rs.getTimestamp("reservation_time");
        if (ts != null) {
            r.setReservationTime(ts.toLocalDateTime());
        }
        
        r.setNotes(rs.getString("notes"));
        r.setStatus(Reservation.Status.valueOf(rs.getString("status")));
        
        // Safely get notified column (might not exist in older schemas)
        try {
            r.setNotified(rs.getBoolean("notified"));
        } catch (SQLException ignored) {
            r.setNotified(false);
        }
        
        // Safely get created_by (might be NULL)
        try {
            int createdBy = rs.getInt("created_by");
            if (!rs.wasNull()) {
                r.setCreatedBy(createdBy);
            }
        } catch (SQLException ignored) {}
        
        // Safely get created_at
        try {
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                r.setCreatedAt(createdAt.toLocalDateTime());
            }
        } catch (SQLException ignored) {}
        
        // Safely get updated_at (might not exist in older schemas)
        try {
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                r.setUpdatedAt(updatedAt.toLocalDateTime());
            }
        } catch (SQLException ignored) {}
        
        // Table name from JOIN
        try {
            r.setTableName(rs.getString("table_name"));
        } catch (SQLException ignored) {}
        
        return r;
    }
    
    @Override
    public boolean hasTimeConflict(int tableId, LocalDateTime startTime, int durationMinutes, int excludeReservationId) {
        // Check for overlapping reservations within the time window
        // A conflict exists if another reservation overlaps with [startTime, startTime + duration]
        String sql = """
            SELECT COUNT(*) FROM reservations 
            WHERE table_id = ?
              AND status IN ('PENDING', 'CONFIRMED')
              AND id != ?
              AND reservation_time BETWEEN DATE_SUB(?, INTERVAL ? MINUTE) AND DATE_ADD(?, INTERVAL ? MINUTE)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, tableId);
            stmt.setInt(2, excludeReservationId > 0 ? excludeReservationId : -1);
            stmt.setTimestamp(3, Timestamp.valueOf(startTime));
            stmt.setInt(4, durationMinutes);
            stmt.setTimestamp(5, Timestamp.valueOf(startTime));
            stmt.setInt(6, durationMinutes);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking time conflict for table {}: {}", tableId, e.getMessage(), e);
        }
        return false;
    }
    
    @Override
    public List<Reservation> findNoShows(int thresholdMinutes) {
        String sql = """
            SELECT r.*, t.name as table_name 
            FROM reservations r 
            LEFT JOIN tables t ON r.table_id = t.id 
            WHERE r.status IN ('PENDING', 'CONFIRMED')
              AND r.reservation_time < DATE_SUB(NOW(), INTERVAL ? MINUTE)
              AND DATE(r.reservation_time) = CURDATE()
            ORDER BY r.reservation_time
            """;
        
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, thresholdMinutes);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding no-shows: {}", e.getMessage(), e);
        }
        return list;
    }
    
    @Override
    public int markNoShows(int thresholdMinutes) {
        String sql = """
            UPDATE reservations 
            SET status = 'NO_SHOW'
            WHERE status IN ('PENDING', 'CONFIRMED')
              AND reservation_time < DATE_SUB(NOW(), INTERVAL ? MINUTE)
              AND DATE(reservation_time) = CURDATE()
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, thresholdMinutes);
            int count = stmt.executeUpdate();
            if (count > 0) {
                logger.info("Marked {} reservations as NO_SHOW (threshold: {} minutes)", count, thresholdMinutes);
            }
            return count;
        } catch (SQLException e) {
            logger.error("Error marking no-shows: {}", e.getMessage(), e);
        }
        return 0;
    }
}
