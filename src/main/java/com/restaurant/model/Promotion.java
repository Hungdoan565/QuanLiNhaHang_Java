package com.restaurant.model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Promotion - Khuyến mãi, mã giảm giá
 */
public class Promotion {
    
    public enum PromotionType {
        PERCENT("Giảm %"),
        FIXED("Giảm tiền"),
        BUY_X_GET_Y("Mua X tặng Y"),
        COMBO("Combo");
        
        private final String displayName;
        PromotionType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    
    private int id;
    private String code;              // NULL = auto-apply
    private String name;
    private String description;
    private PromotionType type;
    private BigDecimal value;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscount;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    private String applicableDays;    // "MON,TUE,WED" or "*"
    private String applicableHours;   // "11:00-14:00" or "*"
    private String applicableCategories;
    private String applicableProducts;
    
    private Integer usageLimit;
    private Integer usageLimitPerCustomer;
    private int usedCount;
    
    private Customer.CustomerTier minCustomerTier;
    
    private boolean isActive;
    private Integer createdBy;
    private LocalDateTime createdAt;
    
    // Constructors
    public Promotion() {
        this.minOrderValue = BigDecimal.ZERO;
        this.applicableDays = "*";
        this.applicableHours = "*";
        this.isActive = true;
        this.usedCount = 0;
    }
    
    // Business methods
    
    /**
     * Check if promotion is currently valid
     */
    public boolean isValid() {
        if (!isActive) return false;
        
        LocalDateTime now = LocalDateTime.now();
        
        // Check date range
        if (now.isBefore(startDate) || now.isAfter(endDate)) {
            return false;
        }
        
        // Check usage limit
        if (usageLimit != null && usedCount >= usageLimit) {
            return false;
        }
        
        // Check day of week
        if (!applicableDays.equals("*")) {
            String todayCode = now.getDayOfWeek().name().substring(0, 3);
            if (!applicableDays.toUpperCase().contains(todayCode)) {
                return false;
            }
        }
        
        // Check hours
        if (!applicableHours.equals("*")) {
            if (!isWithinTimeRange(now.toLocalTime())) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isWithinTimeRange(LocalTime now) {
        try {
            String[] parts = applicableHours.split("-");
            if (parts.length != 2) return true;
            
            LocalTime start = LocalTime.parse(parts[0].trim());
            LocalTime end = LocalTime.parse(parts[1].trim());
            
            return !now.isBefore(start) && !now.isAfter(end);
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Check if applicable to customer tier
     */
    public boolean isApplicableToTier(Customer.CustomerTier tier) {
        if (minCustomerTier == null) return true;
        return tier.ordinal() >= minCustomerTier.ordinal();
    }
    
    /**
     * Check if meets minimum order value
     */
    public boolean meetsMinimumOrder(BigDecimal orderAmount) {
        return orderAmount.compareTo(minOrderValue) >= 0;
    }
    
    /**
     * Calculate discount amount
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isValid() || !meetsMinimumOrder(orderAmount)) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount;
        
        switch (type) {
            case PERCENT -> {
                discount = orderAmount.multiply(value).divide(BigDecimal.valueOf(100));
                if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
                    discount = maxDiscount;
                }
            }
            case FIXED -> discount = value;
            default -> discount = BigDecimal.ZERO;
        }
        
        // Cannot exceed order amount
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }
        
        return discount;
    }
    
    /**
     * Check if this is auto-apply (no code needed)
     */
    public boolean isAutoApply() {
        return code == null || code.isEmpty();
    }
    
    /**
     * Get display text for applicable hours
     */
    public String getApplicableHoursDisplay() {
        if ("*".equals(applicableHours)) {
            return "Cả ngày";
        }
        return applicableHours;
    }
    
    /**
     * Get status badge text
     */
    public String getStatusText() {
        if (!isActive) return "Tạm ngưng";
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startDate)) return "Chưa bắt đầu";
        if (now.isAfter(endDate)) return "Đã hết hạn";
        if (usageLimit != null && usedCount >= usageLimit) return "Đã hết lượt";
        
        return "Đang áp dụng";
    }
    
    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public PromotionType getType() { return type; }
    public void setType(PromotionType type) { this.type = type; }
    
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    
    public BigDecimal getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(BigDecimal minOrderValue) { this.minOrderValue = minOrderValue; }
    
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(BigDecimal maxDiscount) { this.maxDiscount = maxDiscount; }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public String getApplicableDays() { return applicableDays; }
    public void setApplicableDays(String applicableDays) { this.applicableDays = applicableDays; }
    
    public String getApplicableHours() { return applicableHours; }
    public void setApplicableHours(String applicableHours) { this.applicableHours = applicableHours; }
    
    public String getApplicableCategories() { return applicableCategories; }
    public void setApplicableCategories(String applicableCategories) { this.applicableCategories = applicableCategories; }
    
    public String getApplicableProducts() { return applicableProducts; }
    public void setApplicableProducts(String applicableProducts) { this.applicableProducts = applicableProducts; }
    
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    
    public Integer getUsageLimitPerCustomer() { return usageLimitPerCustomer; }
    public void setUsageLimitPerCustomer(Integer usageLimitPerCustomer) { this.usageLimitPerCustomer = usageLimitPerCustomer; }
    
    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }
    
    public Customer.CustomerTier getMinCustomerTier() { return minCustomerTier; }
    public void setMinCustomerTier(Customer.CustomerTier minCustomerTier) { this.minCustomerTier = minCustomerTier; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return name + " (" + (code != null ? code : "Auto") + ")";
    }
}
