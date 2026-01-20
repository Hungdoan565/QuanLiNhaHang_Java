package com.restaurant.service;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.Customer;
import com.restaurant.model.Promotion;
import com.restaurant.model.Promotion.PromotionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Promotion Service - Quản lý khuyến mãi
 */
public class PromotionService {
    
    private static final Logger logger = LogManager.getLogger(PromotionService.class);
    private static PromotionService instance;
    
    private PromotionService() {}
    
    public static synchronized PromotionService getInstance() {
        if (instance == null) {
            instance = new PromotionService();
        }
        return instance;
    }
    
    // ==================== CRUD ====================
    
    public List<Promotion> getAllPromotions() {
        List<Promotion> promotions = new ArrayList<>();
        String sql = "SELECT * FROM promotions ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                promotions.add(mapPromotion(rs));
            }
        } catch (SQLException e) {
            logger.error("Error loading promotions", e);
        }
        return promotions;
    }
    
    public List<Promotion> getActivePromotions() {
        List<Promotion> promotions = new ArrayList<>();
        String sql = """
            SELECT * FROM promotions 
            WHERE is_active = TRUE 
              AND NOW() BETWEEN start_date AND end_date
              AND (usage_limit IS NULL OR used_count < usage_limit)
            ORDER BY type, name
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                promotions.add(mapPromotion(rs));
            }
        } catch (SQLException e) {
            logger.error("Error loading active promotions", e);
        }
        return promotions;
    }
    
    public Promotion getById(int id) {
        String sql = "SELECT * FROM promotions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapPromotion(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting promotion by id", e);
        }
        return null;
    }
    
    public Promotion getByCode(String code) {
        String sql = "SELECT * FROM promotions WHERE code = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, code.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapPromotion(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting promotion by code", e);
        }
        return null;
    }
    
    public boolean createPromotion(Promotion promo) {
        String sql = """
            INSERT INTO promotions (code, name, description, type, value, min_order_value, max_discount,
                start_date, end_date, applicable_days, applicable_hours, usage_limit, min_customer_tier, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, promo.getCode());
            stmt.setString(2, promo.getName());
            stmt.setString(3, promo.getDescription());
            stmt.setString(4, promo.getType().name());
            stmt.setBigDecimal(5, promo.getValue());
            stmt.setBigDecimal(6, promo.getMinOrderValue());
            stmt.setBigDecimal(7, promo.getMaxDiscount());
            stmt.setTimestamp(8, Timestamp.valueOf(promo.getStartDate()));
            stmt.setTimestamp(9, Timestamp.valueOf(promo.getEndDate()));
            stmt.setString(10, promo.getApplicableDays());
            stmt.setString(11, promo.getApplicableHours());
            
            if (promo.getUsageLimit() != null) {
                stmt.setInt(12, promo.getUsageLimit());
            } else {
                stmt.setNull(12, Types.INTEGER);
            }
            
            if (promo.getMinCustomerTier() != null) {
                stmt.setString(13, promo.getMinCustomerTier().name());
            } else {
                stmt.setNull(13, Types.VARCHAR);
            }
            
            if (promo.getCreatedBy() != null) {
                stmt.setInt(14, promo.getCreatedBy());
            } else {
                stmt.setNull(14, Types.INTEGER);
            }
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        promo.setId(keys.getInt(1));
                    }
                }
                logger.info("Created promotion: {} ({})", promo.getName(), promo.getCode());
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error creating promotion", e);
        }
        return false;
    }
    
    public boolean updatePromotion(Promotion promo) {
        String sql = """
            UPDATE promotions SET 
                code = ?, name = ?, description = ?, type = ?, value = ?,
                min_order_value = ?, max_discount = ?, start_date = ?, end_date = ?,
                applicable_days = ?, applicable_hours = ?, usage_limit = ?, 
                min_customer_tier = ?, is_active = ?
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, promo.getCode());
            stmt.setString(2, promo.getName());
            stmt.setString(3, promo.getDescription());
            stmt.setString(4, promo.getType().name());
            stmt.setBigDecimal(5, promo.getValue());
            stmt.setBigDecimal(6, promo.getMinOrderValue());
            stmt.setBigDecimal(7, promo.getMaxDiscount());
            stmt.setTimestamp(8, Timestamp.valueOf(promo.getStartDate()));
            stmt.setTimestamp(9, Timestamp.valueOf(promo.getEndDate()));
            stmt.setString(10, promo.getApplicableDays());
            stmt.setString(11, promo.getApplicableHours());
            
            if (promo.getUsageLimit() != null) {
                stmt.setInt(12, promo.getUsageLimit());
            } else {
                stmt.setNull(12, Types.INTEGER);
            }
            
            if (promo.getMinCustomerTier() != null) {
                stmt.setString(13, promo.getMinCustomerTier().name());
            } else {
                stmt.setNull(13, Types.VARCHAR);
            }
            
            stmt.setBoolean(14, promo.isActive());
            stmt.setInt(15, promo.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating promotion", e);
            return false;
        }
    }
    
    public boolean deletePromotion(int id) {
        String sql = "DELETE FROM promotions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting promotion", e);
            return false;
        }
    }
    
    // ==================== VALIDATION & APPLICATION ====================
    
    /**
     * Validate a promotion code for an order
     */
    public Promotion validateCode(String code, BigDecimal orderAmount, Customer customer) {
        Promotion promo = getByCode(code);
        if (promo == null) {
            return null;
        }
        
        if (!promo.isValid()) {
            return null;
        }
        
        if (!promo.meetsMinimumOrder(orderAmount)) {
            return null;
        }
        
        if (customer != null && !promo.isApplicableToTier(customer.getTier())) {
            return null;
        }
        
        return promo;
    }
    
    /**
     * Get auto-apply promotions for an order
     */
    public List<Promotion> getAutoApplyPromotions(BigDecimal orderAmount, Customer customer) {
        List<Promotion> result = new ArrayList<>();
        
        for (Promotion promo : getActivePromotions()) {
            if (!promo.isAutoApply()) continue;
            if (!promo.isValid()) continue;
            if (!promo.meetsMinimumOrder(orderAmount)) continue;
            if (customer != null && !promo.isApplicableToTier(customer.getTier())) continue;
            
            result.add(promo);
        }
        
        return result;
    }
    
    /**
     * Apply promotion to an order
     */
    public boolean applyPromotion(int promotionId, int orderId, Integer customerId, BigDecimal discountAmount) {
        String usageSql = """
            INSERT INTO promotion_usage (promotion_id, order_id, customer_id, discount_amount)
            VALUES (?, ?, ?, ?)
            """;
        
        String updateCountSql = "UPDATE promotions SET used_count = used_count + 1 WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            
            // Record usage
            try (PreparedStatement stmt = conn.prepareStatement(usageSql)) {
                stmt.setInt(1, promotionId);
                stmt.setInt(2, orderId);
                if (customerId != null) {
                    stmt.setInt(3, customerId);
                } else {
                    stmt.setNull(3, Types.INTEGER);
                }
                stmt.setBigDecimal(4, discountAmount);
                stmt.executeUpdate();
            }
            
            // Update count
            try (PreparedStatement stmt = conn.prepareStatement(updateCountSql)) {
                stmt.setInt(1, promotionId);
                stmt.executeUpdate();
            }
            
            conn.commit();
            return true;
        } catch (SQLException e) {
            logger.error("Error applying promotion", e);
            return false;
        }
    }
    
    // ==================== MAPPER ====================
    
    private Promotion mapPromotion(ResultSet rs) throws SQLException {
        Promotion p = new Promotion();
        p.setId(rs.getInt("id"));
        p.setCode(rs.getString("code"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setType(PromotionType.valueOf(rs.getString("type")));
        p.setValue(rs.getBigDecimal("value"));
        p.setMinOrderValue(rs.getBigDecimal("min_order_value"));
        p.setMaxDiscount(rs.getBigDecimal("max_discount"));
        
        Timestamp startTs = rs.getTimestamp("start_date");
        if (startTs != null) {
            p.setStartDate(startTs.toLocalDateTime());
        }
        
        Timestamp endTs = rs.getTimestamp("end_date");
        if (endTs != null) {
            p.setEndDate(endTs.toLocalDateTime());
        }
        
        p.setApplicableDays(rs.getString("applicable_days"));
        p.setApplicableHours(rs.getString("applicable_hours"));
        p.setApplicableCategories(rs.getString("applicable_categories"));
        p.setApplicableProducts(rs.getString("applicable_products"));
        
        p.setUsageLimit((Integer) rs.getObject("usage_limit"));
        p.setUsageLimitPerCustomer((Integer) rs.getObject("usage_limit_per_customer"));
        p.setUsedCount(rs.getInt("used_count"));
        
        String tier = rs.getString("min_customer_tier");
        if (tier != null) {
            p.setMinCustomerTier(Customer.CustomerTier.valueOf(tier));
        }
        
        p.setActive(rs.getBoolean("is_active"));
        p.setCreatedBy((Integer) rs.getObject("created_by"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            p.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return p;
    }
}
