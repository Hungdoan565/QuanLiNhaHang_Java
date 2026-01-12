package com.restaurant.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton Database Connection Manager using HikariCP
 * 
 * Config priority:
 * 1. ./config/database.properties (external, for deployment)
 * 2. /database.properties.default (bundled in JAR)
 */
public class DatabaseConnection {
    
    private static final Logger logger = LogManager.getLogger(DatabaseConnection.class);
    private static DatabaseConnection instance;
    private HikariDataSource dataSource;
    
    private DatabaseConnection() {
        initializeDataSource();
    }
    
    /**
     * Initialize HikariCP DataSource with config
     */
    private void initializeDataSource() {
        try {
            Properties props = loadConfig();
            HikariConfig config = new HikariConfig(props);
            
            // Additional HikariCP settings
            config.setPoolName("RestaurantPOS-Pool");
            config.addDataSourceProperty("useUnicode", "true");
            config.addDataSourceProperty("characterEncoding", "UTF-8");
            
            this.dataSource = new HikariDataSource(config);
            logger.info("✅ Database connection pool initialized successfully");
            logger.info("   Pool size: {} (min: {})", 
                config.getMaximumPoolSize(), 
                config.getMinimumIdle());
            
        } catch (Exception e) {
            logger.error("❌ Failed to initialize database connection pool", e);
            throw new RuntimeException("Cannot initialize database connection", e);
        }
    }
    
    /**
     * Load database config from file
     * Priority: external file > bundled default
     */
    private Properties loadConfig() throws IOException {
        Properties props = new Properties();
        
        // Try external config first
        File externalConfig = new File("config/database.properties");
        if (externalConfig.exists()) {
            logger.info("📁 Loading config from: {}", externalConfig.getAbsolutePath());
            try (FileInputStream fis = new FileInputStream(externalConfig)) {
                props.load(fis);
                return props;
            }
        }
        
        // Fallback to bundled config
        logger.info("📁 Loading default config from resources");
        try (InputStream is = getClass().getResourceAsStream("/database.properties.default")) {
            if (is == null) {
                throw new IOException("Cannot find database.properties.default in resources");
            }
            props.load(is);
            return props;
        }
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }
    
    /**
     * Get a connection from the pool
     * IMPORTANT: Always use try-with-resources to auto-return connection
     * 
     * Example:
     * try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
     *     // Use connection
     * }
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Check if database is connected
     */
    public boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Get pool statistics
     */
    public String getPoolStats() {
        if (dataSource != null) {
            return String.format("Active: %d, Idle: %d, Total: %d, Waiting: %d",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
            );
        }
        return "Pool not initialized";
    }
    
    /**
     * Shutdown connection pool (call on app exit)
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("🔌 Database connection pool shut down");
        }
    }
    
    /**
     * Test database connection
     */
    public static boolean testConnection() {
        try {
            DatabaseConnection db = getInstance();
            boolean connected = db.isConnected();
            if (connected) {
                logger.info("✅ Database connection test: SUCCESS");
                logger.info("   {}", db.getPoolStats());
            }
            return connected;
        } catch (Exception e) {
            logger.error("❌ Database connection test: FAILED", e);
            return false;
        }
    }
}
