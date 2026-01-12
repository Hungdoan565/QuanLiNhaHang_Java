package com.restaurant.config;

/**
 * Application-wide constants and configuration
 */
public final class AppConfig {
    
    private AppConfig() {
        // Prevent instantiation
    }
    
    // ===========================================
    // Application Info
    // ===========================================
    public static final String APP_NAME = "RestaurantPOS";
    public static final String APP_VERSION = "1.0.0";
    public static final String APP_TITLE = APP_NAME + " v" + APP_VERSION;
    
    // ===========================================
    // Session Settings
    // ===========================================
    public static final int SESSION_TIMEOUT_HOURS = 8; // 1 ca làm việc
    public static final int AUTO_SAVE_INTERVAL_SECONDS = 30;
    
    // ===========================================
    // UI Settings
    // ===========================================
    public static final int DEFAULT_WINDOW_WIDTH = 1280;
    public static final int DEFAULT_WINDOW_HEIGHT = 800;
    public static final int MIN_WINDOW_WIDTH = 1024;
    public static final int MIN_WINDOW_HEIGHT = 768;
    
    // ===========================================
    // Colors (Hex)
    // ===========================================
    public static final class Colors {
        // Primary
        public static final String PRIMARY = "#E85A4F";
        public static final String PRIMARY_DARK = "#C44536";
        public static final String PRIMARY_LIGHT = "#FFE5E2";
        
        // Neutral
        public static final String BACKGROUND = "#F7F7F7";
        public static final String SURFACE = "#FFFFFF";
        public static final String TEXT_PRIMARY = "#2D3436";
        public static final String TEXT_SECONDARY = "#636E72";
        public static final String BORDER = "#DFE6E9";
        
        // Table Status
        public static final String TABLE_AVAILABLE = "#00B894";
        public static final String TABLE_OCCUPIED = "#E74C3C";
        public static final String TABLE_RESERVED = "#FDCB6E";
        public static final String TABLE_CLEANING = "#F39C12";
        
        // Kitchen Timer
        public static final String TIMER_GREEN = "#00B894";
        public static final String TIMER_YELLOW = "#FDCB6E";
        public static final String TIMER_RED = "#E74C3C";
        
        // Status
        public static final String SUCCESS = "#00B894";
        public static final String WARNING = "#FDCB6E";
        public static final String ERROR = "#E74C3C";
        public static final String INFO = "#0984E3";
        
        private Colors() {}
    }
    
    // ===========================================
    // Fonts
    // ===========================================
    public static final String FONT_FAMILY = "Inter";
    public static final int FONT_SIZE_H1 = 28;
    public static final int FONT_SIZE_H2 = 22;
    public static final int FONT_SIZE_H3 = 18;
    public static final int FONT_SIZE_BODY = 14;
    public static final int FONT_SIZE_CAPTION = 12;
    public static final int FONT_SIZE_PRICE = 16;
    
    // ===========================================
    // UI Component Sizes
    // ===========================================
    public static final int BUTTON_HEIGHT = 44;
    public static final int INPUT_HEIGHT = 44;
    public static final int TOUCH_TARGET_MIN = 44;
    public static final int BORDER_RADIUS = 8;
    public static final int CARD_RADIUS = 12;
    
    // ===========================================
    // Kitchen Display Settings
    // ===========================================
    public static final int KDS_REFRESH_INTERVAL_MS = 5000; // 5 seconds
    public static final int KDS_WARNING_MINUTES = 10;
    public static final int KDS_ALERT_MINUTES = 20;
    
    // ===========================================
    // Order Settings
    // ===========================================
    public static final String ORDER_CODE_PREFIX = "ORD";
    public static final int ORDER_CODE_LENGTH = 8;
    
    // ===========================================
    // Tax Settings
    // ===========================================
    public static final double DEFAULT_VAT_PERCENT = 10.0;
    public static final double DEFAULT_SERVICE_CHARGE_PERCENT = 5.0;
    
    // ===========================================
    // Printer Names
    // ===========================================
    public static final String PRINTER_KITCHEN = "Kitchen_Printer";
    public static final String PRINTER_BAR = "Bar_Printer";
    public static final String PRINTER_CASHIER = "Cashier_Printer";
}
