package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OrderDetail Model - Chi tiết đơn hàng (món ăn)
 */
public class OrderDetail {
    
    public enum ItemStatus {
        PENDING,    // Chờ gửi bếp
        COOKING,    // Đang chế biến
        READY,      // Đã xong, chờ phục vụ
        SERVED,     // Đã phục vụ
        CANCELLED   // Đã hủy
    }
    
    private int id;
    private int orderId;
    private int productId;
    private String productName; // Cached for display
    private int quantity;
    private BigDecimal originalPrice; // Giá gốc
    private BigDecimal unitPrice;     // Giá sau modifier
    private BigDecimal subtotal;
    private String modifiers;         // JSON format
    private String notes;
    private ItemStatus status = ItemStatus.PENDING;
    
    private LocalDateTime sentToKitchenAt;
    private LocalDateTime completedAt;
    private Integer cancelledBy;
    private String cancelReason;
    private LocalDateTime createdAt;
    
    public OrderDetail() {
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Calculate subtotal based on quantity and unit price
     */
    public void calculateSubtotal() {
        if (unitPrice != null && quantity > 0) {
            subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        } else {
            subtotal = BigDecimal.ZERO;
        }
    }
    
    /**
     * Mark as sent to kitchen
     */
    public void sendToKitchen() {
        this.status = ItemStatus.COOKING;
        this.sentToKitchenAt = LocalDateTime.now();
    }
    
    /**
     * Mark as ready (kitchen done)
     */
    public void markReady() {
        this.status = ItemStatus.READY;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Mark as served
     */
    public void markServed() {
        this.status = ItemStatus.SERVED;
    }
    
    /**
     * Cancel item
     */
    public void cancel(int cancelledByUserId, String reason) {
        this.status = ItemStatus.CANCELLED;
        this.cancelledBy = cancelledByUserId;
        this.cancelReason = reason;
    }
    
    // ========== Getters and Setters ==========
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { 
        this.quantity = quantity; 
        calculateSubtotal();
    }
    
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { 
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }
    
    public BigDecimal getSubtotal() { 
        if (subtotal == null) calculateSubtotal();
        return subtotal; 
    }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public String getModifiers() { return modifiers; }
    public void setModifiers(String modifiers) { this.modifiers = modifiers; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }
    
    public LocalDateTime getSentToKitchenAt() { return sentToKitchenAt; }
    public void setSentToKitchenAt(LocalDateTime sentToKitchenAt) { this.sentToKitchenAt = sentToKitchenAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public Integer getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(Integer cancelledBy) { this.cancelledBy = cancelledBy; }
    
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
