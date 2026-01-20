package com.restaurant.dao.interfaces;

import com.restaurant.model.Order;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Order Data Access Object Interface
 */
public interface IOrderDAO {
    
    /**
     * Tạo order mới và trả về order với ID
     */
    Order create(Order order);
    
    /**
     * Lấy order theo ID
     */
    Optional<Order> findById(int id);
    
    /**
     * Lấy order theo mã order
     */
    Optional<Order> findByOrderCode(String orderCode);
    
    /**
     * Lấy order đang mở (OPEN) của bàn
     */
    Optional<Order> findOpenOrderByTableId(int tableId);
    
    /**
     * Lấy tất cả orders trong khoảng thời gian
     */
    List<Order> findByDateRange(LocalDate from, LocalDate to);
    
    /**
     * Lấy orders đã hoàn thành trong khoảng thời gian
     */
    List<Order> findCompletedByDateRange(LocalDate from, LocalDate to);
    
    /**
     * Cập nhật order
     */
    boolean update(Order order);
    
    /**
     * Hoàn thành order (đánh dấu COMPLETED)
     */
    boolean complete(int orderId);
    
    /**
     * Hủy order
     */
    boolean cancel(int orderId, int cancelledBy, String reason);
    
    /**
     * Thêm chi tiết order (món ăn)
     */
    boolean addOrderDetail(int orderId, int productId, int quantity, 
                          java.math.BigDecimal unitPrice, String notes);
    
    /**
     * Cập nhật số lượng chi tiết order
     */
    boolean updateOrderDetailQuantity(int orderDetailId, int quantity);
    
    /**
     * Xóa chi tiết order
     */
    boolean removeOrderDetail(int orderDetailId);
    
    /**
     * Cập nhật trạng thái chi tiết order
     */
    boolean updateOrderDetailStatus(int orderDetailId, com.restaurant.model.OrderDetail.ItemStatus status);
    
    /**
     * Mark item as sent to kitchen (sets sent_to_kitchen_at timestamp)
     */
    boolean markItemSentToKitchen(int orderDetailId);
    
    /**
     * Tính tổng tiền order
     */
    java.math.BigDecimal calculateTotal(int orderId);
}
