package com.restaurant;

import com.formdev.flatlaf.FlatLightLaf;
import com.restaurant.config.AppConfig;
import com.restaurant.config.DatabaseConnection;
import com.restaurant.view.LoginFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * RestaurantPOS - Main Entry Point
 * 
 * Initializes:
 * 1. Custom fonts
 * 2. FlatLaf Look and Feel
 * 3. UI customizations
 * 4. Login frame
 */
public class Main {
    
    private static final Logger logger = LogManager.getLogger(Main.class);
    
    public static void main(String[] args) {
        logger.info("=".repeat(50));
        logger.info("Starting {} v{}", AppConfig.APP_NAME, AppConfig.APP_VERSION);
        logger.info("=".repeat(50));
        
        // Set system properties for better rendering
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        // Initialize Look and Feel on EDT
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Load custom fonts
                loadCustomFonts();
                
                // 2. Setup FlatLaf Light theme
                FlatLightLaf.setup();
                
                // 3. Apply custom UI settings
                applyUICustomizations();
                
                logger.info("✅ UI initialized successfully");
                
                // 4. Show login frame
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
                
            } catch (Exception e) {
                logger.error("❌ Failed to initialize application", e);
                JOptionPane.showMessageDialog(
                    null,
                    "Lỗi khởi động ứng dụng:\n" + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            }
        });
        
        // Add shutdown hook to cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            DatabaseConnection.getInstance().shutdown();
            logger.info("Application shutdown complete");
        }));
    }
    
    /**
     * Load custom fonts from resources
     */
    private static void loadCustomFonts() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            
            // Try to load Inter font
            var regularStream = Main.class.getResourceAsStream("/fonts/Inter-Regular.ttf");
            var boldStream = Main.class.getResourceAsStream("/fonts/Inter-Bold.ttf");
            
            if (regularStream != null) {
                ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, regularStream));
                logger.debug("Loaded font: Inter-Regular");
            }
            
            if (boldStream != null) {
                ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, boldStream));
                logger.debug("Loaded font: Inter-Bold");
            }
            
            // Set default font
            UIManager.put("defaultFont", new Font(AppConfig.FONT_FAMILY, Font.PLAIN, AppConfig.FONT_SIZE_BODY));
            
        } catch (Exception e) {
            logger.warn("Could not load custom fonts, using system default: {}", e.getMessage());
            // Fallback to system font
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, AppConfig.FONT_SIZE_BODY));
        }
    }
    
    /**
     * Apply custom UI settings to match design guidelines
     */
    private static void applyUICustomizations() {
        // Border radius for components
        UIManager.put("Button.arc", AppConfig.BORDER_RADIUS);
        UIManager.put("Component.arc", AppConfig.BORDER_RADIUS);
        UIManager.put("TextComponent.arc", AppConfig.BORDER_RADIUS);
        UIManager.put("Component.focusWidth", 1);
        
        // Scrollbar (touch-friendly)
        UIManager.put("ScrollBar.width", 16);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.track", Color.decode("#F0F0F0"));
        
        // Button defaults
        UIManager.put("Button.background", Color.decode(AppConfig.Colors.PRIMARY));
        UIManager.put("Button.focusedBackground", Color.decode(AppConfig.Colors.PRIMARY_DARK));
        UIManager.put("Button.hoverBackground", Color.decode(AppConfig.Colors.PRIMARY_DARK));
        UIManager.put("Button.pressedBackground", Color.decode(AppConfig.Colors.PRIMARY_DARK));
        
        // TextField
        UIManager.put("TextField.placeholderForeground", Color.decode("#B2BEC3"));
        
        // Table
        UIManager.put("Table.rowHeight", 40);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        
        // ComboBox
        UIManager.put("ComboBox.padding", new Insets(8, 12, 8, 12));
        
        logger.debug("Applied FlatLaf customizations");
    }
}
