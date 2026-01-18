package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.User;
import com.restaurant.util.KitchenOrderManager;
import com.restaurant.util.KitchenOrderManager.KitchenOrder;
import com.restaurant.util.KitchenOrderManager.OrderStatus;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Kitchen Display Panel - Màn hình bếp
 * 
 * Features:
 * - Nhận đơn real-time từ POS qua KitchenOrderManager
 * - Màu sắc theo thời gian chờ (xanh -> vàng -> đỏ)
 * - Click để đánh dấu hoàn thành
 * - Auto-refresh mỗi 5 giây
 */
public class KitchenPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(KitchenPanel.class);
    
    // Colors
    private static final Color BACKGROUND = Color.decode("#1A1A2E");
    private static final Color SURFACE = Color.decode("#2D2D44");
    private static final Color TEXT_LIGHT = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(255, 255, 255, 180);
    private static final Color STATUS_NEW = Color.decode("#00B894");
    private static final Color STATUS_PREPARING = Color.decode("#FDCB6E");
    private static final Color STATUS_URGENT = Color.decode("#E74C3C");
    
    private final User currentUser;
    private final KitchenOrderManager orderManager;
    private List<KitchenOrder> orders = new ArrayList<>();
    private JPanel ordersGrid;
    private JLabel statsLabel;
    private Timer refreshTimer;
    private Consumer<List<KitchenOrder>> orderListener;
    
    public KitchenPanel(User user) {
        this.currentUser = user;
        this.orderManager = KitchenOrderManager.getInstance();
        initializeUI();
        setupOrderListener();
        loadOrders();
        startAutoRefresh();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        
        // Header
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);
        
        // Orders grid
        ordersGrid = new JPanel(new MigLayout("wrap 4, gap 16", "[grow][grow][grow][grow]", ""));
        ordersGrid.setOpaque(false);
        
        JScrollPane scrollPane = new JScrollPane(ordersGrid);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 16", "[]push[][]", ""));
        header.setOpaque(false);
        
        // Title
        JLabel title = new JLabel("🍳 Màn hình bếp");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 28));
        title.setForeground(TEXT_LIGHT);
        header.add(title);
        
        // Stats
        statsLabel = new JLabel("0 đang chờ | 0 đang làm");
        statsLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        statsLabel.setForeground(TEXT_MUTED);
        header.add(statsLabel);
        
        // Refresh button
        JButton refreshBtn = new JButton("🔄 Làm mới");
        refreshBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        refreshBtn.setForeground(TEXT_LIGHT);
        refreshBtn.setBackground(SURFACE);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        refreshBtn.addActionListener(e -> refresh());
        header.add(refreshBtn);
        
        return header;
    }
    
    /**
     * Setup listener to receive real-time order updates from POS
     */
    private void setupOrderListener() {
        orderListener = updatedOrders -> {
            SwingUtilities.invokeLater(() -> {
                this.orders = new ArrayList<>(updatedOrders);
                refreshOrdersGrid();
                
                // Play notification sound for new orders
                if (!orders.isEmpty()) {
                    Toolkit.getDefaultToolkit().beep();
                }
            });
        };
        orderManager.addListener(orderListener);
    }
    
    private void loadOrders() {
        // Get orders from manager
        orders = orderManager.getPendingOrders();
        refreshOrdersGrid();
    }
    
    private void refreshOrdersGrid() {
        ordersGrid.removeAll();
        
        if (orders.isEmpty()) {
            // Empty state
            JPanel emptyPanel = new JPanel(new MigLayout("fill, wrap", "[center]", "[center]"));
            emptyPanel.setOpaque(false);
            
            JLabel emptyIcon = new JLabel("✅");
            emptyIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
            emptyPanel.add(emptyIcon, "center");
            
            JLabel emptyText = new JLabel("Không có đơn hàng đang chờ");
            emptyText.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 18));
            emptyText.setForeground(TEXT_MUTED);
            emptyPanel.add(emptyText, "center");
            
            ordersGrid.add(emptyPanel, "span, grow, center");
        } else {
            // Sort by time (oldest first)
            orders.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
            
            for (KitchenOrder order : orders) {
                if (order.getStatus() != OrderStatus.READY) {
                    JPanel card = createOrderCard(order);
                    ordersGrid.add(card, "w 280!, h 220!");
                }
            }
        }
        
        // Update stats
        long waiting = orders.stream().filter(o -> o.getStatus() == OrderStatus.WAITING).count();
        long preparing = orders.stream().filter(o -> o.getStatus() == OrderStatus.PREPARING).count();
        statsLabel.setText(waiting + " đang chờ | " + preparing + " đang làm");
        
        ordersGrid.revalidate();
        ordersGrid.repaint();
    }
    
    private JPanel createOrderCard(KitchenOrder order) {
        Color statusColor = getStatusColor(order);
        
        JPanel card = new JPanel(new MigLayout("fill, wrap, insets 12", "[grow]", "[]8[][grow][]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background
                g2d.setColor(SURFACE);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Left accent
                g2d.setColor(statusColor);
                g2d.fillRoundRect(0, 0, 6, getHeight(), 12, 12);
                g2d.fillRect(3, 0, 3, getHeight());
                
                g2d.dispose();
            }
        };
        card.setOpaque(false);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Header: Order code + Table
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[]push[]", ""));
        headerPanel.setOpaque(false);
        
        JLabel codeLabel = new JLabel(order.getOrderCode());
        codeLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        codeLabel.setForeground(TEXT_LIGHT);
        headerPanel.add(codeLabel);
        
        JLabel tableLabel = new JLabel(order.getTableName());
        tableLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        tableLabel.setForeground(statusColor);
        headerPanel.add(tableLabel);
        
        card.add(headerPanel, "growx");
        
        // Time elapsed
        long minutes = order.getMinutesElapsed();
        String timeText = minutes < 60 ? minutes + " phút trước" : (minutes / 60) + "h " + (minutes % 60) + "m";
        JLabel timeLabel = new JLabel("⏱ " + timeText);
        timeLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        timeLabel.setForeground(minutes > 15 ? STATUS_URGENT : TEXT_MUTED);
        card.add(timeLabel);
        
        // Items
        JPanel itemsPanel = new JPanel(new MigLayout("wrap, insets 0, gap 2", "[grow]", ""));
        itemsPanel.setOpaque(false);
        
        for (String item : order.getItemStrings()) {
            JLabel itemLabel = new JLabel(item);
            itemLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
            itemLabel.setForeground(item.startsWith("✓") ? STATUS_NEW : TEXT_LIGHT);
            itemsPanel.add(itemLabel);
        }
        
        card.add(itemsPanel, "grow");
        
        // Action buttons
        JPanel actionsPanel = new JPanel(new MigLayout("insets 0, gap 8", "[grow][grow]", ""));
        actionsPanel.setOpaque(false);
        
        JButton startBtn = new JButton(order.getStatus() == OrderStatus.WAITING ? "🔥 Bắt đầu" : "Đang làm...");
        startBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
        startBtn.setBackground(order.getStatus() == OrderStatus.WAITING ? STATUS_PREPARING : SURFACE);
        startBtn.setForeground(TEXT_LIGHT);
        startBtn.setBorderPainted(false);
        startBtn.setEnabled(order.getStatus() == OrderStatus.WAITING);
        startBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        startBtn.addActionListener(e -> {
            // Mark all items as being prepared
            ToastNotification.info(SwingUtilities.getWindowAncestor(this), 
                "Đang chuẩn bị: " + order.getTableName());
            refreshOrdersGrid();
        });
        actionsPanel.add(startBtn, "grow");
        
        JButton doneBtn = new JButton("✓ Xong");
        doneBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
        doneBtn.setBackground(STATUS_NEW);
        doneBtn.setForeground(TEXT_LIGHT);
        doneBtn.setBorderPainted(false);
        doneBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        doneBtn.addActionListener(e -> {
            orderManager.completeOrder(order.getId());
            ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                "Hoàn thành: " + order.getTableName());
        });
        actionsPanel.add(doneBtn, "grow");
        
        card.add(actionsPanel, "growx");
        
        return card;
    }
    
    private Color getStatusColor(KitchenOrder order) {
        long minutes = order.getMinutesElapsed();
        if (minutes > 20) return STATUS_URGENT;
        if (minutes > 10 || order.getStatus() == OrderStatus.PREPARING) return STATUS_PREPARING;
        return STATUS_NEW;
    }
    
    private void startAutoRefresh() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    orders = orderManager.getPendingOrders();
                    refreshOrdersGrid();
                });
            }
        }, 5000, 5000); // Refresh every 5 seconds
    }
    
    public void refresh() {
        loadOrders();
        ToastNotification.info(SwingUtilities.getWindowAncestor(this), "Đã làm mới");
    }
    
    public void stopTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        if (orderListener != null) {
            orderManager.removeListener(orderListener);
        }
    }
}
