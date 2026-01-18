# 🎨 UI/UX Guidelines
## RestaurantPOS - Design System

---

## 1. Color Palette

### Primary Colors (F&B Theme - Warm & Appetizing)
| Name | Hex | Usage |
|------|-----|-------|
| **Primary** | `#E85A4F` | Buttons, CTAs, Active states |
| **Primary Dark** | `#C44536` | Hover states |
| **Primary Light** | `#FFE5E2` | Backgrounds, Tags |

### Neutral Colors
| Name | Hex | Usage |
|------|-----|-------|
| **Background** | `#F7F7F7` | Main background |
| **Surface** | `#FFFFFF` | Cards, Panels |
| **Text Primary** | `#2D3436` | Headings, Body text |
| **Text Secondary** | `#636E72` | Subtitles, Labels |
| **Border** | `#DFE6E9` | Dividers, Borders |

### Table Status Colors
| Status | Color | Hex |
|--------|-------|-----|
| Available | 🟢 Green | `#00B894` |
| Occupied | 🔴 Red | `#E74C3C` |
| Reserved | 🟡 Yellow | `#FDCB6E` |
| Cleaning | 🟠 Orange | `#F39C12` |

### Kitchen Timer Colors (Traffic Light)
| Time | Color | Hex | Behavior |
|------|-------|-----|----------|
| < 10 min | Green | `#00B894` | Normal |
| 10-20 min | Yellow | `#FDCB6E` | Warning |
| > 20 min | Red | `#E74C3C` | **Blink animation** |

---

## 2. Typography

### Font Loading 🆕
```java
// Load custom font TRƯỚC khi setup FlatLaf
try {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, 
        Main.class.getResourceAsStream("/fonts/Inter-Regular.ttf")));
    ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, 
        Main.class.getResourceAsStream("/fonts/Inter-Bold.ttf")));
} catch (Exception e) {
    e.printStackTrace();
}

UIManager.put("defaultFont", new Font("Inter", Font.PLAIN, 14));
FlatLightLaf.setup();
```

### Font Scale
| Element | Size | Weight |
|---------|------|--------|
| H1 (Dashboard Title) | 28px | Bold |
| H2 (Section Title) | 22px | SemiBold |
| H3 (Card Title) | 18px | SemiBold |
| Body | 14px | Regular |
| Caption/Label | 12px | Regular |
| Price (Money) | 16px | Bold, Monospace |

---

## 3. Icon System 🆕

> [!IMPORTANT]
> **Bắt buộc dùng SVG Icons** - PNG sẽ bị vỡ khi scale trên màn hình lớn.

### Thư viện: FlatSVGIcon (tích hợp sẵn trong FlatLaf)
```java
import com.formdev.flatlaf.extras.FlatSVGIcon;

// Load icon
FlatSVGIcon icon = new FlatSVGIcon("icons/add.svg", 16, 16);
button.setIcon(icon);

// Đổi màu icon theo context
icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Color.WHITE));
```

### Icon Sizes
| Context | Size | Example |
|---------|------|---------|
| Button Icon | 16x16px | Add, Delete, Edit |
| Menu Icon | 20x20px | Sidebar menu items |
| Feature Icon (Dashboard) | 32x32px | Revenue, Tables, Alerts |
| Empty State | 64x64px | No data illustration |

### Resources Folder Structure
```
resources/
├── icons/
│   ├── add.svg
│   ├── delete.svg
│   ├── search.svg
│   ├── payment.svg
│   └── ...
├── fonts/
│   ├── Inter-Regular.ttf
│   └── Inter-Bold.ttf
└── images/
    └── logo.png
```

---

## 4. Layout by Role

### 4.1 POS Layout (Thu ngân/Phục vụ)
```
┌──────────────────────────────────────────────────────┐
│ Header: Logo | Search | User Info                    │
├─────────────┬────────────────────────┬───────────────┤
│  CATEGORY   │      PRODUCTS          │    BILL       │
│  (Sidebar)  │      (Grid 3x4)        │   (Right)     │
│  Scrollable │      Scrollable        │               │
│             │                        │  ┌─────────┐  │
│  🥤 Đồ uống │  [Cafe sữa] [Trà đào]  │  │Scrollable│ │
│  🍲 Món chính│  [Phở bò]  [Cơm rang] │  │ Items   │  │
│  🥗 Khai vị │  ...                   │  └─────────┘  │
│  🍰 Tráng   │                        │  ═══════════  │
│             │                        │  FIXED FOOTER │
│             │                        │  Total: 150k  │
│             │                        │  [THANH TOÁN] │
└─────────────┴────────────────────────┴───────────────┘
```

> [!CAUTION]
> **Bill Panel Footer PHẢI fixed!** Nút "THANH TOÁN" và "Tổng tiền" luôn hiển thị ở đáy, chỉ danh sách món cuộn.

```java
// POS Bill Panel Structure
JPanel billPanel = new JPanel(new BorderLayout());

JScrollPane itemsScroll = new JScrollPane(itemsListPanel);
billPanel.add(itemsScroll, BorderLayout.CENTER);

JPanel fixedFooter = new JPanel(); // Total + Pay button
billPanel.add(fixedFooter, BorderLayout.SOUTH);
```

### 4.2 Kitchen Display (Dark Mode)
```
┌──────────────────────────────────────────────────────┐
│ 🍳 BẾP - Kitchen Display          [12:30 PM]        │
├──────────────┬──────────────┬──────────────┬────────┤
│   BÀN 5      │    BÀN 3     │   VIP 1      │  ...   │
│   5 phút 🟢  │   12 phút 🟡 │   25 phút 🔴 │        │
│  ──────────  │  ──────────  │  ──────────  │        │
│  • Phở bò x2 │  • Cơm rang  │  • Bò nướng  │        │
│  • Nem x1    │  (không hành)│  • Lẩu thái  │        │
│              │              │              │        │
│  [✓ XONG]    │  [✓ XONG]    │  [✓ XONG]    │        │
└──────────────┴──────────────┴──────────────┴────────┘
Background: #1A1A2E | Text: #FFFFFF | Cards: #16213E
```

### 4.3 Admin Dashboard (Bento Grid with MigLayout) 🆕
```
┌──────────────────────────────────────────────────────┐
│ Sidebar │           DASHBOARD                        │
│ ────────│ ┌─────────────────┬──────────┬──────────┐ │
│ 📊 Dash │ │  Doanh thu hôm  │ Bàn đang │ Cảnh báo │ │
│ 🍽️ POS  │ │  nay: 12.5M VND │ có khách │ kho hết  │ │
│ 📦 Kho  │ │  (Chart)        │   8/20   │   5      │ │
│ 👥 NV   │ ├─────────────────┴──────────┴──────────┤ │
│ 📈 BC   │ │         Top món bán chạy (Chart)      │ │
│ ⚙️ Cài  │ │                                       │ │
│         │ └───────────────────────────────────────┘ │
└─────────┴───────────────────────────────────────────┘
```

**MigLayout cho Bento Grid:**
```java
// Maven dependency
<dependency>
    <groupId>com.miglayout</groupId>
    <artifactId>miglayout-swing</artifactId>
    <version>11.0</version>
</dependency>

// Usage
JPanel dashboard = new JPanel(new MigLayout(
    "wrap 3, gap 16",           // 3 columns, 16px gap
    "[grow][grow][grow]",       // column constraints
    "[][]"                      // row constraints
));

dashboard.add(revenueCard, "span 1 2, grow");  // Span 2 rows
dashboard.add(tablesCard, "grow");
dashboard.add(alertsCard, "grow");
dashboard.add(topSellerChart, "span 2, grow"); // Span 2 columns
```

---

## 5. Component Specs

### 5.1 Buttons
| Type | Background | Text | Border Radius | Padding |
|------|------------|------|---------------|---------|
| Primary | `#E85A4F` | White | 8px | 12px 24px |
| Secondary | White | `#E85A4F` | 8px | 12px 24px |
| Danger | `#E74C3C` | White | 8px | 12px 24px |
| Disabled | `#BDC3C7` | `#7F8C8D` | 8px | 12px 24px |

### 5.2 Cards (Product/Table)
```
Background: White
Border: 1px solid #DFE6E9
Border Radius: 12px
Shadow: 0 2px 8px rgba(0,0,0,0.08)
Padding: 16px
Hover: Shadow increase, slight scale 1.02
```

### 5.3 Input Fields
```
Height: 44px (Touch-friendly)
Border: 1px solid #DFE6E9
Border Radius: 8px
Focus: Border Primary, Shadow glow
Placeholder: #B2BEC3
```

### 5.4 Toast Notification 🆕
> [!TIP]
> **Đừng dùng JOptionPane** cho thông báo nhỏ! Dùng Toast trượt ra từ góc phải, tự ẩn sau 3s.

```java
// Toast Component (Custom)
public class ToastNotification {
    public static void show(JFrame parent, String message, ToastType type) {
        JWindow toast = new JWindow(parent);
        JLabel label = new JLabel(message);
        
        // Style based on type (SUCCESS, ERROR, INFO)
        label.setBackground(type == ToastType.SUCCESS ? 
            new Color(0x00B894) : new Color(0xE74C3C));
        
        toast.add(label);
        toast.pack();
        
        // Position: bottom-right corner
        toast.setLocation(
            parent.getX() + parent.getWidth() - toast.getWidth() - 20,
            parent.getY() + parent.getHeight() - toast.getHeight() - 60
        );
        
        toast.setVisible(true);
        
        // Auto-hide after 3 seconds
        new Timer(3000, e -> toast.dispose()).start();
    }
}

// Usage
ToastNotification.show(mainFrame, "Đã thêm món!", ToastType.SUCCESS);
```

---

## 6. Touch & Accessibility 🆕

### Scrollbar for Touch Screen
```java
// Tăng độ rộng scrollbar để dễ vuốt trên màn hình cảm ứng
UIManager.put("ScrollBar.width", 16);
UIManager.put("ScrollBar.thumbArc", 999);  // Bo tròn
UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
UIManager.put("ScrollBar.track", new Color(0xF0F0F0));
```

### Touch Targets
- Minimum touch target: **44x44px**
- Button padding: **12px 24px** minimum
- List item height: **48px** minimum

### UI States (Best Practice 2025) 🆕
> [!IMPORTANT]
> Mọi component đều cần có đủ 4 states để UX hoàn chỉnh:

| State | Mô tả | Example |
|-------|-------|---------|
| **Empty** | Không có data | Icon 64x64 + "Chưa có món nào" + CTA button |
| **Loading** | Đang tải | Skeleton shimmer hoặc Spinner |
| **Error** | Lỗi xảy ra | Icon ⚠️ + Message + Retry button |
| **Success** | Hoàn thành | Toast notification (3s auto-hide) |

**Empty State Pattern:**
```java
// EmptyStatePanel
JPanel emptyState = new JPanel(new MigLayout("wrap, align center"));
emptyState.add(new JLabel(new FlatSVGIcon("icons/empty-cart.svg", 64, 64)), "center");
emptyState.add(new JLabel("Chưa có món nào trong giỏ"), "center");
emptyState.add(new JLabel("Chọn món từ menu bên trái"), "center, gaptop 8");
```

**Loading Skeleton Pattern:**
```java
// Dùng màu xám nhạt animation shimmer
Color skeletonBase = new Color(0xE0E0E0);
Color skeletonHighlight = new Color(0xF5F5F5);
// Animate gradient left-to-right
```

**Error State Pattern:**
```java
JPanel errorState = new JPanel(new MigLayout("wrap, align center"));
errorState.add(new JLabel(new FlatSVGIcon("icons/error.svg", 48, 48)), "center");
errorState.add(new JLabel("Không thể tải dữ liệu"), "center, gaptop 8");
JButton retryBtn = new JButton("Thử lại");
retryBtn.addActionListener(e -> loadData());
errorState.add(retryBtn, "center, gaptop 16");
```

---

## 7. Hotkeys (POS Screen)

| Key | Action |
|-----|--------|
| `F1` | Thanh toán |
| `F2` | In hóa đơn |
| `F3` | Mở bàn mới |
| `F5` | Refresh |
| `ESC` | Hủy/Đóng |
| `Enter` | Xác nhận |
| `/` | Focus Search |

---

## 8. FlatLaf Complete Setup 🆕

```java
// Main.java
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.*;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // 1. Load custom fonts
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, 
                Main.class.getResourceAsStream("/fonts/Inter-Regular.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, 
                Main.class.getResourceAsStream("/fonts/Inter-Bold.ttf")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 2. Set default font
        UIManager.put("defaultFont", new Font("Inter", Font.PLAIN, 14));
        
        // 3. Setup FlatLaf
        FlatLightLaf.setup();
        
        // 4. Customize components
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        
        // 5. Touch-friendly scrollbars
        UIManager.put("ScrollBar.width", 16);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        
        // 6. Launch app
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
    
    // Kitchen Display cần Dark Mode
    public static void setupDarkMode() {
        FlatDarkLaf.setup();
        // Override với màu Kitchen
        UIManager.put("Panel.background", new Color(0x1A1A2E));
    }
}
```

---

## 9. Development Priority (MVP) 🆕

> [!IMPORTANT]
> Đừng làm hết 100% tính năng ngay! Đi theo thứ tự MVP:

| Phase | Module | Priority |
|-------|--------|----------|
| 1 | **Admin** (Quản lý món/bàn) | ⭐⭐⭐ Làm trước để có data |
| 2 | **POS** (Order/Thanh toán) | ⭐⭐⭐ Core business |
| 3 | **Báo cáo** (Doanh thu) | ⭐⭐ |
| 4 | **KDS** (Kitchen Display) | ⭐ |
| 5 | **Kho nâng cao** (Recipe) | ⭐ |

---

*Version: 1.2 | Updated: 2026-01-12*  
*Changelog: Thêm UI States (Empty/Loading/Error/Success) từ Dribbble best practices research*

