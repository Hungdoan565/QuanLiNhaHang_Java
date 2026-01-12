package com.restaurant.model;

import java.time.LocalDateTime;

/**
 * User entity - Staff accounts
 */
public class User {
    
    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String phone;
    private String email;
    private String avatarPath;
    private int roleId;
    private Role role; // Eager loaded
    private boolean active;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // ===========================================
    // Constructors
    // ===========================================
    public User() {
        this.active = true;
    }
    
    public User(int id, String username, String fullName, Role role) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.roleId = role != null ? role.getId() : 0;
        this.active = true;
    }
    
    // ===========================================
    // Helper Methods
    // ===========================================
    
    /**
     * Get display name (full name or username)
     */
    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        return username;
    }
    
    /**
     * Get role name
     */
    public String getRoleName() {
        return role != null ? role.getName() : "Unknown";
    }
    
    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return role != null && role.isAdmin();
    }
    
    /**
     * Check if user can perform specific action
     */
    public boolean canAccessPOS() {
        return role != null && role.canAccessPOS();
    }
    
    public boolean canAccessKitchen() {
        return role != null && role.canAccessKitchen();
    }
    
    public boolean canBill() {
        return role != null && role.canBill();
    }
    
    public boolean canManageShift() {
        return role != null && role.canManageShift();
    }
    
    public boolean canCancelOrder() {
        return role != null && role.canCancelOrder();
    }
    
    public boolean canAccessReports() {
        return role != null && role.canAccessReports();
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
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getAvatarPath() {
        return avatarPath;
    }
    
    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }
    
    public int getRoleId() {
        return roleId;
    }
    
    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
        if (role != null) {
            this.roleId = role.getId();
        }
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', fullName='%s', role='%s'}",
            id, username, fullName, getRoleName());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return id == user.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
