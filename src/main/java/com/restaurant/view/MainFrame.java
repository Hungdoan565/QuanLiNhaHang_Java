package com.restaurant.view;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.User;
import com.restaurant.service.AuthService;
import com.restaurant.util.ToastNotification;
import com.restaurant.view.components.Sidebar;
import com.restaurant.view.panels.DashboardPanel;
import com.restaurant.view.panels.InventoryPanel;
import com.restaurant.view.panels.KitchenPanel;
import com.restaurant.view.panels.MenuPanel;
import com.restaurant.view.panels.POSPanel;
import com.restaurant.view.panels.ReportsPanel;
import com.restaurant.view.panels.SettingsPanel;
import com.restaurant.view.panels.StaffPanel;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main Application Frame with Sidebar Navigation
 * 
 * Layout:
 * ┌─────────┬────────────────────────────────────┐
 * │ Sidebar │          Content Area              │
 * │         │  ┌──────────────────────────────┐  │
 * │ 📊 Dash │  │     Header (title + user)    │  │
 * │ 🍽️ POS  │  ├──────────────────────────────┤  │
 * │ 📦 Kho  │  │                              │  │
 * │ 📋 Menu │  │       Dynamic Content        │  │
 * │ 👥 NV   │  │      (CardLayout panels)     │  │
 * │ 📈 BC   │  │                              │  │
 * │ ⚙️ Cài  │  └──────────────────────────────┘  │
 * │         │                                    │
 * │ [Logout]│                                    │
 * └─────────┴────────────────────────────────────┘
 */
public class MainFrame extends JFrame {
    
    private static final Logger logger = LogManager.getLogger(MainFrame.class);
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    
    // Current user
    private final User currentUser;
    
    // Components
    private Sidebar sidebar;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JLabel headerTitle;
    private JLabel clockLabel;
    private Timer clockTimer;
    private POSPanel posPanel;
    
    // Panel identifiers
    public static final String PANEL_DASHBOARD = "dashboard";
    public static final String PANEL_POS = "pos";
    public static final String PANEL_KITCHEN = "kitchen";
    public static final String PANEL_MENU = "menu";
    public static final String PANEL_INVENTORY = "inventory";
    public static final String PANEL_STAFF = "staff";
    public static final String PANEL_REPORTS = "reports";
    public static final String PANEL_SETTINGS = "settings";
    
    public MainFrame(User user) {
        this.currentUser = user;
        initializeFrame();
        initializeComponents();
        setupKeyBindings();
        startClock();
        
        // Show dashboard by default
        navigateTo(PANEL_DASHBOARD, "Tổng quan");
        
        logger.info("MainFrame initialized for user: {}", user.getUsername());
    }
    
    private void initializeFrame() {
        setTitle(AppConfig.APP_TITLE + " - " + currentUser.getDisplayName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(AppConfig.DEFAULT_WINDOW_WIDTH, AppConfig.DEFAULT_WINDOW_HEIGHT);
        setMinimumSize(new Dimension(AppConfig.MIN_WINDOW_WIDTH, AppConfig.MIN_WINDOW_HEIGHT));
        setLocationRelativeTo(null);
        
        // Confirm exit
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });
    }
    
    private void initializeComponents() {
        // Main container
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(BACKGROUND);
        
        // Create sidebar
        sidebar = new Sidebar(currentUser, this::navigateTo);
        mainContainer.add(sidebar, BorderLayout.WEST);
        
        // Create content area
        JPanel contentArea = createContentArea();
        mainContainer.add(contentArea, BorderLayout.CENTER);
        
        setContentPane(mainContainer);
    }
    
    private JPanel createContentArea() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        
        // Header
        JPanel header = createHeader();
        panel.add(header, BorderLayout.NORTH);
        
        // Content panel with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BACKGROUND);
        
        // Add panels - all real implementations
        DashboardPanel dashboardPanel = new DashboardPanel(currentUser);
        dashboardPanel.setOnNavigate(panelId -> {
            String title = switch (panelId) {
                case PANEL_POS -> "Bán hàng";
                case PANEL_REPORTS -> "Báo cáo";
                case PANEL_KITCHEN -> "Màn hình bếp";
                case PANEL_MENU -> "Thực đơn";
                case PANEL_INVENTORY -> "Kho hàng";
                case PANEL_STAFF -> "Nhân viên";
                case PANEL_SETTINGS -> "Cài đặt";
                default -> "Tổng quan";
            };
            navigateTo(panelId, title);
        });
        contentPanel.add(dashboardPanel, PANEL_DASHBOARD);
        
        posPanel = new POSPanel(currentUser);
        contentPanel.add(posPanel, PANEL_POS);
        contentPanel.add(new KitchenPanel(currentUser), PANEL_KITCHEN);
        contentPanel.add(new MenuPanel(currentUser), PANEL_MENU);
        contentPanel.add(new InventoryPanel(currentUser), PANEL_INVENTORY);
        contentPanel.add(new StaffPanel(currentUser), PANEL_STAFF);
        
        // Reports panel with error handling
        try {
            contentPanel.add(new ReportsPanel(currentUser), PANEL_REPORTS);
            logger.info("ReportsPanel initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize ReportsPanel", e);
            contentPanel.add(createPlaceholderPanel("📊", "Lỗi tải báo cáo", e.getMessage()), PANEL_REPORTS);
        }
        
        contentPanel.add(new SettingsPanel(currentUser), PANEL_SETTINGS);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0 0 16 0", "[grow][]", "[]"));
        header.setOpaque(false);
        
        // Title
        headerTitle = new JLabel("Tổng quan");
        headerTitle.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, AppConfig.FONT_SIZE_H2));
        headerTitle.setForeground(TEXT_PRIMARY);
        header.add(headerTitle, "grow");
        
        // Right side: Clock + User info
        JPanel rightPanel = new JPanel(new MigLayout("insets 0, gap 16", "[][]", "[]"));
        rightPanel.setOpaque(false);
        
        // Clock
        clockLabel = new JLabel();
        clockLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, AppConfig.FONT_SIZE_BODY));
        clockLabel.setForeground(TEXT_SECONDARY);
        updateClock();
        rightPanel.add(clockLabel);
        
        // User chip
        JPanel userChip = createUserChip();
        rightPanel.add(userChip);
        
        header.add(rightPanel);
        
        return header;
    }
    
    private JPanel createUserChip() {
        JPanel chip = new JPanel(new MigLayout("insets 6 12, gap 8", "[][]", "[]"));
        chip.setBackground(SURFACE);
        chip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.decode(AppConfig.Colors.BORDER), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        chip.putClientProperty(FlatClientProperties.STYLE, "arc: 20");
        
        // Avatar placeholder
        JLabel avatar = new JLabel(getAvatarEmoji());
        avatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        chip.add(avatar);
        
        // Name & Role
        JPanel info = new JPanel(new MigLayout("wrap, insets 0, gap 0", "[]", "[]0[]"));
        info.setOpaque(false);
        
        JLabel nameLabel = new JLabel(currentUser.getDisplayName());
        nameLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        nameLabel.setForeground(TEXT_PRIMARY);
        info.add(nameLabel);
        
        JLabel roleLabel = new JLabel(currentUser.getRoleName());
        roleLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 10));
        roleLabel.setForeground(TEXT_SECONDARY);
        info.add(roleLabel);
        
        chip.add(info);
        
        // Make clickable for dropdown menu
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showUserMenu(chip, e.getX(), e.getY());
            }
        });
        
        return chip;
    }
    
    private String getAvatarEmoji() {
        String roleName = currentUser.getRoleName().toUpperCase();
        return switch (roleName) {
            case "ADMIN" -> "👨‍💼";
            case "CASHIER" -> "💳";
            case "WAITER" -> "🧑‍🍳";
            case "CHEF" -> "👨‍🍳";
            default -> "👤";
        };
    }
    
    private void showUserMenu(Component parent, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        menu.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        
        JMenuItem profileItem = new JMenuItem("👤 Thông tin cá nhân");
        profileItem.addActionListener(e -> showProfile());
        menu.add(profileItem);
        
        JMenuItem changePassItem = new JMenuItem("🔑 Đổi mật khẩu");
        changePassItem.addActionListener(e -> showChangePassword());
        menu.add(changePassItem);
        
        menu.addSeparator();
        
        JMenuItem logoutItem = new JMenuItem("🚪 Đăng xuất");
        logoutItem.setForeground(Color.decode(AppConfig.Colors.ERROR));
        logoutItem.addActionListener(e -> logout());
        menu.add(logoutItem);
        
        menu.show(parent, 0, parent.getHeight());
    }
    
    private JPanel createPlaceholderPanel(String emoji, String title, String description) {
        JPanel panel = new JPanel(new MigLayout("fill, wrap", "[center]", "[center]"));
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.decode(AppConfig.Colors.BORDER), 1),
            BorderFactory.createEmptyBorder(40, 40, 40, 40)
        ));
        
        // Emoji
        JLabel emojiLabel = new JLabel(emoji);
        emojiLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        panel.add(emojiLabel, "center");
        
        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, AppConfig.FONT_SIZE_H2));
        titleLabel.setForeground(TEXT_PRIMARY);
        panel.add(titleLabel, "center, gaptop 16");
        
        // Description
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, AppConfig.FONT_SIZE_BODY));
        descLabel.setForeground(TEXT_SECONDARY);
        panel.add(descLabel, "center, gaptop 8");
        
        // Coming soon
        JLabel comingSoon = new JLabel("🚧 Đang phát triển...");
        comingSoon.setFont(new Font(AppConfig.FONT_FAMILY, Font.ITALIC, AppConfig.FONT_SIZE_CAPTION));
        comingSoon.setForeground(Color.decode(AppConfig.Colors.WARNING));
        panel.add(comingSoon, "center, gaptop 24");
        
        return panel;
    }
    
    /**
     * Navigate to a specific panel
     */
    public void navigateTo(String panelId, String title) {
        cardLayout.show(contentPanel, panelId);
        headerTitle.setText(title);
        sidebar.setActiveItem(panelId);
        
        // Refresh POS panel data when navigating to it
        if (PANEL_POS.equals(panelId) && posPanel != null) {
            posPanel.refresh();
        }
        
        logger.debug("Navigated to: {}", panelId);
    }
    
    private void setupKeyBindings() {
        JRootPane rootPane = getRootPane();
        InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rootPane.getActionMap();
        
        // F1 - Dashboard
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "dashboard");
        am.put("dashboard", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(PANEL_DASHBOARD, "Tổng quan");
            }
        });
        
        // F2 - POS
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "pos");
        am.put("pos", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentUser.canAccessPOS()) {
                    navigateTo(PANEL_POS, "Bán hàng");
                }
            }
        });
        
        // F5 - Refresh
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        am.put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ToastNotification.info(MainFrame.this, "Đã làm mới dữ liệu");
            }
        });
        
        // Ctrl+Q - Logout
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK), "logout");
        am.put("logout", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logout();
            }
        });
    }
    
    private void startClock() {
        clockTimer = new Timer(1000, e -> updateClock());
        clockTimer.start();
    }
    
    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        String time = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String date = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        clockLabel.setText(date + " | " + time);
    }
    
    private void showProfile() {
        JOptionPane.showMessageDialog(this,
            String.format("<html><b>%s</b><br>Username: %s<br>Role: %s<br>Email: %s<br>Phone: %s</html>",
                currentUser.getDisplayName(),
                currentUser.getUsername(),
                currentUser.getRoleName(),
                currentUser.getEmail() != null ? currentUser.getEmail() : "N/A",
                currentUser.getPhone() != null ? currentUser.getPhone() : "N/A"
            ),
            "Thông tin cá nhân",
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    private void showChangePassword() {
        JPasswordField currentPass = new JPasswordField();
        JPasswordField newPass = new JPasswordField();
        JPasswordField confirmPass = new JPasswordField();
        
        JPanel panel = new JPanel(new MigLayout("wrap 2", "[][grow, fill]", ""));
        panel.add(new JLabel("Mật khẩu hiện tại:"));
        panel.add(currentPass, "w 200!");
        panel.add(new JLabel("Mật khẩu mới:"));
        panel.add(newPass, "w 200!");
        panel.add(new JLabel("Xác nhận mật khẩu:"));
        panel.add(confirmPass, "w 200!");
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Đổi mật khẩu",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            
        if (result == JOptionPane.OK_OPTION) {
            String newPassword = new String(newPass.getPassword());
            String confirmPassword = new String(confirmPass.getPassword());
            
            if (!newPassword.equals(confirmPassword)) {
                ToastNotification.error(this, "Mật khẩu xác nhận không khớp!");
                return;
            }
            
            if (newPassword.length() < 6) {
                ToastNotification.error(this, "Mật khẩu phải có ít nhất 6 ký tự!");
                return;
            }
            
            // TODO: Implement password change via AuthService
            ToastNotification.success(this, "Đổi mật khẩu thành công! (Demo)");
        }
    }
    
    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc muốn đăng xuất?",
            "Xác nhận đăng xuất",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            logger.info("User {} logging out", currentUser.getUsername());
            AuthService.getInstance().logout();
            
            if (clockTimer != null) {
                clockTimer.stop();
            }
            
            dispose();
            
            // Show login frame
            SwingUtilities.invokeLater(() -> {
                new LoginFrame().setVisible(true);
            });
        }
    }
    
    private void confirmExit() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc muốn thoát ứng dụng?",
            "Xác nhận thoát",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            logger.info("Application exit requested by user: {}", currentUser.getUsername());
            if (clockTimer != null) {
                clockTimer.stop();
            }
            dispose();
            System.exit(0);
        }
    }
    
    /**
     * Get current logged in user
     */
    public User getCurrentUser() {
        return currentUser;
    }
}
