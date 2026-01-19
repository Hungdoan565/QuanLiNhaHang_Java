package com.restaurant.dao.impl;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.dao.interfaces.IOrderDAO;
import com.restaurant.model.Order;
import com.restaurant.model.Order.OrderStatus;
import com.restaurant.model.OrderDetail;
import com.restaurant.model.OrderDetail.ItemStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Order DAO Implementation - Lưu và truy xuất đơn hàng từ database
 */
public class OrderDAOImpl implements IOrderDAO {
    
    private static final Logger logger = LogManager.getLogger(OrderDAOImpl.class);
    
    @Override
    public Order create(Order order) {
        String orderSql = """
            INSERT INTO orders (order_code, table_id, user_id, shift_id, guest_count, 
                               status, subtotal, discount_percent, discount_amount,
                               tax_percent, tax_amount, service_charge, total_amount, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, order.getOrderCode());
            stmt.setInt(2, order.getTableId());
            stmt.setInt(3, order.getUserId());
            stmt.setObject(4, order.getShiftId());
            stmt.setInt(5, order.getGuestCount());
            stmt.setString(6, order.getStatus().name());
            stmt.setBigDecimal(7, order.getSubtotal());
            stmt.setBigDecimal(8, order.getDiscountPercent());
            stmt.setBigDecimal(9, order.getDiscountAmount());
            stmt.setBigDecimal(10, order.getTaxPercent());
            stmt.setBigDecimal(11, order.getTaxAmount());
            stmt.setBigDecimal(12, order.getServiceCharge());
            stmt.setBigDecimal(13, order.getTotalAmount());
            stmt.setString(14, order.getNotes());
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    order.setId(rs.getInt(1));
                    
                    // Insert order details
                    for (OrderDetail item : order.getItems()) {
                        item.setOrderId(order.getId());
                        addOrderDetailInternal(conn, item);
                    }
                    
                    logger.info("Created order: {} for table {}", order.getOrderCode(), order.getTableId());
                    return order;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error creating order", e);
        }
        
        return null;
    }
    
    private void addOrderDetailInternal(Connection conn, OrderDetail item) throws SQLException {
        String sql = """
            INSERT INTO order_details (order_id, product_id, quantity, original_price, 
                                       unit_price, subtotal, notes, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, item.getOrderId());
            stmt.setInt(2, item.getProductId());
            stmt.setInt(3, item.getQuantity());
            stmt.setBigDecimal(4, item.getOriginalPrice());
            stmt.setBigDecimal(5, item.getUnitPrice());
            stmt.setBigDecimal(6, item.getSubtotal());
            stmt.setString(7, item.getNotes());
            stmt.setString(8, item.getStatus().name());
            
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                item.setId(rs.getInt(1));
            }
        }
    }
    
    @Override
    public Optional<Order> findById(int id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Order order = mapOrder(rs);
                loadOrderDetails(conn, order);
                return Optional.of(order);
            }
            
        } catch (SQLException e) {
            logger.error("Error finding order by id: {}", id, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<Order> findByOrderCode(String orderCode) {
        String sql = "SELECT * FROM orders WHERE order_code = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, orderCode);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Order order = mapOrder(rs);
                loadOrderDetails(conn, order);
                return Optional.of(order);
            }
            
        } catch (SQLException e) {
            logger.error("Error finding order by code: {}", orderCode, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<Order> findOpenOrderByTableId(int tableId) {
        String sql = "SELECT * FROM orders WHERE table_id = ? AND status = 'OPEN' ORDER BY created_at DESC LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, tableId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Order order = mapOrder(rs);
                loadOrderDetails(conn, order);
                logger.debug("Found open order {} for table {}", order.getOrderCode(), tableId);
                return Optional.of(order);
            }
            
        } catch (SQLException e) {
            logger.error("Error finding open order for table: {}", tableId, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<Order> findByDateRange(LocalDate from, LocalDate to) {
        return findOrdersByDateRange(from, to, null);
    }
    
    @Override
    public List<Order> findCompletedByDateRange(LocalDate from, LocalDate to) {
        return findOrdersByDateRange(from, to, "COMPLETED");
    }
    
    private List<Order> findOrdersByDateRange(LocalDate from, LocalDate to, String status) {
        List<Order> orders = new ArrayList<>();
        
        String sql = "SELECT * FROM orders WHERE DATE(created_at) BETWEEN ? AND ?";
        if (status != null) {
            sql += " AND status = ?";
        }
        sql += " ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(from));
            stmt.setDate(2, Date.valueOf(to));
            if (status != null) {
                stmt.setString(3, status);
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Order order = mapOrder(rs);
                loadOrderDetails(conn, order);
                orders.add(order);
            }
            
        } catch (SQLException e) {
            logger.error("Error finding orders by date range", e);
        }
        
        return orders;
    }
    
    @Override
    public boolean update(Order order) {
        String sql = """
            UPDATE orders SET 
                guest_count = ?, status = ?, subtotal = ?, discount_percent = ?,
                discount_amount = ?, tax_percent = ?, tax_amount = ?, service_charge = ?,
                total_amount = ?, notes = ?, updated_at = NOW()
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, order.getGuestCount());
            stmt.setString(2, order.getStatus().name());
            stmt.setBigDecimal(3, order.getSubtotal());
            stmt.setBigDecimal(4, order.getDiscountPercent());
            stmt.setBigDecimal(5, order.getDiscountAmount());
            stmt.setBigDecimal(6, order.getTaxPercent());
            stmt.setBigDecimal(7, order.getTaxAmount());
            stmt.setBigDecimal(8, order.getServiceCharge());
            stmt.setBigDecimal(9, order.getTotalAmount());
            stmt.setString(10, order.getNotes());
            stmt.setInt(11, order.getId());
            
            int affected = stmt.executeUpdate();
            return affected > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating order: {}", order.getId(), e);
        }
        
        return false;
    }
    
    @Override
    public boolean complete(int orderId) {
        String sql = "UPDATE orders SET status = 'COMPLETED', completed_at = NOW() WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                logger.info("Order {} completed", orderId);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error completing order: {}", orderId, e);
        }
        
        return false;
    }
    
    @Override
    public boolean cancel(int orderId, int cancelledBy, String reason) {
        String sql = "UPDATE orders SET status = 'CANCELLED', cancelled_by = ?, cancel_reason = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, cancelledBy);
            stmt.setString(2, reason);
            stmt.setInt(3, orderId);
            
            int affected = stmt.executeUpdate();
            return affected > 0;
            
        } catch (SQLException e) {
            logger.error("Error cancelling order: {}", orderId, e);
        }
        
        return false;
    }
    
    @Override
    public boolean addOrderDetail(int orderId, int productId, int quantity,
                                 BigDecimal unitPrice, String notes) {
        String sql = """
            INSERT INTO order_details (order_id, product_id, quantity, original_price, 
                                       unit_price, subtotal, notes, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING')
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            
            stmt.setInt(1, orderId);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            stmt.setBigDecimal(4, unitPrice);
            stmt.setBigDecimal(5, unitPrice);
            stmt.setBigDecimal(6, subtotal);
            stmt.setString(7, notes);
            
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                updateOrderTotals(conn, orderId);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error adding order detail", e);
        }
        
        return false;
    }
    
    @Override
    public boolean updateOrderDetailQuantity(int orderDetailId, int quantity) {
        String sql = "UPDATE order_details SET quantity = ?, subtotal = unit_price * ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, quantity);
            stmt.setInt(2, quantity);
            stmt.setInt(3, orderDetailId);
            
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                // Get order_id and update totals
                String getOrderIdSql = "SELECT order_id FROM order_details WHERE id = ?";
                try (PreparedStatement getStmt = conn.prepareStatement(getOrderIdSql)) {
                    getStmt.setInt(1, orderDetailId);
                    ResultSet rs = getStmt.executeQuery();
                    if (rs.next()) {
                        updateOrderTotals(conn, rs.getInt("order_id"));
                    }
                }
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error updating order detail quantity", e);
        }
        
        return false;
    }
    
    @Override
    public boolean removeOrderDetail(int orderDetailId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Get order_id first
            int orderId = 0;
            String getOrderIdSql = "SELECT order_id FROM order_details WHERE id = ?";
            try (PreparedStatement getStmt = conn.prepareStatement(getOrderIdSql)) {
                getStmt.setInt(1, orderDetailId);
                ResultSet rs = getStmt.executeQuery();
                if (rs.next()) {
                    orderId = rs.getInt("order_id");
                }
            }
            
            // Delete
            String sql = "DELETE FROM order_details WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, orderDetailId);
                int affected = stmt.executeUpdate();
                
                if (affected > 0 && orderId > 0) {
                    updateOrderTotals(conn, orderId);
                    return true;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error removing order detail", e);
        }
        
        return false;
    }
    
    @Override
    public boolean updateOrderDetailStatus(int orderDetailId, ItemStatus status) {
        String sql = """
            UPDATE order_details SET status = ?,
                sent_to_kitchen_at = CASE WHEN ? = 'COOKING' AND sent_to_kitchen_at IS NULL THEN NOW() ELSE sent_to_kitchen_at END,
                completed_at = CASE WHEN ? IN ('READY', 'SERVED') AND completed_at IS NULL THEN NOW() ELSE completed_at END
            WHERE id = ?
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            stmt.setString(2, status.name());
            stmt.setString(3, status.name());
            stmt.setInt(4, orderDetailId);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                logger.debug("Updated order detail {} status to {}", orderDetailId, status);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error updating order detail status", e);
        }
        
        return false;
    }
    
    @Override
    public BigDecimal calculateTotal(int orderId) {
        String sql = "SELECT SUM(subtotal) as total FROM order_details WHERE order_id = ? AND status != 'CANCELLED'";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
            
        } catch (SQLException e) {
            logger.error("Error calculating total", e);
        }
        
        return BigDecimal.ZERO;
    }
    
    // ========== Helper Methods ==========
    
    private void updateOrderTotals(Connection conn, int orderId) throws SQLException {
        BigDecimal subtotal = BigDecimal.ZERO;
        
        String sumSql = "SELECT SUM(subtotal) as total FROM order_details WHERE order_id = ? AND status != 'CANCELLED'";
        try (PreparedStatement stmt = conn.prepareStatement(sumSql)) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                subtotal = rs.getBigDecimal("total");
                if (subtotal == null) subtotal = BigDecimal.ZERO;
            }
        }
        
        String updateSql = "UPDATE orders SET subtotal = ?, total_amount = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setBigDecimal(1, subtotal);
            stmt.setBigDecimal(2, subtotal); // For now, total = subtotal (no tax/discount)
            stmt.setInt(3, orderId);
            stmt.executeUpdate();
        }
    }
    
    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setOrderCode(rs.getString("order_code"));
        order.setTableId(rs.getInt("table_id"));
        order.setUserId(rs.getInt("user_id"));
        order.setShiftId(rs.getObject("shift_id", Integer.class));
        order.setGuestCount(rs.getInt("guest_count"));
        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
        order.setSubtotal(rs.getBigDecimal("subtotal"));
        order.setDiscountPercent(rs.getBigDecimal("discount_percent"));
        order.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        order.setTaxPercent(rs.getBigDecimal("tax_percent"));
        order.setTaxAmount(rs.getBigDecimal("tax_amount"));
        order.setServiceCharge(rs.getBigDecimal("service_charge"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setNotes(rs.getString("notes"));
        order.setCancelReason(rs.getString("cancel_reason"));
        order.setCancelledBy(rs.getObject("cancelled_by", Integer.class));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) order.setCreatedAt(createdAt.toLocalDateTime());
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) order.setUpdatedAt(updatedAt.toLocalDateTime());
        
        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) order.setCompletedAt(completedAt.toLocalDateTime());
        
        return order;
    }
    
    private void loadOrderDetails(Connection conn, Order order) throws SQLException {
        String sql = """
            SELECT od.*, p.name as product_name 
            FROM order_details od
            LEFT JOIN products p ON od.product_id = p.id
            WHERE od.order_id = ?
            ORDER BY od.created_at
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, order.getId());
            ResultSet rs = stmt.executeQuery();
            
            List<OrderDetail> items = new ArrayList<>();
            while (rs.next()) {
                OrderDetail item = new OrderDetail();
                item.setId(rs.getInt("id"));
                item.setOrderId(rs.getInt("order_id"));
                item.setProductId(rs.getInt("product_id"));
                item.setProductName(rs.getString("product_name"));
                item.setQuantity(rs.getInt("quantity"));
                item.setOriginalPrice(rs.getBigDecimal("original_price"));
                item.setUnitPrice(rs.getBigDecimal("unit_price"));
                item.setSubtotal(rs.getBigDecimal("subtotal"));
                item.setNotes(rs.getString("notes"));
                item.setStatus(ItemStatus.valueOf(rs.getString("status")));
                
                Timestamp sentAt = rs.getTimestamp("sent_to_kitchen_at");
                if (sentAt != null) item.setSentToKitchenAt(sentAt.toLocalDateTime());
                
                Timestamp compAt = rs.getTimestamp("completed_at");
                if (compAt != null) item.setCompletedAt(compAt.toLocalDateTime());
                
                items.add(item);
            }
            
            order.setItems(items);
        }
    }
}
