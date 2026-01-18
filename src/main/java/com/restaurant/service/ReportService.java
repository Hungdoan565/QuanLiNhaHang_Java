package com.restaurant.service;

import com.restaurant.config.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Report Service - Thống kê và báo cáo doanh thu
 */
public class ReportService {
    
    private static final Logger logger = LogManager.getLogger(ReportService.class);
    private static ReportService instance;
    
    private ReportService() {}
    
    public static synchronized ReportService getInstance() {
        if (instance == null) {
            instance = new ReportService();
        }
        return instance;
    }
    
    /**
     * Lấy doanh thu theo ngày trong khoảng thời gian
     */
    public List<DailyRevenue> getDailyRevenue(LocalDate fromDate, LocalDate toDate) {
        List<DailyRevenue> result = new ArrayList<>();
        
        String sql = """
            SELECT 
                DATE(o.completed_at) as order_date,
                COUNT(DISTINCT o.id) as order_count,
                COALESCE(SUM(o.subtotal), 0) as gross_revenue,
                COALESCE(SUM(o.discount_amount), 0) as total_discount,
                COALESCE(SUM(o.total_amount), 0) as net_revenue
            FROM orders o
            WHERE o.status = 'COMPLETED'
              AND DATE(o.completed_at) BETWEEN ? AND ?
            GROUP BY DATE(o.completed_at)
            ORDER BY order_date DESC
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(fromDate));
            stmt.setDate(2, Date.valueOf(toDate));
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(new DailyRevenue(
                    rs.getDate("order_date").toLocalDate(),
                    rs.getInt("order_count"),
                    rs.getBigDecimal("gross_revenue"),
                    rs.getBigDecimal("total_discount"),
                    rs.getBigDecimal("net_revenue")
                ));
            }
            
            logger.info("Loaded {} days of revenue data from {} to {}", result.size(), fromDate, toDate);
            
        } catch (SQLException e) {
            logger.error("Error loading daily revenue", e);
        }
        
        return result;
    }
    
    /**
     * Lấy top món bán chạy trong khoảng thời gian
     */
    public List<TopProduct> getTopProducts(LocalDate fromDate, LocalDate toDate, int limit) {
        List<TopProduct> result = new ArrayList<>();
        
        String sql = """
            SELECT 
                p.id,
                p.name,
                c.icon as category_icon,
                SUM(od.quantity) as total_quantity,
                SUM(od.subtotal) as total_revenue
            FROM order_details od
            JOIN products p ON od.product_id = p.id
            LEFT JOIN categories c ON p.category_id = c.id
            JOIN orders o ON od.order_id = o.id
            WHERE o.status = 'COMPLETED'
              AND DATE(o.completed_at) BETWEEN ? AND ?
              AND od.status != 'CANCELLED'
            GROUP BY p.id, p.name, c.icon
            ORDER BY total_quantity DESC
            LIMIT ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(fromDate));
            stmt.setDate(2, Date.valueOf(toDate));
            stmt.setInt(3, limit);
            
            ResultSet rs = stmt.executeQuery();
            int rank = 1;
            while (rs.next()) {
                result.add(new TopProduct(
                    rank++,
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("category_icon"),
                    rs.getInt("total_quantity"),
                    rs.getBigDecimal("total_revenue")
                ));
            }
            
            logger.info("Loaded {} top products", result.size());
            
        } catch (SQLException e) {
            logger.error("Error loading top products", e);
        }
        
        return result;
    }
    
    /**
     * Lấy thống kê tổng quan
     */
    public ReportSummary getSummary(LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT 
                COALESCE(SUM(o.total_amount), 0) as total_revenue,
                COUNT(DISTINCT o.id) as total_orders,
                SUM(o.guest_count) as total_guests,
                COUNT(DISTINCT o.table_id) as tables_used
            FROM orders o
            WHERE o.status = 'COMPLETED'
              AND DATE(o.completed_at) BETWEEN ? AND ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(fromDate));
            stmt.setDate(2, Date.valueOf(toDate));
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                BigDecimal totalRevenue = rs.getBigDecimal("total_revenue");
                int totalOrders = rs.getInt("total_orders");
                int totalGuests = rs.getInt("total_guests");
                int tablesUsed = rs.getInt("tables_used");
                
                BigDecimal avgPerOrder = totalOrders > 0 
                    ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                
                return new ReportSummary(totalRevenue, totalOrders, totalGuests, tablesUsed, avgPerOrder);
            }
            
        } catch (SQLException e) {
            logger.error("Error loading summary", e);
        }
        
        return new ReportSummary(BigDecimal.ZERO, 0, 0, 0, BigDecimal.ZERO);
    }
    
    /**
     * Lấy thống kê kỳ trước để so sánh
     */
    public ReportSummary getPreviousPeriodSummary(LocalDate fromDate, LocalDate toDate) {
        // Calculate same duration for previous period
        long days = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDate prevTo = fromDate.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(days - 1);
        
        return getSummary(prevFrom, prevTo);
    }
    
    // ========== Inner Classes ==========
    
    public record DailyRevenue(
        LocalDate date,
        int orderCount,
        BigDecimal grossRevenue,
        BigDecimal discount,
        BigDecimal netRevenue
    ) {}
    
    public record TopProduct(
        int rank,
        int productId,
        String name,
        String categoryIcon,
        int quantity,
        BigDecimal revenue
    ) {}
    
    public record ReportSummary(
        BigDecimal totalRevenue,
        int totalOrders,
        int totalGuests,
        int tablesUsed,
        BigDecimal avgPerOrder
    ) {}
}
