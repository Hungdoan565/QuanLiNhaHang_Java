package com.restaurant.model;

import java.math.BigDecimal;

/**
 * Split Bill Item Assignment Model
 * For BY_ITEM mode: tracks which items belong to which split part
 * Supports shared items (one item split across multiple parts)
 */
public class SplitBillItemAssignment {
    
    private int id;
    private int splitBillId;
    private int orderDetailId;
    private int partNumber;
    private int shareCount = 1;         // Số người chia món này
    private BigDecimal shareAmount = BigDecimal.ZERO;  // Số tiền sau khi chia
    
    // Transient display fields
    private String itemName;
    private BigDecimal itemPrice;
    private int itemQuantity;
    
    public SplitBillItemAssignment() {}
    
    public SplitBillItemAssignment(int orderDetailId, int partNumber, int shareCount, BigDecimal shareAmount) {
        this.orderDetailId = orderDetailId;
        this.partNumber = partNumber;
        this.shareCount = shareCount;
        this.shareAmount = shareAmount;
    }
    
    // ==================== BUSINESS METHODS ====================
    
    /**
     * Check if this is a shared item
     */
    public boolean isShared() {
        return shareCount > 1;
    }
    
    /**
     * Get display text for share info
     */
    public String getShareDisplay() {
        if (shareCount > 1) {
            return "Chia " + shareCount + " người";
        }
        return "";
    }
    
    // ==================== GETTERS & SETTERS ====================
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getSplitBillId() { return splitBillId; }
    public void setSplitBillId(int splitBillId) { this.splitBillId = splitBillId; }
    
    public int getOrderDetailId() { return orderDetailId; }
    public void setOrderDetailId(int orderDetailId) { this.orderDetailId = orderDetailId; }
    
    public int getPartNumber() { return partNumber; }
    public void setPartNumber(int partNumber) { this.partNumber = partNumber; }
    
    public int getShareCount() { return shareCount; }
    public void setShareCount(int shareCount) { this.shareCount = shareCount; }
    
    public BigDecimal getShareAmount() { return shareAmount; }
    public void setShareAmount(BigDecimal shareAmount) { this.shareAmount = shareAmount; }
    
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    
    public BigDecimal getItemPrice() { return itemPrice; }
    public void setItemPrice(BigDecimal itemPrice) { this.itemPrice = itemPrice; }
    
    public int getItemQuantity() { return itemQuantity; }
    public void setItemQuantity(int itemQuantity) { this.itemQuantity = itemQuantity; }
}
