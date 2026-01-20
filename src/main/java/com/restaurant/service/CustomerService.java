package com.restaurant.service;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.Customer;
import com.restaurant.model.Customer.CustomerTier;
import com.restaurant.model.Customer.Gender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer Service - Quản lý khách hàng và loyalty
 */
public class CustomerService {
    
    private static final Logger logger = LogManager.getLogger(CustomerService.class);
    private static CustomerService instance;
    
    private CustomerService() {}
    
    public static synchronized CustomerService getInstance() {
        if (instance == null) {
            instance = new CustomerService();
        }
        return instance;
    }
    
    // ==================== CRUD ====================
    
    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE is_active = TRUE ORDER BY full_name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                customers.add(mapCustomer(rs));
            }
        } catch (SQLException e) {
            logger.error("Error loading customers", e);
        }
        return customers;
    }
    
    public Customer getById(int id) {
        String sql = "SELECT * FROM customers WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapCustomer(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting customer by id", e);
        }
        return null;
    }
    
    public Customer getByPhone(String phone) {
        String sql = "SELECT * FROM customers WHERE phone = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapCustomer(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting customer by phone", e);
        }
        return null;
    }
    
    public List<Customer> searchCustomers(String keyword) {
        List<Customer> customers = new ArrayList<>();
        String sql = """
            SELECT * FROM customers 
            WHERE is_active = TRUE 
              AND (full_name LIKE ? OR phone LIKE ? OR email LIKE ?)
            ORDER BY full_name
            LIMIT 20
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String pattern = "%" + keyword + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapCustomer(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching customers", e);
        }
        return customers;
    }
    
    public boolean createCustomer(Customer customer) {
        String sql = """
            INSERT INTO customers (full_name, phone, email, birthday, gender, address, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, customer.getFullName());
            stmt.setString(2, customer.getPhone());
            stmt.setString(3, customer.getEmail());
            stmt.setDate(4, customer.getBirthday() != null ? Date.valueOf(customer.getBirthday()) : null);
            stmt.setString(5, customer.getGender() != null ? customer.getGender().name() : null);
            stmt.setString(6, customer.getAddress());
            stmt.setString(7, customer.getNotes());
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        customer.setId(keys.getInt(1));
                    }
                }
                logger.info("Created customer: {} ({})", customer.getFullName(), customer.getPhone());
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error creating customer", e);
        }
        return false;
    }
    
    public boolean updateCustomer(Customer customer) {
        String sql = """
            UPDATE customers SET 
                full_name = ?, phone = ?, email = ?, birthday = ?, 
                gender = ?, address = ?, notes = ?, tier = ?
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, customer.getFullName());
            stmt.setString(2, customer.getPhone());
            stmt.setString(3, customer.getEmail());
            stmt.setDate(4, customer.getBirthday() != null ? Date.valueOf(customer.getBirthday()) : null);
            stmt.setString(5, customer.getGender() != null ? customer.getGender().name() : null);
            stmt.setString(6, customer.getAddress());
            stmt.setString(7, customer.getNotes());
            stmt.setString(8, customer.getTier().name());
            stmt.setInt(9, customer.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating customer", e);
            return false;
        }
    }
    
    public boolean deleteCustomer(int id) {
        String sql = "UPDATE customers SET is_active = FALSE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting customer", e);
            return false;
        }
    }
    
    // ==================== LOYALTY ====================
    
    /**
     * Process order completion: update stats, earn points
     */
    public void processOrderCompletion(int customerId, int orderId, BigDecimal orderAmount) {
        Customer customer = getById(customerId);
        if (customer == null) return;
        
        // Calculate points earned
        int pointsEarned = customer.calculatePointsFromAmount(orderAmount);
        
        // Update customer stats
        String updateSql = """
            UPDATE customers SET 
                visit_count = visit_count + 1,
                total_spent = total_spent + ?,
                loyalty_points = loyalty_points + ?,
                last_visit = CURDATE(),
                tier = ?
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            
            // Update tier based on new total
            customer.setTotalSpent(customer.getTotalSpent().add(orderAmount));
            customer.updateTier();
            
            stmt.setBigDecimal(1, orderAmount);
            stmt.setInt(2, pointsEarned);
            stmt.setString(3, customer.getTier().name());
            stmt.setInt(4, customerId);
            stmt.executeUpdate();
            
            // Record loyalty transaction
            if (pointsEarned > 0) {
                recordLoyaltyTransaction(customerId, orderId, pointsEarned, "EARN", 
                    "Tích điểm từ đơn hàng #" + orderId);
            }
            
            logger.info("Processed order for customer {}: +{} points, total spent: {}", 
                customerId, pointsEarned, customer.getTotalSpent().add(orderAmount));
        } catch (SQLException e) {
            logger.error("Error processing order completion", e);
        }
    }
    
    /**
     * Redeem points for discount
     * @return discount amount (1 point = 100đ)
     */
    public BigDecimal redeemPoints(int customerId, int orderId, int pointsToRedeem) {
        Customer customer = getById(customerId);
        if (customer == null || pointsToRedeem > customer.getLoyaltyPoints()) {
            return BigDecimal.ZERO;
        }
        
        String sql = "UPDATE customers SET loyalty_points = loyalty_points - ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, pointsToRedeem);
            stmt.setInt(2, customerId);
            
            if (stmt.executeUpdate() > 0) {
                recordLoyaltyTransaction(customerId, orderId, -pointsToRedeem, "REDEEM", 
                    "Đổi điểm cho đơn hàng #" + orderId);
                
                // 1 point = 100đ
                return BigDecimal.valueOf(pointsToRedeem * 100);
            }
        } catch (SQLException e) {
            logger.error("Error redeeming points", e);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Add bonus points (promotion, birthday, etc.)
     */
    public boolean addBonusPoints(int customerId, int points, String reason) {
        String sql = "UPDATE customers SET loyalty_points = loyalty_points + ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, points);
            stmt.setInt(2, customerId);
            
            if (stmt.executeUpdate() > 0) {
                recordLoyaltyTransaction(customerId, null, points, "BONUS", reason);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error adding bonus points", e);
        }
        return false;
    }
    
    private void recordLoyaltyTransaction(int customerId, Integer orderId, int pointsChange, 
                                          String type, String description) {
        String sql = """
            INSERT INTO loyalty_transactions 
                (customer_id, order_id, points_change, balance_after, transaction_type, description)
            VALUES (?, ?, ?, (SELECT loyalty_points FROM customers WHERE id = ?), ?, ?)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, customerId);
            if (orderId != null) {
                stmt.setInt(2, orderId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setInt(3, pointsChange);
            stmt.setInt(4, customerId);
            stmt.setString(5, type);
            stmt.setString(6, description);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error recording loyalty transaction", e);
        }
    }
    
    // ==================== STATS ====================
    
    public int getTotalCustomers() {
        String sql = "SELECT COUNT(*) FROM customers WHERE is_active = TRUE";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting customer count", e);
        }
        return 0;
    }
    
    public List<Customer> getTopCustomers(int limit) {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE is_active = TRUE ORDER BY total_spent DESC LIMIT ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapCustomer(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting top customers", e);
        }
        return customers;
    }
    
    public List<Customer> getBirthdayCustomers() {
        List<Customer> customers = new ArrayList<>();
        String sql = """
            SELECT * FROM customers 
            WHERE is_active = TRUE 
              AND MONTH(birthday) = MONTH(CURDATE())
              AND DAY(birthday) BETWEEN DAY(CURDATE()) AND DAY(CURDATE()) + 7
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                customers.add(mapCustomer(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting birthday customers", e);
        }
        return customers;
    }
    
    // ==================== MAPPER ====================
    
    private Customer mapCustomer(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setFullName(rs.getString("full_name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        
        Date birthday = rs.getDate("birthday");
        if (birthday != null) {
            c.setBirthday(birthday.toLocalDate());
        }
        
        String gender = rs.getString("gender");
        if (gender != null) {
            c.setGender(Gender.valueOf(gender));
        }
        
        c.setAddress(rs.getString("address"));
        c.setTier(CustomerTier.valueOf(rs.getString("tier")));
        c.setLoyaltyPoints(rs.getInt("loyalty_points"));
        c.setTotalSpent(rs.getBigDecimal("total_spent"));
        c.setVisitCount(rs.getInt("visit_count"));
        
        Date lastVisit = rs.getDate("last_visit");
        if (lastVisit != null) {
            c.setLastVisit(lastVisit.toLocalDate());
        }
        
        c.setNotes(rs.getString("notes"));
        c.setActive(rs.getBoolean("is_active"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            c.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return c;
    }
}
