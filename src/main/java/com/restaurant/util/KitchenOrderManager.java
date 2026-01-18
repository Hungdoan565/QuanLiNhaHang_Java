package com.restaurant.util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Shared state manager for Kitchen Orders
 * Allows POS to push orders and Kitchen to listen for updates
 * 
 * TODO: Replace with proper database + WebSocket in production
 */
public class KitchenOrderManager {
    
    private static KitchenOrderManager instance;
    
    private final List<KitchenOrder> pendingOrders = new CopyOnWriteArrayList<>();
    private final List<Consumer<List<KitchenOrder>>> listeners = new CopyOnWriteArrayList<>();
    
    private KitchenOrderManager() {}
    
    public static synchronized KitchenOrderManager getInstance() {
        if (instance == null) {
            instance = new KitchenOrderManager();
        }
        return instance;
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
     * Mark order as completed (remove from list)
     */
    public void completeOrder(int orderId) {
        pendingOrders.removeIf(o -> o.getId() == orderId);
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
    
    private void notifyListeners() {
        List<KitchenOrder> snapshot = new ArrayList<>(pendingOrders);
        for (Consumer<List<KitchenOrder>> listener : listeners) {
            listener.accept(snapshot);
        }
    }
    
    /**
     * Clear all orders (for testing)
     */
    public void clear() {
        pendingOrders.clear();
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
        
        public KitchenOrder(String orderCode, String tableName, List<OrderItem> items) {
            this.id = idCounter++;
            this.orderCode = orderCode;
            this.tableName = tableName;
            this.createdAt = LocalDateTime.now();
            this.items = new ArrayList<>(items);
            this.status = OrderStatus.WAITING;
        }
        
        public int getId() { return id; }
        public String getOrderCode() { return orderCode; }
        public String getTableName() { return tableName; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public List<OrderItem> getItems() { return items; }
        public OrderStatus getStatus() { return status; }
        
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
        private final String name;
        private final int quantity;
        private boolean ready;
        
        public OrderItem(String name, int quantity) {
            this.name = name;
            this.quantity = quantity;
            this.ready = false;
        }
        
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public boolean isReady() { return ready; }
        public void setReady(boolean ready) { this.ready = ready; }
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
