# 🏗️ Technical Architecture Document
## Hệ thống Quản lý Nhà hàng - RestaurantPOS

---

## 1. System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                        │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │  Login  │ │   POS   │ │ Kitchen │ │  Admin  │ │ Reports │   │
│  │  View   │ │  View   │ │ Display │ │Dashboard│ │  View   │   │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘   │
│       └───────────┴───────────┴───────────┴───────────┘         │
│                              │                                   │
│                     Java Swing + FlatLaf                        │
└──────────────────────────────┼──────────────────────────────────┘
                               │
┌──────────────────────────────┼──────────────────────────────────┐
│                        CONTROLLER LAYER                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │AuthController│ │OrderController│ │ReportController│ ...       │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘               │
└─────────┼───────────────┼───────────────┼───────────────────────┘
          │               │               │
┌─────────┼───────────────┼───────────────┼───────────────────────┐
│                        SERVICE LAYER (BUS)                       │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐ │
│  │ AuthService │ │ OrderService│ │ StockService│ │PrintService│ │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └─────┬─────┘ │
└─────────┼───────────────┼───────────────┼──────────────┼────────┘
          │               │               │              │
┌─────────┼───────────────┼───────────────┼──────────────┼────────┐
│                        DATA ACCESS LAYER (DAO)                   │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │   UserDAO   │ │  OrderDAO   │ │ ProductDAO  │ ...           │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘               │
│         └───────────────┴───────────────┘                       │
│                         │                                        │
│              ┌──────────┴──────────┐                            │
│              │  DatabaseConnection │                            │
│              │  (HikariCP Pool)    │  ← 🆕 Connection Pool      │
│              └──────────┬──────────┘                            │
└─────────────────────────┼───────────────────────────────────────┘
                          │
┌─────────────────────────┼───────────────────────────────────────┐
│                    DATABASE LAYER                                │
│              ┌──────────┴──────────┐                            │
│              │      MySQL 8.0      │                            │
│              │   (Local Server)    │                            │
│              └─────────────────────┘                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Project Structure

```
QL_NhaHang/
├── config/                            # 🆕 External config (ngoài JAR)
│   └── database.properties
│
├── docs/                              # Documentation
│   ├── PRD.md
│   ├── TECHNICAL_ARCHITECTURE.md
│   ├── DATABASE_DESIGN.md
│   └── UI_GUIDELINES.md
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── restaurant/
│       │           ├── Main.java
│       │           │
│       │           ├── config/
│       │           │   ├── DatabaseConfig.java
│       │           │   └── AppConfig.java
│       │           │
│       │           ├── model/
│       │           │   ├── User.java
│       │           │   ├── Shift.java      # 🆕
│       │           │   ├── Order.java
│       │           │   └── ...
│       │           │
│       │           ├── dao/
│       │           │   ├── interfaces/
│       │           │   ├── impl/
│       │           │   └── DatabaseConnection.java
│       │           │
│       │           ├── service/
│       │           │   ├── AuthService.java
│       │           │   ├── OrderService.java
│       │           │   ├── ShiftService.java  # 🆕
│       │           │   ├── PrintService.java  # 🆕
│       │           │   └── ...
│       │           │
│       │           ├── controller/
│       │           ├── view/
│       │           ├── util/
│       │           └── constant/
│       │
│       └── resources/
│           ├── database.properties.default  # 🆕 Fallback config
│           ├── images/
│           ├── fonts/
│           └── reports/
│
├── pom.xml
└── README.md
```

---

## 3. Design Patterns Applied

### 3.1 Singleton Pattern - Connection Pool (HikariCP) 🆕
> [!IMPORTANT]
> Singleton giữ **HikariDataSource** (bể chứa), KHÔNG giữ Connection đơn lẻ!

```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private HikariDataSource dataSource;

    private DatabaseConnection() {
        // Load config (ưu tiên file ngoài JAR)
        String configPath = loadConfigPath();
        
        HikariConfig config = new HikariConfig(configPath);
        // Hoặc set thủ công:
        // config.setJdbcUrl("jdbc:mysql://192.168.1.100:3306/restaurant_db");
        // config.setUsername("restaurant_app");
        // config.setPassword("secret");
        
        // Tối ưu pool cho POS
        config.setMaximumPoolSize(10);  // Max 10 connections
        config.setMinimumIdle(2);       // Luôn giữ 2 sẵn sàng
        config.setIdleTimeout(30000);   // 30s không dùng -> đóng bớt
        config.setConnectionTimeout(10000); // 10s timeout
        
        this.dataSource = new HikariDataSource(config);
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection(); // Xin từ pool, tự trả khi close
    }
    
    private String loadConfigPath() {
        // Ưu tiên file ngoài JAR (để dễ config khi deploy)
        File externalConfig = new File("config/database.properties");
        if (externalConfig.exists()) {
            return externalConfig.getAbsolutePath();
        }
        // Fallback: file trong resources
        return getClass().getResource("/database.properties.default").getPath();
    }
    
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
```

**Cách dùng trong DAO (try-with-resources tự động trả connection):**
```java
public class UserDAOImpl implements IUserDAO {
    @Override
    public User getByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            // ... mapping
            
        } catch (SQLException e) {
            logger.error("Error fetching user", e);
        }
        return null;
    }
}
```

### 3.2 DAO Pattern - Data Access
```java
public interface IProductDAO {
    List<Product> getAll();
    Product getById(int id);
    boolean insert(Product product);
    boolean update(Product product);
    boolean delete(int id);
    List<Product> searchByName(String keyword);
}

public class ProductDAOImpl implements IProductDAO {
    @Override
    public List<Product> getAll() {
        String sql = "SELECT * FROM products WHERE is_active = 1";
        // Use try-with-resources as shown above
    }
}
```

### 3.3 MVC Pattern - Controller
```java
public class OrderController {
    private OrderService orderService;
    private POSPanel posPanel;
    
    public OrderController(POSPanel panel) {
        this.posPanel = panel;
        this.orderService = new OrderService();
    }
    
    public void handleAddItem(int productId, int quantity) {
        // Validate → Call service → Update view
    }
}
```

### 3.4 Observer Pattern + Polling - Kitchen Display 🆕
```java
// KitchenPanel tự động refresh mỗi 5 giây
public class KitchenPanel extends JPanel {
    private Timer refreshTimer;
    private OrderService orderService;
    
    public KitchenPanel() {
        this.orderService = new OrderService();
        startAutoRefresh();
    }
    
    private void startAutoRefresh() {
        refreshTimer = new Timer(5000, e -> {
            // Chạy trên background thread
            new SwingWorker<List<OrderDetail>, Void>() {
                @Override
                protected List<OrderDetail> doInBackground() {
                    return orderService.getPendingOrders();
                }
                @Override
                protected void done() {
                    try {
                        updateOrderCards(get());
                    } catch (Exception ex) {
                        logger.error("Refresh failed", ex);
                    }
                }
            }.execute();
        });
        refreshTimer.start();
    }
    
    public void stopAutoRefresh() {
        if (refreshTimer != null) refreshTimer.stop();
    }
}
```

---

## 4. Key Technologies & Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| **Java** | 17+ | Core language |
| **Maven** | 3.9+ | Build & dependency management |
| **MySQL Connector/J** | 8.0.33 | Database driver |
| **HikariCP** | 5.0.1 | Connection pooling ✅ |
| **FlatLaf** | 3.4+ | Modern Look & Feel |
| **JCalendar** | 1.4 | Date picker component |
| **JFreeChart** | 1.5.4 | Charts for reports |
| **JasperReports** | 6.20+ | PDF report generation |
| **BCrypt** | 0.10.2 | Password hashing |
| **Log4j2** | 2.20+ | Logging framework |

---

## 5. Maven Dependencies (pom.xml)

```xml
<dependencies>
    <!-- MySQL Connector -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.0.33</version>
    </dependency>
    
    <!-- 🆕 HikariCP Connection Pool -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>5.0.1</version>
    </dependency>
    
    <!-- FlatLaf Look and Feel -->
    <dependency>
        <groupId>com.formdev</groupId>
        <artifactId>flatlaf</artifactId>
        <version>3.4</version>
    </dependency>
    
    <!-- JFreeChart -->
    <dependency>
        <groupId>org.jfree</groupId>
        <artifactId>jfreechart</artifactId>
        <version>1.5.4</version>
    </dependency>
    
    <!-- JCalendar -->
    <dependency>
        <groupId>com.toedter</groupId>
        <artifactId>jcalendar</artifactId>
        <version>1.4</version>
    </dependency>
    
    <!-- BCrypt for password hashing -->
    <dependency>
        <groupId>at.favre.lib</groupId>
        <artifactId>bcrypt</artifactId>
        <version>0.10.2</version>
    </dependency>
    
    <!-- Log4j2 -->
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-core</artifactId>
        <version>2.20.0</version>
    </dependency>
</dependencies>
```

---

## 6. Configuration Strategy 🆕

### 6.1 External Config Priority
```
Ưu tiên đọc config:
1. ./config/database.properties     ← File ngoài JAR (deploy)
2. src/resources/database.properties.default  ← Fallback
```

### 6.2 database.properties
```properties
# Database
jdbcUrl=jdbc:mysql://192.168.1.100:3306/restaurant_db
dataSource.user=restaurant_app
dataSource.password=secret_password

# HikariCP Pool
maximumPoolSize=10
minimumIdle=2
idleTimeout=30000
connectionTimeout=10000
```

> [!TIP]
> Khi deploy, chỉ cần copy file `database.properties` ra thư mục `config/` bên cạnh file `.exe/.jar` và sửa IP. Không cần build lại!

### 6.3 LAN Architecture
```
┌─────────────────┐     LAN      ┌─────────────────┐
│   POS Station   │◄────────────►│  MySQL Server   │
│   (Client App)  │              │  (Main Machine) │
└─────────────────┘              └─────────────────┘
        ▲                                ▲
        │            LAN                 │
        │     ┌─────────────────┐        │
        └────►│ Kitchen Display │◄───────┘
              │ (Auto-refresh)  │
              └─────────────────┘
```

---

## 7. Printing Service 🆕

```java
import javax.print.*;
import java.awt.print.*;

public class PrintService {
    
    /**
     * In order xuống bếp (Kitchen Ticket)
     */
    public void printKitchenTicket(Order order, String printerName) {
        PrinterJob job = PrinterJob.getPrinterJob();
        
        // Tìm máy in theo tên
        javax.print.PrintService printer = findPrinter(printerName);
        if (printer != null) {
            try {
                job.setPrintService(printer);
                job.setPrintable(new KitchenTicketPrintable(order));
                job.print();
            } catch (PrinterException e) {
                logger.error("Print failed: " + printerName, e);
            }
        }
    }
    
    /**
     * In hóa đơn (Receipt)
     */
    public void printReceipt(Order order) {
        // Dùng JasperReports để tạo PDF rồi in
        // Hoặc in trực tiếp qua máy in nhiệt
    }
    
    /**
     * Mở két tiền (Cash Drawer)
     */
    public void openCashDrawer(String printerName) {
        // Gửi ESC/POS command xuống máy in nhiệt
        // Thường là: 0x1B, 0x70, 0x00, 0x19, 0xFA
        byte[] openDrawerCmd = {0x1B, 0x70, 0x00, 0x19, (byte)0xFA};
        sendToPrinter(printerName, openDrawerCmd);
    }
    
    private javax.print.PrintService findPrinter(String name) {
        for (javax.print.PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
            if (ps.getName().equalsIgnoreCase(name)) {
                return ps;
            }
        }
        return null;
    }
}
```

---

## 8. Security Considerations

### 8.1 Password Handling
```java
String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
boolean isValid = BCrypt.checkpw(inputPassword, hashedPassword);
```

### 8.2 SQL Injection Prevention
```java
String sql = "SELECT * FROM users WHERE username = ?";
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setString(1, username);
```

### 8.3 Audit Logging
```java
public class AuditLog {
    public static void log(int userId, String action, String tableName, 
                           int recordId, String oldValues, String newValues) {
        String sql = "INSERT INTO audit_logs (...) VALUES (...)";
        // Log to database
    }
}
```

---

## 9. Error Handling Strategy

```java
public class ServiceException extends Exception {
    public static final int INVALID_INPUT = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int NOT_FOUND = 404;
    public static final int DATABASE_ERROR = 500;
    
    private int errorCode;
    public ServiceException(String msg, int code) {
        super(msg);
        this.errorCode = code;
    }
}
```

---

## 10. Threading Strategy (SwingWorker)

```java
// Long-running tasks MUST use SwingWorker
public class LoadReportWorker extends SwingWorker<Report, Void> {
    @Override
    protected Report doInBackground() throws Exception {
        return reportService.generateMonthlyReport();
    }
    
    @Override
    protected void done() {
        try {
            updateUI(get());
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }
}
```

---

## 11. Deployment

### 11.1 Build Executable JAR
```bash
mvn clean package
```

### 11.2 Create Windows Executable
```bash
jpackage --input target/ \
         --name RestaurantPOS \
         --main-jar restaurant-pos.jar \
         --type exe \
         --icon src/main/resources/images/icon.ico
```

### 11.3 Deployment Folder Structure
```
RestaurantPOS/
├── RestaurantPOS.exe
├── config/
│   └── database.properties  ← Sửa IP ở đây
├── logs/
└── reports/
```

---

*Document Version: 1.1*  
*Last Updated: 2026-01-12*  
*Changelog: Fix Singleton với HikariCP, thêm PrintService, External Config, Kitchen Polling*
