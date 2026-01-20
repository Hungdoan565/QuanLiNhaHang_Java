package com.restaurant.util;

import com.restaurant.config.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Shared state manager for Kitchen Orders
 * Loads orders from database for real-time sync across multiple instances
 */
public class KitchenOrderManager {
    
    private static final Logger logger = LogManager.getLogger(KitchenOrderManager.class);
    private static KitchenOrderManager instance;
    
    private final List<KitchenOrder> pendingOrders = new CopyOnWriteArrayList<>();
    private final List<KitchenOrder> completedOrders = new CopyOnWriteArrayList<>();
    private final List<Consumer<List<KitchenOrder>>> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<KitchenOrder>> readyListeners = new CopyOnWriteArrayList<>();
    
    private KitchenOrderManager() {}
    
    public static synchronized KitchenOrderManager getInstance() {
        if (instance == null) {
            instance = new KitchenOrderManager();
        }
        return instance;
    }
    
    /**
     * Load orders from database - call this to sync with real data
     * Loads OPEN orders with items that are PENDING, COOKING, or READY
     */
    public void loadFromDatabase() {
        // Use IFNULL for current_step in case column doesn't exist or is null
        String sql = """
            SELECT 
                o.id AS order_id, o.order_code, t.name AS table_name, o.created_at,
                od.id AS item_id, od.product_id, p.name AS product_name, 
                od.quantity, od.status AS item_status
            FROM orders o
            JOIN tables t ON o.table_id = t.id
            JOIN order_details od ON o.id = od.order_id
            JOIN products p ON od.product_id = p.id
            WHERE o.status = 'OPEN' 
              AND od.status IN ('PENDING', 'COOKING', 'READY')
            ORDER BY o.created_at, od.id
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            Map<Integer, KitchenOrder> orderMap = new HashMap<>();
            
            while (rs.next()) {
                int orderId = rs.getInt("order_id");
                
                // Create or get order
                KitchenOrder order = orderMap.get(orderId);
                if (order == null) {
                    String orderCode = rs.getString("order_code");
                    String tableName = rs.getString("table_name");
                    LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                    order = new KitchenOrder(orderId, orderCode, tableName, createdAt);
                    orderMap.put(orderId, order);
                }
                
                // Add item
                int itemId = rs.getInt("item_id");
                String itemName = rs.getString("product_name");
                int quantity = rs.getInt("quantity");
                int productId = rs.getInt("product_id");
                String itemStatus = rs.getString("item_status");
                
                OrderItem item = new OrderItem(itemId, itemName, quantity, productId);
                item.setReady("READY".equals(itemStatus));
                
                // Set current step based on status
                if ("COOKING".equals(itemStatus)) {
                    item.setCurrentStep(1);
                } else if ("READY".equals(itemStatus)) {
                    item.setCurrentStep(3); // Completed
                }
                
                order.getItems().add(item);
                
                // Set order status based on items
                if ("COOKING".equals(itemStatus)) {
                    order.setStatus(OrderStatus.PREPARING);
                } else if ("READY".equals(itemStatus) && order.getStatus() != OrderStatus.PREPARING) {
                    if (order.getItems().stream().allMatch(OrderItem::isReady)) {
                        order.setStatus(OrderStatus.READY);
                    }
                }
            }
            
            // Update pending orders
            pendingOrders.clear();
            pendingOrders.addAll(orderMap.values());
            
            logger.info("Kitchen: Loaded {} orders from database", orderMap.size());
            notifyListeners();
            
        } catch (SQLException e) {
            logger.error("Error loading orders from database: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Add new order from POS
     */
    public void addOrder(KitchenOrder order) {
        pendingOrders.add(order);
        notifyListeners();
    }
    
    /**
     * Get all pending orders
     */
    public List<KitchenOrder> getPendingOrders() {
        return new ArrayList<>(pendingOrders);
    }
    
    /**
     * Get completed orders ready for serving
     */
    public List<KitchenOrder> getCompletedOrders() {
        return new ArrayList<>(completedOrders);
    }
    
    /**
     * Check if a table has order ready for serving
     */
    public boolean hasReadyOrderForTable(String tableName) {
        return completedOrders.stream()
            .anyMatch(o -> o.getTableName().equals(tableName));
    }
    
    /**
     * Get ready order for table
     */
    public KitchenOrder getReadyOrderForTable(String tableName) {
        return completedOrders.stream()
            .filter(o -> o.getTableName().equals(tableName))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Mark order as completed and ready for serving
     */
    public void completeOrder(int orderId) {
        for (KitchenOrder order : pendingOrders) {
            if (order.getId() == orderId) {
                order.setStatus(OrderStatus.READY);
                completedOrders.add(order);
                pendingOrders.remove(order);
                
                // Notify ready listeners (POS)
                notifyReadyListeners(order);
                break;
            }
        }
        notifyListeners();
    }
    
    /**
     * Mark order as served (remove from completed)
     */
    public void markServed(String tableName) {
        completedOrders.removeIf(o -> o.getTableName().equals(tableName));
        notifyListeners();
    }
    
    /**
     * Mark order item as ready
     */
    public void markItemReady(int orderId, String itemName) {
        for (KitchenOrder order : pendingOrders) {
            if (order.getId() == orderId) {
                order.markItemReady(itemName);
                notifyListeners();
                break;
            }
        }
    }
    
    /**
     * Register listener for order updates (used by KitchenPanel)
     */
    public void addListener(Consumer<List<KitchenOrder>> listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove listener
     */
    public void removeListener(Consumer<List<KitchenOrder>> listener) {
        listeners.remove(listener);
    }
    
    /**
     * Register listener for order ready notifications (used by POS)
     */
    public void addReadyListener(Consumer<KitchenOrder> listener) {
        readyListeners.add(listener);
    }
    
    /**
     * Remove ready listener
     */
    public void removeReadyListener(Consumer<KitchenOrder> listener) {
        readyListeners.remove(listener);
    }
    
    private void notifyListeners() {
        List<KitchenOrder> snapshot = new ArrayList<>(pendingOrders);
        for (Consumer<List<KitchenOrder>> listener : listeners) {
            listener.accept(snapshot);
        }
    }
    
    private void notifyReadyListeners(KitchenOrder order) {
        for (Consumer<KitchenOrder> listener : readyListeners) {
            listener.accept(order);
        }
    }
    
    /**
     * Clear all orders (for testing)
     */
    public void clear() {
        pendingOrders.clear();
        completedOrders.clear();
        notifyListeners();
    }
    
    // ============ KitchenOrder class ============
    public static class KitchenOrder {
        private static int idCounter = 1;
        
        private final int id;
        private final String orderCode;
        private final String tableName;
        private final LocalDateTime createdAt;
        private final List<OrderItem> items;
        private OrderStatus status;
        
        // Constructor for POS (new order)
        public KitchenOrder(String orderCode, String tableName, List<OrderItem> items) {
            this.id = idCounter++;
            this.orderCode = orderCode;
            this.tableName = tableName;
            this.createdAt = LocalDateTime.now();
            this.items = new ArrayList<>(items);
            this.status = OrderStatus.WAITING;
        }
        
        // Constructor for database loading
        public KitchenOrder(int id, String orderCode, String tableName, LocalDateTime createdAt) {
            this.id = id;
            this.orderCode = orderCode;
            this.tableName = tableName;
            this.createdAt = createdAt;
            this.items = new ArrayList<>();
            this.status = OrderStatus.WAITING;
        }
        
        public int getId() { return id; }
        public String getOrderCode() { return orderCode; }
        public String getTableName() { return tableName; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public List<OrderItem> getItems() { return items; }
        public OrderStatus getStatus() { return status; }
        public void setStatus(OrderStatus status) { this.status = status; }
        
        public void markItemReady(String itemName) {
            for (OrderItem item : items) {
                if (item.getName().equals(itemName)) {
                    item.setReady(true);
                    break;
                }
            }
            // Check if all items are ready
            if (items.stream().allMatch(OrderItem::isReady)) {
                this.status = OrderStatus.READY;
            } else {
                this.status = OrderStatus.PREPARING;
            }
        }
        
        /**
         * Get time elapsed since order created
         */
        public long getMinutesElapsed() {
            return java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        }
        
        /**
         * Get formatted item list for display
         */
        public List<String> getItemStrings() {
            List<String> result = new ArrayList<>();
            for (OrderItem item : items) {
                String prefix = item.isReady() ? "✓ " : "";
                result.add(prefix + item.getQuantity() + "x " + item.getName());
            }
            return result;
        }
    }
    
    public static class OrderItem {
        private int orderDetailId; // For DB updates
        private final String name;
        private final int quantity;
        private final int productId; // For recipe lookup
        private boolean ready;
        private int currentStep; // Current cooking step (0 = not started)
        
        public OrderItem(String name, int quantity) {
            this(0, name, quantity, 0); // Default orderDetailId = 0, productId = 0
        }
        
        public OrderItem(String name, int quantity, int productId) {
            this(0, name, quantity, productId);
        }
        
        public OrderItem(int orderDetailId, String name, int quantity, int productId) {
            this.orderDetailId = orderDetailId;
            this.name = name;
            this.quantity = quantity;
            this.productId = productId;
            this.ready = false;
            this.currentStep = 0;
        }
        
        public int getOrderDetailId() { return orderDetailId; }
        public void setOrderDetailId(int orderDetailId) { this.orderDetailId = orderDetailId; }
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public int getProductId() { return productId; }
        public boolean isReady() { return ready; }
        public void setReady(boolean ready) { this.ready = ready; }
        public int getCurrentStep() { return currentStep; }
        public void setCurrentStep(int currentStep) { this.currentStep = currentStep; }
        
        /**
         * Advance to next cooking step
         */
        public void advanceStep() {
            this.currentStep++;
        }
    }
    
    public enum OrderStatus {
        WAITING("Chờ xử lý"),
        PREPARING("Đang làm"),
        READY("Sẵn sàng");
        
        private final String displayName;
        OrderStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}
