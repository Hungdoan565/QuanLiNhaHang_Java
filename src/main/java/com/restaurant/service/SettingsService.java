package com.restaurant.service;

import com.restaurant.config.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * SettingsService - Quản lý cấu hình hệ thống
 * 
 * Singleton với cache trong memory để tối ưu performance.
 * Settings được load từ database khi khởi động và cache lại.
 */
public class SettingsService {
    
    private static final Logger logger = LogManager.getLogger(SettingsService.class);
    private static SettingsService instance;
    
    // Cache settings in memory
    private final Map<String, String> cache = new HashMap<>();
    private boolean loaded = false;
    
    // Default keys
    public static final String KEY_RESTAURANT_NAME = "restaurant_name";
    public static final String KEY_RESTAURANT_ADDRESS = "restaurant_address";
    public static final String KEY_RESTAURANT_PHONE = "restaurant_phone";
    public static final String KEY_RESTAURANT_LOGO = "restaurant_logo";
    public static final String KEY_VAT_PERCENT = "vat_percent";
    public static final String KEY_VAT_ENABLED = "vat_enabled";
    public static final String KEY_SERVICE_CHARGE_PERCENT = "service_charge_percent";
    public static final String KEY_SERVICE_CHARGE_ENABLED = "service_charge_enabled";
    public static final String KEY_RECEIPT_FOOTER = "receipt_footer";
    public static final String KEY_KITCHEN_AUTO_PRINT = "kitchen_auto_print";
    public static final String KEY_PRINT_CUSTOMER_RECEIPT = "print_customer_receipt";
    public static final String KEY_PRINTER_KITCHEN = "printer_kitchen";
    public static final String KEY_PRINTER_BAR = "printer_bar";
    public static final String KEY_PRINTER_CASHIER = "printer_cashier";
    public static final String KEY_BANK_NAME = "bank_name";
    public static final String KEY_BANK_ACCOUNT_NUMBER = "bank_account_number";
    public static final String KEY_BANK_ACCOUNT_NAME = "bank_account_name";
    public static final String KEY_BANK_QR_IMAGE = "bank_qr_image";
    public static final String KEY_THEME = "display_theme";
    public static final String KEY_FONT_SIZE = "display_font_size";
    public static final String KEY_PRIMARY_COLOR = "display_primary_color";
    public static final String KEY_KITCHEN_COLUMNS = "display_kitchen_columns";
    
    private SettingsService() {
        loadAllSettings();
    }
    
    public static synchronized SettingsService getInstance() {
        if (instance == null) {
            instance = new SettingsService();
        }
        return instance;
    }
    
    /**
     * Load all settings from database into cache
     */
    public void loadAllSettings() {
        cache.clear();
        String sql = "SELECT setting_key, setting_value FROM settings";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String key = rs.getString("setting_key");
                String value = rs.getString("setting_value");
                cache.put(key, value != null ? value : "");
            }
            
            loaded = true;
            logger.info("Loaded {} settings from database", cache.size());
            
        } catch (SQLException e) {
            logger.error("Error loading settings", e);
            setDefaults();
        }
    }
    
    /**
     * Set default values if database fails
     */
    private void setDefaults() {
        cache.put(KEY_RESTAURANT_NAME, "RestaurantPOS");
        cache.put(KEY_RESTAURANT_ADDRESS, "123 Nguyễn Huệ, Quận 1, TP.HCM");
        cache.put(KEY_RESTAURANT_PHONE, "028 1234 5678");
        cache.put(KEY_VAT_PERCENT, "10");
        cache.put(KEY_SERVICE_CHARGE_PERCENT, "5");
        cache.put(KEY_RECEIPT_FOOTER, "Cảm ơn quý khách!\nHẹn gặp lại!");
        cache.put(KEY_KITCHEN_AUTO_PRINT, "true");
        cache.put(KEY_THEME, "dark");
        cache.put(KEY_FONT_SIZE, "14");
        cache.put(KEY_PRIMARY_COLOR, "#28a745");
    }
    
    /**
     * Get setting value by key
     */
    public String get(String key) {
        if (!loaded) {
            loadAllSettings();
        }
        return cache.getOrDefault(key, "");
    }
    
    /**
     * Get setting as integer
     */
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Get setting as double (for percentages)
     */
    public double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(get(key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Get setting as boolean
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value.isEmpty()) return defaultValue;
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
    
    /**
     * Set a single setting (saves to DB immediately)
     */
    public boolean set(String key, String value) {
        return set(key, value, null);
    }
    
    /**
     * Set a single setting with user ID
     */
    public boolean set(String key, String value, Integer userId) {
        String sql = """
            INSERT INTO settings (setting_key, setting_value, updated_by) 
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE setting_value = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, key);
            stmt.setString(2, value);
            if (userId != null) {
                stmt.setInt(3, userId);
                stmt.setInt(5, userId);
            } else {
                stmt.setNull(3, Types.INTEGER);
                stmt.setNull(5, Types.INTEGER);
            }
            stmt.setString(4, value);
            
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                cache.put(key, value); // Update cache
                logger.info("Setting saved: {} = {}", key, value);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error saving setting: {}", key, e);
        }
        return false;
    }
    
    /**
     * Save multiple settings at once
     */
    public boolean saveAll(Map<String, String> settings, Integer userId) {
        String sql = """
            INSERT INTO settings (setting_key, setting_value, updated_by) 
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE setting_value = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                stmt.setString(1, entry.getKey());
                stmt.setString(2, entry.getValue());
                if (userId != null) {
                    stmt.setInt(3, userId);
                    stmt.setInt(5, userId);
                } else {
                    stmt.setNull(3, Types.INTEGER);
                    stmt.setNull(5, Types.INTEGER);
                }
                stmt.setString(4, entry.getValue());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            conn.commit();
            
            // Update cache
            cache.putAll(settings);
            logger.info("Saved {} settings", settings.size());
            return true;
            
        } catch (SQLException e) {
            logger.error("Error saving settings batch", e);
        }
        return false;
    }
    
    /**
     * Reload settings from database (clear cache first)
     */
    public void reload() {
        loaded = false;
        loadAllSettings();
    }
    
    /**
     * Get all cached settings
     */
    public Map<String, String> getAll() {
        if (!loaded) {
            loadAllSettings();
        }
        return new HashMap<>(cache);
    }
}
