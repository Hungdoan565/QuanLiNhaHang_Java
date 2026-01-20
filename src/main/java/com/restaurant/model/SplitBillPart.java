package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Split Bill Part Model
 * Represents one payment portion in a split bill
 */
public class SplitBillPart {
    
    private int id;
    private int splitBillId;
    private int partNumber;         // 1, 2, 3...
    private String payerName;       // Tên người trả (tùy chọn)
    private BigDecimal amount = BigDecimal.ZERO;
    private boolean paid = false;
    private String paymentMethod;   // CASH, CARD, TRANSFER, EWALLET
    private LocalDateTime paidAt;
    private String notes;
    
    public SplitBillPart() {}
    
    public SplitBillPart(int partNumber, BigDecimal amount) {
        this.partNumber = partNumber;
        this.amount = amount;
    }
    
    // ==================== BUSINESS METHODS ====================
    
    /**
     * Mark this part as paid
     */
    public void markPaid(String paymentMethod) {
        this.paid = true;
        this.paymentMethod = paymentMethod;
        this.paidAt = LocalDateTime.now();
    }
    
    /**
     * Get display label
     */
    public String getDisplayLabel() {
        if (payerName != null && !payerName.isEmpty()) {
            return payerName;
        }
        return "Phần " + partNumber;
    }
    
    /**
     * Get status display
     */
    public String getStatusDisplay() {
        if (paid) {
            return "✅ Đã thanh toán";
        }
        return "⏳ Chờ thanh toán";
    }
    
    // ==================== GETTERS & SETTERS ====================
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getSplitBillId() { return splitBillId; }
    public void setSplitBillId(int splitBillId) { this.splitBillId = splitBillId; }
    
    public int getPartNumber() { return partNumber; }
    public void setPartNumber(int partNumber) { this.partNumber = partNumber; }
    
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
