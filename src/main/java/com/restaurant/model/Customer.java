package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer - Khách hàng với loyalty points
 */
public class Customer {
    
    public enum CustomerTier {
        REGULAR("Đồng", 1.0, 0),      // Hệ số x1.0
        SILVER("Bạc", 1.1, 3),        // Hệ số x1.1, giảm 3%
        GOLD("Vàng", 1.2, 5),          // Hệ số x1.2, giảm 5%
        VIP("Kim Cương", 1.5, 10);     // Hệ số x1.5, giảm 10%
        
        private final String displayName;
        private final double pointsMultiplier;  // Hệ số nhân điểm
        private final int discountPercent; // % giảm giá
        
        CustomerTier(String displayName, double pointsMultiplier, int discountPercent) {
            this.displayName = displayName;
            this.pointsMultiplier = pointsMultiplier;
            this.discountPercent = discountPercent;
        }
        
        public String getDisplayName() { return displayName; }
        public double getPointsMultiplier() { return pointsMultiplier; }
        public int getDiscountPercent() { return discountPercent; }
    }
    
    public enum Gender {
        MALE("Nam"),
        FEMALE("Nữ"),
        OTHER("Khác");
        
        private final String displayName;
        Gender(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    
    private int id;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate birthday;
    private Gender gender;
    private String address;
    private CustomerTier tier;
    private int loyaltyPoints;
    private BigDecimal totalSpent;
    private int visitCount;
    private LocalDate lastVisit;
    private String notes;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public Customer() {
        this.tier = CustomerTier.REGULAR;
        this.loyaltyPoints = 0;
        this.totalSpent = BigDecimal.ZERO;
        this.visitCount = 0;
        this.isActive = true;
    }
    
    public Customer(String fullName, String phone) {
        this();
        this.fullName = fullName;
        this.phone = phone;
    }
    
    // Business methods
    
    /**
     * Tính điểm thưởng từ số tiền
     * Công thức: (Tổng đơn / 10.000) * Hệ số Tier
     * VD: Đơn 1.3 triệu, tier Gold (x1.2) = 130 * 1.2 = 156 điểm
     */
    public int calculatePointsFromAmount(BigDecimal amount) {
        // Base points = amount / 10,000 (mỗi 10k = 1 điểm cơ bản)
        int basePoints = amount.divide(BigDecimal.valueOf(10000), 0, java.math.RoundingMode.DOWN).intValue();
        // Nhân với hệ số tier
        return (int) Math.round(basePoints * tier.getPointsMultiplier());
    }
    
    /**
     * Tính giảm giá theo tier
     */
    public BigDecimal calculateTierDiscount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(tier.getDiscountPercent()))
                     .divide(BigDecimal.valueOf(100));
    }
    
    /**
     * Cập nhật tier dựa trên tổng chi tiêu
     */
    public void updateTier() {
        if (totalSpent.compareTo(BigDecimal.valueOf(15000000)) >= 0) {
            this.tier = CustomerTier.VIP;
        } else if (totalSpent.compareTo(BigDecimal.valueOf(5000000)) >= 0) {
            this.tier = CustomerTier.GOLD;
        } else if (totalSpent.compareTo(BigDecimal.valueOf(2000000)) >= 0) {
            this.tier = CustomerTier.SILVER;
        } else {
            this.tier = CustomerTier.REGULAR;
        }
    }
    
    /**
     * Cộng điểm
     */
    public void addPoints(int points) {
        this.loyaltyPoints += points;
    }
    
    /**
     * Trừ điểm (đổi điểm)
     */
    public boolean redeemPoints(int points) {
        if (points <= loyaltyPoints) {
            this.loyaltyPoints -= points;
            return true;
        }
        return false;
    }
    
    /**
     * Ghi nhận visit
     */
    public void recordVisit(BigDecimal orderAmount) {
        this.visitCount++;
        this.lastVisit = LocalDate.now();
        this.totalSpent = this.totalSpent.add(orderAmount);
        updateTier();
    }
    
    /**
     * Check sinh nhật trong tuần
     */
    public boolean isBirthdayThisWeek() {
        if (birthday == null) return false;
        LocalDate today = LocalDate.now();
        LocalDate birthdayThisYear = birthday.withYear(today.getYear());
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, birthdayThisYear);
        return daysDiff >= 0 && daysDiff <= 7;
    }
    
    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
    
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public CustomerTier getTier() { return tier; }
    public void setTier(CustomerTier tier) { this.tier = tier; }
    
    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }
    
    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
    
    public int getVisitCount() { return visitCount; }
    public void setVisitCount(int visitCount) { this.visitCount = visitCount; }
    
    public LocalDate getLastVisit() { return lastVisit; }
    public void setLastVisit(LocalDate lastVisit) { this.lastVisit = lastVisit; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public String toString() {
        return fullName + " (" + phone + ") - " + tier.getDisplayName();
    }
}
