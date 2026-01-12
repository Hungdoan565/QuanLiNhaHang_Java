package com.restaurant.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utility to generate BCrypt password hash
 * Run this to get the correct hash for seed.sql
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        String password = "123456";
        
        // Generate hash with cost factor 12
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        
        System.out.println("=".repeat(60));
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("=".repeat(60));
        
        // Verify the hash works
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
        System.out.println("Verification: " + (result.verified ? "SUCCESS ✓" : "FAILED ✗"));
        
        // SQL update statement
        System.out.println("\n-- Run this SQL to update passwords:");
        System.out.println("UPDATE users SET password_hash = '" + hash + "';");
    }
}
