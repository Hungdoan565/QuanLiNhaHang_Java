package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.User;
import com.restaurant.service.TableService;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Dashboard Panel - Tổng quan hoạt động nhà hàng
 * 
 * Features:
 * - Bento grid layout với các thẻ thống kê
 * - Mini revenue chart
 * - Loading skeleton animation
 * - Clickable stats cards
 * - Auto-refresh mỗi 30 giây
 */
public class DashboardPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(DashboardPanel.class);
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color SUCCESS = Color.decode(AppConfig.Colors.SUCCESS);
    private static final Color WARNING = Color.decode(AppConfig.Colors.WARNING);
    private static final Color ERROR = Color.decode(AppConfig.Colors.ERROR);
    private static final Color INFO = Color.decode(AppConfig.Colors.INFO);
    
    private final User currentUser;
    private final NumberFormat currencyFormat;
    private final TableService tableService;
    
    // Stats components (for updating)
    private JLabel revenueValue;
    private JLabel tablesValue;
    private JLabel ordersValue;
    private JLabel alertsValue;
    private JPanel revenueChartPanel;
    private JPanel recentOrdersTable;
    private JPanel alertsContainer;
    
    // Loading state
    private boolean isLoading = true;
    private Timer pulseTimer;
    
    // Auto-refresh timer
    private Timer refreshTimer;
    
    // Navigation callback
    private Consumer<String> onNavigate;
    
    // Chart data (demo - last 7 days revenue)
    private int[] chartData = {65, 80, 45, 90, 70, 85, 95};
    
    public DashboardPanel(User user) {
        this.currentUser = user;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        this.tableService = TableService.getInstance();
        
        initializeUI();
        startLoadingAnimation();
        loadDashboardData();
        startAutoRefresh();
    }
    
    /**
     * Set navigation callback to switch panels
     */
    public void setOnNavigate(Consumer<String> callback) {
        this.onNavigate = callback;
    }
    
    private void initializeUI() {
        setLayout(new MigLayout("fill, wrap, insets 0, gap 16", "[grow]", "[][grow]"));
        setBackground(BACKGROUND);
        
        // Welcome header
        add(createWelcomeHeader(), "growx");
        
        // Stats grid (Bento style)
        add(createStatsGrid(), "grow");
    }
    
    private JPanel createWelcomeHeader() {
        JPanel panel = new JPanel(new MigLayout("insets 0", "[]push[]", "[]"));
        panel.setOpaque(false);
        
        // Greeting with animation effect
        String greeting = getGreeting();
        JLabel greetingLabel = new JLabel(greeting + ", " + currentUser.getDisplayName() + "!");
        greetingLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, AppConfig.FONT_SIZE_H3));
        greetingLabel.setForeground(TEXT_PRIMARY);
        panel.add(greetingLabel);
        
        // Date
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", new Locale("vi")));
        JLabel dateLabel = new JLabel(today);
        dateLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, AppConfig.FONT_SIZE_BODY));
        dateLabel.setForeground(TEXT_SECONDARY);
        panel.add(dateLabel);
        
        return panel;
    }
    
    private String getGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "Chào buổi sáng";
        if (hour < 18) return "Chào buổi chiều";
        return "Chào buổi tối";
    }
    
    private JPanel createStatsGrid() {
        JPanel grid = new JPanel(new MigLayout(
            "fill, wrap 4, gap 16", 
            "[25%][25%][25%][25%]", 
            "[grow][grow]"
        ));
        grid.setOpaque(false);
        
        // Row 1: Revenue (span 2), Tables, Orders
        grid.add(createRevenueCard(), "span 2, grow");
        grid.add(createTablesCard(), "grow");
        grid.add(createOrdersCard(), "grow");
        
        // Row 2: Quick Actions, Recent Orders, Alerts
        grid.add(createQuickActionsCard(), "grow");
        grid.add(createRecentOrdersCard(), "span 2, grow");
        grid.add(createAlertsCard(), "grow");
        
        return grid;
    }
    
    private JPanel createRevenueCard() {
        JPanel card = createCard("💰", "Doanh thu hôm nay", SUCCESS);
        
        // Revenue value with loading state
        revenueValue = new JLabel("Đang tải...");
        revenueValue.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 32));
        revenueValue.setForeground(TEXT_PRIMARY);
        card.add(revenueValue, "center, gaptop 8");
        
        // Comparison badge
        JPanel badge = new JPanel(new MigLayout("insets 4 8", "[]", ""));
        badge.setBackground(new Color(SUCCESS.getRed(), SUCCESS.getGreen(), SUCCESS.getBlue(), 40));
        badge.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        JLabel comparison = new JLabel("📈 +15% so với hôm qua");
        comparison.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
        comparison.setForeground(SUCCESS);
        badge.add(comparison);
        card.add(badge, "center, gaptop 8");
        
        // Mini chart
        revenueChartPanel = createMiniChart();
        card.add(revenueChartPanel, "grow, h 60!, gaptop 12");
        
        return card;
    }
    
    /**
     * Create animated mini bar chart for revenue trends
     */
    private JPanel createMiniChart() {
        JPanel chart = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int barWidth = (getWidth() - 48) / 7;
                int maxHeight = getHeight() - 20;
                int x = 24;
                
                String[] days = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
                
                for (int i = 0; i < 7; i++) {
                    int barHeight = (int) (maxHeight * chartData[i] / 100.0);
                    int y = maxHeight - barHeight + 10;
                    
                    // Gradient bar
                    Color barColor = i == 6 ? SUCCESS : new Color(SUCCESS.getRed(), SUCCESS.getGreen(), SUCCESS.getBlue(), 120);
                    g2d.setColor(barColor);
                    g2d.fill(new RoundRectangle2D.Float(x, y, barWidth - 4, barHeight, 6, 6));
                    
                    // Day label
                    g2d.setColor(TEXT_SECONDARY);
                    g2d.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 9));
                    FontMetrics fm = g2d.getFontMetrics();
                    int textX = x + (barWidth - 4 - fm.stringWidth(days[i])) / 2;
                    g2d.drawString(days[i], textX, getHeight() - 2);
                    
                    x += barWidth;
                }
                
                g2d.dispose();
            }
        };
        chart.setOpaque(false);
        return chart;
    }
    
    private JPanel createTablesCard() {
        JPanel card = createClickableCard("🪑", "Bàn đang phục vụ", INFO, () -> {
            if (onNavigate != null) onNavigate.accept("pos");
        });
        
        tablesValue = new JLabel("--/--");
        tablesValue.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 28));
        tablesValue.setForeground(TEXT_PRIMARY);
        card.add(tablesValue, "center, gaptop 8");
        
        JLabel label = new JLabel("bàn có khách");
        label.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        label.setForeground(TEXT_SECONDARY);
        card.add(label, "center");
        
        // Progress indicator
        JPanel progressContainer = new JPanel(new MigLayout("insets 8 0 0 0", "[grow]", ""));
        progressContainer.setOpaque(false);
        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(40);
        progress.setStringPainted(false);
        progress.setPreferredSize(new Dimension(100, 6));
        progress.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 3; background: " + colorToHex(BACKGROUND));
        progressContainer.add(progress, "grow");
        card.add(progressContainer, "growx, gaptop 8");
        
        return card;
    }
    
    private JPanel createOrdersCard() {
        JPanel card = createClickableCard("📋", "Đơn chờ xử lý", WARNING, () -> {
            if (onNavigate != null) onNavigate.accept("kitchen");
        });
        
        ordersValue = new JLabel("--");
        ordersValue.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 28));
        ordersValue.setForeground(TEXT_PRIMARY);
        card.add(ordersValue, "center, gaptop 8");
        
        JLabel label = new JLabel("đơn hàng");
        label.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        label.setForeground(TEXT_SECONDARY);
        card.add(label, "center");
        
        // Pulsing dot indicator
        JPanel indicator = new JPanel() {
            private float alpha = 1.0f;
            {
                setOpaque(false);
                setPreferredSize(new Dimension(12, 12));
                
                // Pulse animation
                Timer pulseTimer = new Timer();
                pulseTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        alpha = alpha > 0.3f ? alpha - 0.1f : 1.0f;
                        repaint();
                    }
                }, 0, 100);
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(WARNING.getRed(), WARNING.getGreen(), WARNING.getBlue(), (int)(alpha * 255)));
                g2d.fillOval(0, 0, 12, 12);
                g2d.dispose();
            }
        };
        
        JPanel indicatorRow = new JPanel(new MigLayout("insets 8 0 0 0", "push[][]push", ""));
        indicatorRow.setOpaque(false);
        indicatorRow.add(indicator);
        JLabel liveLabel = new JLabel("Live");
        liveLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 10));
        liveLabel.setForeground(WARNING);
        indicatorRow.add(liveLabel);
        card.add(indicatorRow, "center, gaptop 8");
        
        return card;
    }
    
    private JPanel createQuickActionsCard() {
        JPanel card = createCard("⚡", "Thao tác nhanh", PRIMARY);
        
        JPanel buttons = new JPanel(new MigLayout("wrap, insets 0, gap 8", "[grow]", ""));
        buttons.setOpaque(false);
        
        buttons.add(createQuickButton("➕ Mở bàn mới", PRIMARY, this::openNewTable), "growx");
        buttons.add(createQuickButton("🧾 Thanh toán", SUCCESS, this::goToPayment), "growx");
        buttons.add(createQuickButton("📊 Báo cáo ca", INFO, this::goToReports), "growx");
        
        card.add(buttons, "grow, gaptop 8");
        
        return card;
    }
    
    private JButton createQuickButton(String text, Color accentColor, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        btn.setBackground(BACKGROUND);
        btn.setForeground(TEXT_PRIMARY);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 8; hoverBackground: " + colorToHex(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 40)));
        
        // Hover effect - change text color
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(accentColor);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(TEXT_PRIMARY);
            }
        });
        
        btn.addActionListener(e -> action.run());
        return btn;
    }
    
    // ===== QUICK ACTION HANDLERS =====
    
    private void openNewTable() {
        if (onNavigate != null) {
            onNavigate.accept("pos");
        }
        ToastNotification.info(SwingUtilities.getWindowAncestor(this), "Chọn bàn trống và nhấn 'Mở bàn' để bắt đầu");
    }
    
    private void goToPayment() {
        if (onNavigate != null) {
            onNavigate.accept("pos");
        }
        ToastNotification.info(SwingUtilities.getWindowAncestor(this), "Chọn bàn có khách để thanh toán");
    }
    
    private void goToReports() {
        if (onNavigate != null) {
            onNavigate.accept("reports");
        }
    }
    
    private JPanel createRecentOrdersCard() {
        JPanel card = createCard("🕐", "Đơn hàng gần đây", INFO);
        
        // Table container
        recentOrdersTable = new JPanel(new MigLayout("wrap 4, insets 0, gap 8", "[60][grow][80][100]", ""));
        recentOrdersTable.setOpaque(false);
        
        // Header
        recentOrdersTable.add(createTableHeader("Mã"));
        recentOrdersTable.add(createTableHeader("Bàn"));
        recentOrdersTable.add(createTableHeader("Tổng"));
        recentOrdersTable.add(createTableHeader("Trạng thái"));
        
        card.add(recentOrdersTable, "grow, gaptop 8");
        
        return card;
    }
    
    private JLabel createTableHeader(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
        label.setForeground(TEXT_SECONDARY);
        return label;
    }
    
    private void addOrderRow(JPanel table, String code, String tableName, String total, String status, Color statusColor) {
        JLabel codeLabel = new JLabel(code);
        codeLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        codeLabel.setForeground(TEXT_PRIMARY);
        table.add(codeLabel);
        
        JLabel tableLabel = new JLabel(tableName);
        tableLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        tableLabel.setForeground(TEXT_PRIMARY);
        table.add(tableLabel);
        
        JLabel totalLabel = new JLabel(total);
        totalLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        totalLabel.setForeground(TEXT_PRIMARY);
        table.add(totalLabel);
        
        // Status badge
        JPanel statusBadge = new JPanel(new MigLayout("insets 2 6", "[]", ""));
        statusBadge.setBackground(new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), 40));
        statusBadge.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        JLabel statusLabel = new JLabel(status);
        statusLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 10));
        statusLabel.setForeground(statusColor);
        statusBadge.add(statusLabel);
        table.add(statusBadge);
    }
    
    private JPanel createAlertsCard() {
        JPanel card = createClickableCard("⚠️", "Cảnh báo", ERROR, () -> {
            if (onNavigate != null) onNavigate.accept("inventory");
        });
        
        alertsValue = new JLabel("--");
        alertsValue.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 28));
        alertsValue.setForeground(ERROR);
        card.add(alertsValue, "center, gaptop 8");
        
        JLabel label = new JLabel("nguyên liệu sắp hết");
        label.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        label.setForeground(TEXT_SECONDARY);
        card.add(label, "center");
        
        // Alert items container
        alertsContainer = new JPanel(new MigLayout("wrap, insets 0, gap 4", "[grow]", ""));
        alertsContainer.setOpaque(false);
        card.add(alertsContainer, "grow, gaptop 8");
        
        return card;
    }
    
    private void addAlertItem(JPanel container, String name, String remaining) {
        JPanel item = new JPanel(new MigLayout("insets 4 8, gap 4", "[]push[]", ""));
        item.setBackground(new Color(ERROR.getRed(), ERROR.getGreen(), ERROR.getBlue(), 30));
        item.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 11));
        nameLabel.setForeground(TEXT_PRIMARY);
        item.add(nameLabel);
        
        JLabel remainLabel = new JLabel(remaining);
        remainLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
        remainLabel.setForeground(ERROR);
        item.add(remainLabel);
        
        // Hover effect
        item.addMouseListener(new MouseAdapter() {
            Color originalBg = item.getBackground();
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setBackground(new Color(ERROR.getRed(), ERROR.getGreen(), ERROR.getBlue(), 60));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(originalBg);
            }
        });
        
        container.add(item, "growx");
    }
    
    private JPanel createCard(String icon, String title, Color accentColor) {
        JPanel card = new JPanel(new MigLayout("wrap, insets 16, gap 0", "[grow, center]", ""));
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.decode(AppConfig.Colors.BORDER), 1),
            BorderFactory.createEmptyBorder()
        ));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        
        // Header with icon and title
        JPanel header = new JPanel(new MigLayout("insets 0, gap 8", "[][]push", ""));
        header.setOpaque(false);
        
        // Icon badge
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        header.add(iconLabel);
        
        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        titleLabel.setForeground(TEXT_PRIMARY);
        header.add(titleLabel);
        
        card.add(header, "growx");
        
        return card;
    }
    
    private JPanel createClickableCard(String icon, String title, Color accentColor, Runnable onClick) {
        JPanel card = createCard(icon, title, accentColor);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(accentColor, 2),
                    BorderFactory.createEmptyBorder()
                ));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.decode(AppConfig.Colors.BORDER), 1),
                    BorderFactory.createEmptyBorder()
                ));
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }
        });
        
        return card;
    }
    
    /**
     * Start loading skeleton animation
     */
    private void startLoadingAnimation() {
        isLoading = true;
        pulseTimer = new Timer();
        pulseTimer.scheduleAtFixedRate(new TimerTask() {
            private float alpha = 0.5f;
            private boolean increasing = true;
            
            @Override
            public void run() {
                if (!isLoading) {
                    pulseTimer.cancel();
                    return;
                }
                
                if (increasing) {
                    alpha += 0.05f;
                    if (alpha >= 1.0f) increasing = false;
                } else {
                    alpha -= 0.05f;
                    if (alpha <= 0.5f) increasing = true;
                }
                
                // Update loading labels opacity would go here
                // For simplicity, we just show loading text
            }
        }, 0, 50);
    }
    
    private void loadDashboardData() {
        SwingWorker<DashboardStats, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardStats doInBackground() {
                DashboardStats stats = new DashboardStats();
                
                try {
                    // Simulate network delay for demo
                    Thread.sleep(500);
                    
                    // Load table stats from database
                    TableService.TableStats tableStats = tableService.getStats();
                    stats.occupiedTables = tableStats.occupied();
                    stats.totalTables = tableStats.total();
                    
                    // TODO: Load from OrderDAO when implemented
                    stats.todayRevenue = 12500000;
                    stats.pendingOrders = Math.max(1, stats.occupiedTables);
                    stats.lowStockItems = 2;
                    
                } catch (Exception e) {
                    logger.error("Error loading dashboard stats", e);
                }
                
                return stats;
            }
            
            @Override
            protected void done() {
                try {
                    isLoading = false;
                    DashboardStats stats = get();
                    updateUI(stats);
                } catch (Exception e) {
                    logger.error("Error updating dashboard UI", e);
                }
            }
        };
        worker.execute();
    }
    
    private void updateUI(DashboardStats stats) {
        SwingUtilities.invokeLater(() -> {
            // Animate revenue value
            animateValue(revenueValue, currencyFormat.format(stats.todayRevenue));
            
            // Update tables with progress
            tablesValue.setText(stats.occupiedTables + "/" + stats.totalTables);
            
            // Update orders
            ordersValue.setText(String.valueOf(stats.pendingOrders));
            
            // Update alerts
            alertsValue.setText(String.valueOf(stats.lowStockItems));
            
            // Update recent orders table
            updateRecentOrders();
            
            // Update alerts list
            updateAlertsList();
            
            // Refresh chart
            if (revenueChartPanel != null) {
                revenueChartPanel.repaint();
            }
            
            logger.debug("Dashboard UI updated");
        });
        
        logger.info("Dashboard data loaded");
    }
    
    /**
     * Simple text animation effect
     */
    private void animateValue(JLabel label, String targetText) {
        label.setText(targetText);
        // Could add fade-in effect here
    }
    
    private void updateRecentOrders() {
        // Clear existing rows (keep header)
        while (recentOrdersTable.getComponentCount() > 4) {
            recentOrdersTable.remove(recentOrdersTable.getComponentCount() - 1);
        }
        
        // TODO: Load from OrderDAO when implemented
        addOrderRow(recentOrdersTable, "ORD001", "Bàn 5", "250,000 ₫", "Đang phục vụ", WARNING);
        addOrderRow(recentOrdersTable, "ORD002", "Bàn 3", "180,000 ₫", "Chờ thanh toán", INFO);
        addOrderRow(recentOrdersTable, "ORD003", "VIP 1", "520,000 ₫", "Hoàn thành", SUCCESS);
        
        recentOrdersTable.revalidate();
        recentOrdersTable.repaint();
    }
    
    private void updateAlertsList() {
        alertsContainer.removeAll();
        
        // TODO: Load from IngredientDAO when implemented
        addAlertItem(alertsContainer, "Cà phê hạt", "Còn 500g");
        addAlertItem(alertsContainer, "Sữa tươi", "Còn 2L");
        
        alertsContainer.revalidate();
        alertsContainer.repaint();
    }
    
    /**
     * Start auto-refresh timer (every 30 seconds)
     */
    private void startAutoRefresh() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                loadDashboardData();
            }
        }, 30000, 30000);
    }
    
    /**
     * Stop auto-refresh timer
     */
    public void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
        if (pulseTimer != null) {
            pulseTimer.cancel();
            pulseTimer = null;
        }
    }
    
    /**
     * Manual refresh
     */
    public void refresh() {
        loadDashboardData();
        ToastNotification.info(SwingUtilities.getWindowAncestor(this), "Đã làm mới dữ liệu");
    }
    
    private String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    /**
     * Stats holder class
     */
    private static class DashboardStats {
        long todayRevenue = 0;
        int occupiedTables = 0;
        int totalTables = 0;
        int pendingOrders = 0;
        int lowStockItems = 0;
    }
}
