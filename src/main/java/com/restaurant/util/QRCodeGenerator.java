package com.restaurant.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.net.URL;

/**
 * QR Code Generator utility for VietQR payment
 */
public class QRCodeGenerator {
    
    // Bank settings - TODO: Load from SettingsService
    private static final String BANK_ID = "970436"; // VietcomBank
    private static final String ACCOUNT_NO = "1029849106";
    private static final String ACCOUNT_NAME = "DOAN VINH HUNG";
    
    /**
     * Generate VietQR code for a specific amount
     * @param amount Payment amount
     * @param description Payment description
     * @param size QR code size in pixels
     * @return ImageIcon with QR code, or null if failed
     */
    public static ImageIcon generateVietQR(BigDecimal amount, String description, int size) {
        try {
            long amountValue = amount.longValue();
            String cleanDescription = description.replaceAll("[^a-zA-Z0-9]", "");
            
            String vietQRUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                BANK_ID, ACCOUNT_NO, amountValue, cleanDescription, 
                ACCOUNT_NAME.replace(" ", "%20")
            );
            
            URL url = new URL(vietQRUrl);
            Image image = ImageIO.read(url);
            if (image != null) {
                Image scaled = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception e) {
            System.err.println("Failed to generate QR: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Generate VietQR with default size (220px)
     */
    public static ImageIcon generateVietQR(BigDecimal amount, String description) {
        return generateVietQR(amount, description, 220);
    }
    
    /**
     * Get bank account info for display
     */
    public static String getBankInfo() {
        return "STK: " + ACCOUNT_NO + " - VietcomBank";
    }
    
    /**
     * Get account number for copy
     */
    public static String getAccountNo() {
        return ACCOUNT_NO;
    }
    
    /**
     * Get account name
     */
    public static String getAccountName() {
        return ACCOUNT_NAME;
    }
}
