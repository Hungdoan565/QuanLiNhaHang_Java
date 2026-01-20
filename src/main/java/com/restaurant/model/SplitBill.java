package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Split Bill Model
 * Represents a split payment session for an order
 */
public class SplitBill {
    
    public enum SplitType {
        EQUAL,      // Chia đều (default)
        BY_ITEM,    // Chia theo món
        CUSTOM      // Tùy chỉnh số tiền
    }
    
    public enum Status {
        PENDING,    // Chưa thanh toán
        PARTIAL,    // Thanh toán một phần
        COMPLETED   // Hoàn tất
    }
    
    private int id;
    private int orderId;
    private SplitType splitType = SplitType.EQUAL;
    private int totalSplits = 2;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private Status status = Status.PENDING;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    
    // Child entities
    private List<SplitBillPart> parts = new ArrayList<>();
    
    // Transient fields for display
    private String orderCode;
    private String tableName;
    
    public SplitBill() {}
    
    public SplitBill(int orderId, SplitType splitType, int totalSplits, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.splitType = splitType;
        this.totalSplits = totalSplits;
        this.totalAmount = totalAmount;
    }
    
    // ==================== BUSINESS METHODS ====================
    
    /**
     * Create EQUAL split parts
     */
    public void createEqualParts(int numberOfParts) {
        this.splitType = SplitType.EQUAL;
        this.totalSplits = numberOfParts;
        this.parts.clear();
        
        BigDecimal amountPerPart = totalAmount.divide(
            new BigDecimal(numberOfParts), 0, java.math.RoundingMode.CEILING);
        
        for (int i = 1; i <= numberOfParts; i++) {
            SplitBillPart part = new SplitBillPart();
            part.setSplitBillId(this.id);
            part.setPartNumber(i);
            part.setAmount(amountPerPart);
            parts.add(part);
        }
        
        // Adjust last part to match exact total
        if (!parts.isEmpty()) {
            BigDecimal sumSoFar = parts.stream()
                .map(SplitBillPart::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal diff = sumSoFar.subtract(totalAmount);
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                SplitBillPart lastPart = parts.get(parts.size() - 1);
                lastPart.setAmount(lastPart.getAmount().subtract(diff));
            }
        }
    }
    
    /**
     * Check if all parts are paid
     */
    public boolean isFullyPaid() {
        return parts.stream().allMatch(SplitBillPart::isPaid);
    }
    
    /**
     * Get amount paid so far
     */
    public BigDecimal getPaidAmount() {
        return parts.stream()
            .filter(SplitBillPart::isPaid)
            .map(SplitBillPart::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Get remaining amount
     */
    public BigDecimal getRemainingAmount() {
        return totalAmount.subtract(getPaidAmount());
    }
    
    /**
     * Get paid parts count
     */
    public int getPaidPartsCount() {
        return (int) parts.stream().filter(SplitBillPart::isPaid).count();
    }
    
    // ==================== GETTERS & SETTERS ====================
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    
    public SplitType getSplitType() { return splitType; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }
    
    public int getTotalSplits() { return totalSplits; }
    public void setTotalSplits(int totalSplits) { this.totalSplits = totalSplits; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public List<SplitBillPart> getParts() { return parts; }
    public void setParts(List<SplitBillPart> parts) { this.parts = parts; }
    
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
}
