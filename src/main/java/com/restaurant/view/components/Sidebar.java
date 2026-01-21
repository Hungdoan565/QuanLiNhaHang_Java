package com.restaurant.view.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.Role;
import com.restaurant.model.User;
import com.restaurant.view.MainFrame;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Sidebar Navigation Component
 * 
 * Features:
 * - Collapsible sidebar
 * - Role-based menu items
 * - Active state highlighting
 * - Smooth hover effects
 */
public class Sidebar extends JPanel {
    
    // Colors
    private static final Color SIDEBAR_BG = Color.decode("#1A1A2E");
    private static final Color SIDEBAR_ITEM_HOVER = Color.decode("#2D2D44");
    private static final Color SIDEBAR_ITEM_ACTIVE = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color TEXT_LIGHT = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(255, 255, 255, 150);
    private static final Color ICON_COLOR = new Color(255, 255, 255, 220);
    private static final Color ICON_ACTIVE_COLOR = Color.WHITE;
    
    // Dimensions
    private static final int SIDEBAR_WIDTH = 220;
    private static final int SIDEBAR_COLLAPSED_WIDTH = 60;
    private static final int ITEM_HEIGHT = 44;
    
    // State
    private boolean collapsed = false;
    private String activeItemId = MainFrame.PANEL_DASHBOARD;
    private final List<SidebarItem> menuItems = new ArrayList<>();
    private final User currentUser;
    private final BiConsumer<String, String> navigationCallback;
    
    // Components
    private JPanel menuContainer;
    private JButton toggleButton;
    private JLabel logoLabel;
    private JLabel appNameLabel;
    
    public Sidebar(User user, BiConsumer<String, String> navigationCallback) {
        this.currentUser = user;
        this.navigationCallback = navigationCallback;
        
        initializeUI();
        buildMenu();
    }
    
    private void initializeUI() {
        setLayout(new MigLayout("fill, wrap, insets 0", "[fill]", "[][][grow][]"));
        setBackground(SIDEBAR_BG);
        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        
        // Header (Logo + App Name)
        JPanel header = createHeader();
        add(header, "h 80!");
        
        // Divider
        add(createDivider(), "h 1!, gaptop 8, gapbottom 8");
        
        // Menu container with scroll
        menuContainer = new JPanel(new MigLayout("wrap, insets 8, gap 4, fillx", "[fill]", ""));
        menuContainer.setOpaque(false);
        
        JScrollPane scrollPane = new JScrollPane(menuContainer);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(SIDEBAR_BG);
        
        // Style scrollbar nhỏ gọn
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scrollPane.getVerticalScrollBar().setBackground(SIDEBAR_BG);
        
        add(scrollPane, "grow");
        
        // Footer (Toggle + Logout)
        JPanel footer = createFooter();
        add(footer, "h 100!");
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 16, gap 12", "[][]", "[center]"));
        header.setOpaque(false);
        
        // Logo - custom icon
        logoLabel = new JLabel(createMenuIcon("logo", 32, ICON_COLOR));
        header.add(logoLabel);
        
        // App name
        appNameLabel = new JLabel("<html><b>" + AppConfig.APP_NAME + "</b><br><span style='font-size:10px'>Restaurant POS</span></html>");
        appNameLabel.setForeground(TEXT_LIGHT);
        appNameLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        header.add(appNameLabel);
        
        return header;
    }
    
    private JPanel createDivider() {
        JPanel divider = new JPanel();
        divider.setBackground(new Color(255, 255, 255, 30));
        return divider;
    }
    
    private void buildMenu() {
        menuContainer.removeAll();
        menuItems.clear();
        
        Role role = currentUser.getRole();
        if (role == null) {
            return; // No role = no menu
        }
        
        // CHEF special case - Kitchen + My Schedule
        if (role.isChef() && !role.isAdmin()) {
            addMenuItem(MainFrame.PANEL_KITCHEN, "kitchen", "Màn hình bếp", true);
            addMenuItem(MainFrame.PANEL_MY_SCHEDULE, "schedule", "Lịch của tôi", true);
            menuContainer.revalidate();
            menuContainer.repaint();
            return;
        }
        
        // WAITER special case - Pickup panel + My Schedule
        if (role.isWaiter()) {
            addMenuItem(MainFrame.PANEL_WAITER, "kitchen", "Lấy món", true);
            addMenuItem(MainFrame.PANEL_MY_SCHEDULE, "schedule", "Lịch của tôi", true);
            menuContainer.revalidate();
            menuContainer.repaint();
            return;
        }
        
        // Dashboard - Admin, Manager, Cashier
        if (role.canAccessDashboard()) {
            addMenuItem(MainFrame.PANEL_DASHBOARD, "dashboard", "Tổng quan", true);
        }
        
        // POS - Admin, Manager, Cashier (NOT Waiter - they use WaiterPanel)
        if (role.canAccessPOS() && !role.isWaiter()) {
            addMenuItem(MainFrame.PANEL_POS, "pos", "Bán hàng", true);
        }
        
        // Kitchen - Admin, Manager, Chef
        if (role.canAccessKitchen()) {
            addMenuItem(MainFrame.PANEL_KITCHEN, "kitchen", "Màn hình bếp", true);
        }
        
        // My Schedule - For all staff (not admin/manager since they use ScheduleManagementPanel)
        if (!role.isAdmin() && !role.isManager()) {
            addMenuItem(MainFrame.PANEL_MY_SCHEDULE, "schedule", "Lịch của tôi", true);
        }
        
        // QUẢN LÝ section - only show if has any management permission
        if (role.canManageMenu() || role.canManageInventory() || role.canManageStaff() || role.canAccessReports()) {
            menuContainer.add(Box.createVerticalStrut(8));
            menuContainer.add(createSectionLabel("QUẢN LÝ"));
            
            // Menu Management - Admin, Manager
            if (role.canManageMenu()) {
                addMenuItem(MainFrame.PANEL_MENU, "menu", "Thực đơn", true);
            }
            
            // Inventory - Admin, Manager
            if (role.canManageInventory()) {
                addMenuItem(MainFrame.PANEL_INVENTORY, "inventory", "Kho hàng", true);
            }
            
            // Staff - Admin, Manager
            if (role.canManageStaff()) {
                addMenuItem(MainFrame.PANEL_STAFF, "staff", "Nhân viên", true);
            }
            
            // Schedule - Admin, Manager
            if (role.canManageStaff()) {
                addMenuItem(MainFrame.PANEL_SCHEDULE, "schedule", "Lịch làm việc", true);
            }
            
            // Customers - Admin, Manager
            if (role.canManageStaff()) {
                addMenuItem(MainFrame.PANEL_CUSTOMERS, "customers", "Khách hàng", true);
            }
            
            // Promotions - Admin, Manager
            if (role.canManageStaff()) {
                addMenuItem(MainFrame.PANEL_PROMOTIONS, "promotions", "Khuyến mãi", true);
            }
            
            // Reservations - Admin, Manager
            if (role.canManageStaff()) {
                addMenuItem(MainFrame.PANEL_RESERVATIONS, "reservations", "Đặt bàn", true);
            }
            
            // Reports - Admin, Manager
            if (role.canAccessReports()) {
                addMenuItem(MainFrame.PANEL_REPORTS, "reports", "Báo cáo", true);
            }
        }
        
        // HỆ THỐNG section - Admin only
        if (role.canAccessSettings()) {
            menuContainer.add(Box.createVerticalStrut(8));
            menuContainer.add(createSectionLabel("HỆ THỐNG"));
            addMenuItem(MainFrame.PANEL_SETTINGS, "settings", "Cài đặt", true);
        }
        
        menuContainer.revalidate();
        menuContainer.repaint();
    }
    
    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 10));
        label.setForeground(TEXT_MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 0));
        return label;
    }
    
    private void addMenuItem(String id, String iconType, String label, boolean enabled) {
        SidebarItem item = new SidebarItem(id, iconType, label, enabled);
        menuItems.add(item);
        menuContainer.add(item, "h " + ITEM_HEIGHT + "!");
    }
    
    /**
     * Create custom painted menu icon
     */
    private static Icon createMenuIcon(String type, int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                
                int w = size;
                int h = size;
                int p = 2; // padding
                
                switch (type) {
                    case "logo" -> {
                        // Restaurant plate icon
                        g2d.drawOval(x + p, y + p + 4, w - p * 2, h - p * 2 - 4);
                        g2d.drawLine(x + w/2, y + p, x + w/2, y + p + 6);
                        g2d.drawLine(x + w/2 - 4, y + p + 2, x + w/2 + 4, y + p + 2);
                    }
                    case "dashboard" -> {
                        // Grid/dashboard icon
                        int s = (w - p * 2 - 2) / 2;
                        g2d.fillRoundRect(x + p, y + p, s, s, 3, 3);
                        g2d.fillRoundRect(x + p + s + 2, y + p, s, s, 3, 3);
                        g2d.fillRoundRect(x + p, y + p + s + 2, s, s, 3, 3);
                        g2d.fillRoundRect(x + p + s + 2, y + p + s + 2, s, s, 3, 3);
                    }
                    case "pos" -> {
                        // Shopping cart / POS icon
                        g2d.drawRoundRect(x + p + 2, y + p, w - p * 2 - 4, h - p * 2 - 4, 4, 4);
                        g2d.drawLine(x + p + 2, y + p + 5, x + w - p - 2, y + p + 5);
                        g2d.fillOval(x + p + 4, y + h - p - 5, 4, 4);
                        g2d.fillOval(x + w - p - 8, y + h - p - 5, 4, 4);
                    }
                    case "kitchen" -> {
                        // Chef hat / cooking icon
                        g2d.drawArc(x + p + 2, y + p, w - p * 2 - 4, h - p * 2 - 4, 0, 180);
                        g2d.drawLine(x + p + 4, y + h/2, x + w - p - 4, y + h/2);
                        g2d.drawOval(x + w/2 - 2, y + h/2 + 2, 4, 4);
                    }
                    case "menu" -> {
                        // Menu/list icon
                        g2d.drawRoundRect(x + p, y + p, w - p * 2, h - p * 2, 3, 3);
                        g2d.drawLine(x + p + 4, y + p + 5, x + w - p - 4, y + p + 5);
                        g2d.drawLine(x + p + 4, y + h/2, x + w - p - 4, y + h/2);
                        g2d.drawLine(x + p + 4, y + h - p - 5, x + w - p - 4, y + h - p - 5);
                    }
                    case "inventory" -> {
                        // Box/package icon
                        g2d.drawRoundRect(x + p, y + p + 3, w - p * 2, h - p * 2 - 3, 3, 3);
                        g2d.drawLine(x + p, y + p + 7, x + w - p, y + p + 7);
                        g2d.drawLine(x + w/2, y + p + 7, x + w/2, y + h - p);
                    }
                    case "staff" -> {
                        // Users/people icon
                        g2d.drawOval(x + w/2 - 3, y + p, 6, 6);
                        g2d.drawArc(x + w/2 - 6, y + p + 6, 12, 10, 0, 180);
                        // Second person (smaller, behind)
                        g2d.drawOval(x + p + 2, y + p + 2, 5, 5);
                        g2d.drawArc(x + p, y + p + 7, 9, 7, 0, 180);
                    }
                    case "reports" -> {
                        // Chart/graph icon
                        g2d.drawLine(x + p + 2, y + h - p - 2, x + p + 2, y + p + 2);
                        g2d.drawLine(x + p + 2, y + h - p - 2, x + w - p - 2, y + h - p - 2);
                        // Bars
                        g2d.fillRect(x + p + 5, y + h - p - 6, 3, 4);
                        g2d.fillRect(x + p + 9, y + h - p - 10, 3, 8);
                        g2d.fillRect(x + p + 13, y + h - p - 7, 3, 5);
                    }
                    case "schedule" -> {
                        // Calendar icon
                        g2d.drawRoundRect(x + p, y + p + 2, w - p * 2, h - p * 2 - 2, 3, 3);
                        g2d.drawLine(x + p, y + p + 6, x + w - p, y + p + 6);
                        g2d.drawLine(x + p + 4, y + p, x + p + 4, y + p + 4);
                        g2d.drawLine(x + w - p - 4, y + p, x + w - p - 4, y + p + 4);
                        // Calendar dots
                        g2d.fillRect(x + p + 4, y + p + 9, 2, 2);
                        g2d.fillRect(x + p + 8, y + p + 9, 2, 2);
                        g2d.fillRect(x + p + 12, y + p + 9, 2, 2);
                    }
                    case "customers" -> {
                        // Two people icon
                        // Person 1 (left)
                        g2d.drawOval(x + p + 2, y + p + 2, 4, 4);
                        g2d.drawArc(x + p, y + p + 6, 6, 5, 0, 180);
                        // Person 2 (right)
                        g2d.drawOval(x + p + 8, y + p + 2, 4, 4);
                        g2d.drawArc(x + p + 6, y + p + 6, 6, 5, 0, 180);
                    }
                    case "promotions" -> {
                        // Gift box icon
                        g2d.drawRoundRect(x + p + 2, y + p + 5, 10, 8, 2, 2);
                        g2d.drawLine(x + p + 7, y + p + 5, x + p + 7, y + p + 13);
                        g2d.drawLine(x + p + 2, y + p + 7, x + p + 12, y + p + 7);
                        // Ribbon/bow
                        g2d.drawArc(x + p + 4, y + p + 2, 4, 4, 0, 180);
                        g2d.drawArc(x + p + 7, y + p + 2, 4, 4, 0, 180);
                    }
                    case "reservations" -> {
                        // Calendar with clock icon
                        g2d.drawRoundRect(x + p + 1, y + p + 3, 12, 10, 2, 2);
                        g2d.drawLine(x + p + 1, y + p + 6, x + p + 13, y + p + 6);
                        g2d.drawLine(x + p + 4, y + p + 1, x + p + 4, y + p + 4);
                        g2d.drawLine(x + p + 10, y + p + 1, x + p + 10, y + p + 4);
                        // Clock hands
                        g2d.fillOval(x + p + 6, y + p + 8, 3, 3);
                    }
                    case "settings" -> {
                        // Gear/cog icon
                        int cx = x + w/2;
                        int cy = y + h/2;
                        g2d.drawOval(cx - 4, cy - 4, 8, 8);
                        for (int i = 0; i < 8; i++) {
                            double angle = Math.PI * 2 * i / 8;
                            int x1 = (int)(cx + Math.cos(angle) * 5);
                            int y1 = (int)(cy + Math.sin(angle) * 5);
                            int x2 = (int)(cx + Math.cos(angle) * 7);
                            int y2 = (int)(cy + Math.sin(angle) * 7);
                            g2d.drawLine(x1, y1, x2, y2);
                        }
                    }
                }
                g2d.dispose();
            }
            
            @Override
            public int getIconWidth() { return size; }
            
            @Override
            public int getIconHeight() { return size; }
        };
    }
    
    private JPanel createFooter() {
        JPanel footer = new JPanel(new MigLayout("wrap, insets 8", "[fill]", "[][grow][]"));
        footer.setOpaque(false);
        
        // Divider
        footer.add(createDivider(), "h 1!");
        
        // Spacer
        footer.add(Box.createVerticalGlue(), "grow");
        
        // Toggle button
        toggleButton = new JButton(collapsed ? "»" : "«");
        toggleButton.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        toggleButton.setForeground(TEXT_MUTED);
        toggleButton.setBackground(SIDEBAR_ITEM_HOVER);
        toggleButton.setBorderPainted(false);
        toggleButton.setFocusPainted(false);
        toggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleButton.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        toggleButton.addActionListener(e -> toggleCollapse());
        footer.add(toggleButton, "h 36!, gaptop 8");
        
        return footer;
    }
    
    private void toggleCollapse() {
        collapsed = !collapsed;
        
        int targetWidth = collapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_WIDTH;
        toggleButton.setText(collapsed ? "»" : "«");
        appNameLabel.setVisible(!collapsed);
        
        // Update section labels visibility
        for (Component c : menuContainer.getComponents()) {
            if (c instanceof JLabel label && !(c instanceof SidebarItem)) {
                label.setVisible(!collapsed);
            }
        }
        
        // Update menu items
        for (SidebarItem item : menuItems) {
            item.setCollapsed(collapsed);
        }
        
        setPreferredSize(new Dimension(targetWidth, 0));
        revalidate();
        repaint();
        
        // Notify parent to relayout
        if (getParent() != null) {
            getParent().revalidate();
        }
    }
    
    public void setActiveItem(String itemId) {
        this.activeItemId = itemId;
        for (SidebarItem item : menuItems) {
            item.setActive(item.getId().equals(itemId));
        }
    }
    
    /**
     * Individual sidebar menu item
     */
    private class SidebarItem extends JPanel {
        
        private final String id;
        private final String iconText;
        private final String labelText;
        private final JLabel iconLabel;
        private final JLabel textLabel;
        private boolean active = false;
        private boolean collapsed = false;
        
        public SidebarItem(String id, String icon, String label, boolean enabled) {
            this.id = id;
            this.iconText = icon;
            this.labelText = label;
            
            setLayout(new MigLayout("insets 8 12, gap 12", "[][]", "[center]"));
            setOpaque(true);
            setBackground(SIDEBAR_BG);
            setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            putClientProperty(FlatClientProperties.STYLE, "arc: 8");
            
            // Icon - use custom painted icon
            iconLabel = new JLabel(createMenuIcon(icon, 20, ICON_COLOR));
            add(iconLabel);
            
            // Text
            textLabel = new JLabel(label);
            textLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
            textLabel.setForeground(TEXT_LIGHT);
            add(textLabel);
            
            if (enabled) {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (!active) {
                            setBackground(SIDEBAR_ITEM_HOVER);
                        }
                    }
                    
                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (!active) {
                            setBackground(SIDEBAR_BG);
                        }
                    }
                    
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        navigationCallback.accept(id, labelText);
                    }
                });
            } else {
                setEnabled(false);
                iconLabel.setForeground(TEXT_MUTED);
                textLabel.setForeground(TEXT_MUTED);
            }
            
            // Set active if this is the dashboard (default)
            if (id.equals(activeItemId)) {
                setActive(true);
            }
        }
        
        public String getId() {
            return id;
        }
        
        public void setActive(boolean active) {
            this.active = active;
            if (active) {
                setBackground(SIDEBAR_ITEM_ACTIVE);
                textLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
                iconLabel.setIcon(createMenuIcon(iconText, 20, ICON_ACTIVE_COLOR));
            } else {
                setBackground(SIDEBAR_BG);
                textLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
                iconLabel.setIcon(createMenuIcon(iconText, 20, ICON_COLOR));
            }
        }
        
        public void setCollapsed(boolean collapsed) {
            this.collapsed = collapsed;
            textLabel.setVisible(!collapsed);
            
            if (collapsed) {
                setToolTipText(labelText);
            } else {
                setToolTipText(null);
            }
        }
    }
}
