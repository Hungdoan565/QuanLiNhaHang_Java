package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Model - Đơn hàng/Hóa đơn
 */
public class Order {
    
    public enum OrderStatus {
        OPEN,       // Đang phục vụ
        COMPLETED,  // Đã thanh toán
        CANCELLED   // Đã hủy
    }
    
    private int id;
    private String orderCode;
    private int tableId;
    private String tableName; // For display
    private int userId;
    private String userName; // Nhân viên tạo
    private Integer shiftId;
    private int guestCount = 1;
    private OrderStatus status = OrderStatus.OPEN;
    
    // Amounts
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal discountPercent = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal taxPercent = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal serviceCharge = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    private String notes;
    private String cancelReason;
    private Integer cancelledBy;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    
    // Order details (items)
    private List<OrderDetail> items = new ArrayList<>();
    
    public Order() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Order(int tableId, int userId) {
        this();
        this.tableId = tableId;
        this.userId = userId;
        this.orderCode = generateOrderCode();
    }
    
    /**
     * Generate unique order code: ORD-YYYYMMDD-XXXX
     */
    private String generateOrderCode() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("ORD-%04d%02d%02d-%04d",
            now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
            (int)(Math.random() * 10000));
    }
    
    /**
     * Add item to order
     */
    public void addItem(Product product, int quantity) {
        // Check if product already exists
        for (OrderDetail item : items) {
            if (item.getProductId() == product.getId()) {
                item.setQuantity(item.getQuantity() + quantity);
                recalculateTotals();
                return;
            }
        }
        
        // Add new item
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(this.id);
        detail.setProductId(product.getId());
        detail.setProductName(product.getName());
        detail.setQuantity(quantity);
        detail.setOriginalPrice(product.getPrice());
        detail.setUnitPrice(product.getPrice());
        detail.calculateSubtotal();
        
        items.add(detail);
        recalculateTotals();
    }
    
    /**
     * Update item quantity
     */
    public void updateItemQuantity(int productId, int quantity) {
        for (OrderDetail item : items) {
            if (item.getProductId() == productId) {
                if (quantity <= 0) {
                    items.remove(item);
                } else {
                    item.setQuantity(quantity);
                    item.calculateSubtotal();
                }
                recalculateTotals();
                return;
            }
        }
    }
    
    /**
     * Remove item from order
     */
    public void removeItem(int productId) {
        items.removeIf(item -> item.getProductId() == productId);
        recalculateTotals();
    }
    
    /**
     * Recalculate all totals
     */
    public void recalculateTotals() {
        subtotal = BigDecimal.ZERO;
        for (OrderDetail item : items) {
            subtotal = subtotal.add(item.getSubtotal());
        }
        
        // Calculate discount
        if (discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = subtotal.multiply(discountPercent).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
        }
        
        // After discount
        BigDecimal afterDiscount = subtotal.subtract(discountAmount);
        
        // Calculate tax
        if (taxPercent.compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = afterDiscount.multiply(taxPercent).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
        }
        
        // Total
        totalAmount = afterDiscount.add(taxAmount).add(serviceCharge);
    }
    
    /**
     * Complete the order (mark as paid)
     */
    public void complete() {
        this.status = OrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Cancel the order
     */
    public void cancel(int cancelledByUserId, String reason) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledBy = cancelledByUserId;
        this.cancelReason = reason;
    }
    
    // ========== Getters and Setters ==========
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    
    public int getTableId() { return tableId; }
    public void setTableId(int tableId) { this.tableId = tableId; }
    
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public Integer getShiftId() { return shiftId; }
    public void setShiftId(Integer shiftId) { this.shiftId = shiftId; }
    
    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    
    public BigDecimal getTaxPercent() { return taxPercent; }
    public void setTaxPercent(BigDecimal taxPercent) { this.taxPercent = taxPercent; }
    
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    
    public BigDecimal getServiceCharge() { return serviceCharge; }
    public void setServiceCharge(BigDecimal serviceCharge) { this.serviceCharge = serviceCharge; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    
    public Integer getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(Integer cancelledBy) { this.cancelledBy = cancelledBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public List<OrderDetail> getItems() { return items; }
    public void setItems(List<OrderDetail> items) { this.items = items; }
    
    public boolean isEmpty() { return items.isEmpty(); }
    
    public int getItemCount() {
        return items.stream().mapToInt(OrderDetail::getQuantity).sum();
    }
}
