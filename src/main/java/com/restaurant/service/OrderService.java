package com.restaurant.service;

import com.restaurant.dao.impl.OrderDAOImpl;
import com.restaurant.dao.interfaces.IOrderDAO;
import com.restaurant.model.Order;
import com.restaurant.model.OrderDetail;
import com.restaurant.model.Product;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Order Service - Business logic for order management
 */
public class OrderService {
    
    private static final Logger logger = LogManager.getLogger(OrderService.class);
    private static OrderService instance;
    
    private final IOrderDAO orderDAO;
    
    private OrderService() {
        this.orderDAO = new OrderDAOImpl();
    }
    
    public static synchronized OrderService getInstance() {
        if (instance == null) {
            instance = new OrderService();
        }
        return instance;
    }
    
    /**
     * Tạo order mới cho bàn
     */
    public Order createOrder(int tableId, int userId) {
        Order order = new Order(tableId, userId);
        return orderDAO.create(order);
    }
    
    /**
     * Tạo hoặc lấy order đang mở của bàn
     */
    public Order getOrCreateOrderForTable(int tableId, int userId) {
        // Check for existing open order
        Optional<Order> existing = orderDAO.findOpenOrderByTableId(tableId);
        if (existing.isPresent()) {
            logger.debug("Found existing order {} for table {}", existing.get().getOrderCode(), tableId);
            return existing.get();
        }
        
        // Create new order
        Order newOrder = createOrder(tableId, userId);
        logger.info("Created new order {} for table {}", newOrder.getOrderCode(), tableId);
        return newOrder;
    }
    
    /**
     * Lấy order đang mở của bàn
     */
    public Optional<Order> getOpenOrderForTable(int tableId) {
        return orderDAO.findOpenOrderByTableId(tableId);
    }
    
    /**
     * Lấy order theo ID
     */
    public Optional<Order> getOrderById(int orderId) {
        return orderDAO.findById(orderId);
    }
    
    /**
     * Thêm món vào order
     */
    public boolean addItemToOrder(int orderId, Product product, int quantity) {
        return orderDAO.addOrderDetail(orderId, product.getId(), quantity, 
                                       product.getPrice(), null);
    }
    
    /**
     * Cập nhật số lượng món
     */
    public boolean updateItemQuantity(int orderDetailId, int quantity) {
        if (quantity <= 0) {
            return orderDAO.removeOrderDetail(orderDetailId);
        }
        return orderDAO.updateOrderDetailQuantity(orderDetailId, quantity);
    }
    
    /**
     * Xóa món khỏi order
     */
    public boolean removeItemFromOrder(int orderDetailId) {
        return orderDAO.removeOrderDetail(orderDetailId);
    }
    
    /**
     * Cập nhật thông tin order
     */
    public boolean updateOrder(Order order) {
        return orderDAO.update(order);
    }
    
    /**
     * Hoàn thành order (thanh toán)
     */
    public boolean completeOrder(int orderId) {
        boolean success = orderDAO.complete(orderId);
        if (success) {
            logger.info("Order {} completed (paid)", orderId);
        }
        return success;
    }
    
    /**
     * Cập nhật trạng thái món
     */
    public boolean updateItemStatus(int orderDetailId, OrderDetail.ItemStatus status) {
        return orderDAO.updateOrderDetailStatus(orderDetailId, status);
    }
    
    /**
     * Mark item as sent to kitchen (sets sent_to_kitchen_at timestamp)
     */
    public boolean markItemSentToKitchen(int orderDetailId) {
        return orderDAO.markItemSentToKitchen(orderDetailId);
    }
    
    /**
     * Hủy order
     */
    public boolean cancelOrder(int orderId, int userId, String reason) {
        return orderDAO.cancel(orderId, userId, reason);
    }
    
    /**
     * Lấy orders đã hoàn thành trong khoảng thời gian
     */
    public List<Order> getCompletedOrders(LocalDate from, LocalDate to) {
        return orderDAO.findCompletedByDateRange(from, to);
    }
    
    /**
     * Tính tổng doanh thu trong khoảng thời gian
     */
    public BigDecimal getTotalRevenue(LocalDate from, LocalDate to) {
        List<Order> orders = getCompletedOrders(from, to);
        return orders.stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
