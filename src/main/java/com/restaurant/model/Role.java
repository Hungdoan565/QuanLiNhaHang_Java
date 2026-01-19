package com.restaurant.model;

import java.time.LocalDateTime;

/**
 * Role entity - User permission roles
 */
public class Role {
    
    private int id;
    private String name;
    private String description;
    private String permissions; // JSON string
    private LocalDateTime createdAt;
    
    // ===========================================
    // Constructors
    // ===========================================
    public Role() {}
    
    public Role(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public Role(int id, String name, String description, String permissions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.permissions = permissions;
    }
    
    // ===========================================
    // Role Constants
    // ===========================================
    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String CASHIER = "CASHIER";
    public static final String WAITER = "WAITER";
    public static final String CHEF = "CHEF";
    
    // ===========================================
    // Permission Checks - Based on Real Restaurant Workflow
    // ===========================================
    public boolean isAdmin() {
        return ADMIN.equalsIgnoreCase(this.name);
    }
    
    public boolean isManager() {
        return MANAGER.equalsIgnoreCase(this.name);
    }
    
    public boolean isCashier() {
        return CASHIER.equalsIgnoreCase(this.name);
    }
    
    public boolean isWaiter() {
        return WAITER.equalsIgnoreCase(this.name);
    }
    
    public boolean isChef() {
        return CHEF.equalsIgnoreCase(this.name);
    }
    
    // POS: Admin, Manager, Cashier, Waiter
    public boolean canAccessPOS() {
        return isAdmin() || isManager() || isCashier() || isWaiter();
    }
    
    // Kitchen: Admin, Manager, Chef
    public boolean canAccessKitchen() {
        return isAdmin() || isManager() || isChef();
    }
    
    // Dashboard: Admin, Manager, Cashier
    public boolean canAccessDashboard() {
        return isAdmin() || isManager() || isCashier();
    }
    
    // Bill/Payment: Admin, Manager, Cashier (NOT Waiter)
    public boolean canBill() {
        return isAdmin() || isManager() || isCashier();
    }
    
    // Shift Management: Admin, Manager
    public boolean canManageShift() {
        return isAdmin() || isManager();
    }
    
    // Cancel Order: Admin, Manager
    public boolean canCancelOrder() {
        return isAdmin() || isManager();
    }
    
    // Reports: Admin, Manager
    public boolean canAccessReports() {
        return isAdmin() || isManager();
    }
    
    // Menu Management: Admin, Manager
    public boolean canManageMenu() {
        return isAdmin() || isManager();
    }
    
    // Inventory: Admin, Manager
    public boolean canManageInventory() {
        return isAdmin() || isManager();
    }
    
    // Staff: Admin, Manager (limited), can view but Manager cannot delete
    public boolean canManageStaff() {
        return isAdmin() || isManager();
    }
    
    // Settings: Admin only
    public boolean canAccessSettings() {
        return isAdmin();
    }
    
    // ===========================================
    // Getters & Setters
    // ===========================================
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getPermissions() {
        return permissions;
    }
    
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Role role = (Role) obj;
        return id == role.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
