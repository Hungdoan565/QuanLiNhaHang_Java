package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.dao.impl.OrderDAOImpl;
import com.restaurant.dao.interfaces.IOrderDAO;
import com.restaurant.model.OrderDetail.ItemStatus;
import com.restaurant.model.User;
import com.restaurant.util.KitchenOrderManager;
import com.restaurant.util.KitchenOrderManager.KitchenOrder;
import com.restaurant.util.KitchenOrderManager.OrderItem;
import com.restaurant.util.KitchenOrderManager.OrderStatus;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Waiter Panel - Order Pickup Interface
 * 
 * Purpose: Show orders ready for pickup from kitchen
 * Users: WAITER role only
 * 
 * Features:
 * - Display orders with READY status from kitchen
 * - Real-time notifications when kitchen calls
 * - Mark orders as picked up / served
 * - Auto-refresh every 5 seconds
 */
public class WaiterPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(WaiterPanel.class);
    
    // Dark theme colors
    private static final Color BACKGROUND = Color.decode("#0D1117");
    private static final Color SURFACE = Color.decode("#161B22");
    private static final Color CARD_BG = Color.decode("#21262D");
    private static final Color TEXT_WHITE = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(255, 255, 255, 160);
    private static final Color BORDER = Color.decode("#30363D");
    
    // Status colors
    private static final Color READY_COLOR = Color.decode("#22C55E");   // Green
    private static final Color COOKING_COLOR = Color.decode("#F97316"); // Orange
    private static final Color URGENT_COLOR = Color.decode("#EF4444");  // Red
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    
    // Fonts
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_CARD_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    
    private final User currentUser;
    private final KitchenOrderManager orderManager;
    private final IOrderDAO orderDAO = new OrderDAOImpl();
    
    private JPanel ordersGrid;
    private JLabel statsLabel;
    private JLabel emptyLabel;
    private Timer refreshTimer;
    private Consumer<List<KitchenOrder>> orderListener;
    private int lastReadyCount = 0;
    
    public WaiterPanel(User user) {
        this.currentUser = user;
        this.orderManager = KitchenOrderManager.getInstance();
        logger.info("WaiterPanel: Initializing for user {}", user.getUsername());
        initializeUI();
        setupOrderListener();
        refresh();
        startAutoRefresh();
        logger.info("WaiterPanel: Initialization complete");
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        
        // Header
        add(createHeader(), BorderLayout.NORTH);
        
        // Main content - orders grid
        JPanel content = createContentArea();
        add(content, BorderLayout.CENTER);
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 16 24, fill", "[][grow][][]", "[center]"));
        header.setBackground(SURFACE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        
        // Icon + Title
        JLabel icon = new JLabel("🍽️");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        header.add(icon);
        
        JLabel title = new JLabel("Lấy món từ bếp");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_WHITE);
        header.add(title, "gapleft 12");
        
        // Stats
        statsLabel = new JLabel("0 món cần lấy");
        statsLabel.setFont(FONT_HEADER);
        statsLabel.setForeground(TEXT_MUTED);
        header.add(statsLabel, "gapleft 32");
        
        // Spacer
        header.add(new JLabel(), "growx");
        
        // Refresh button
        JButton refreshBtn = new JButton("🔄 Làm mới");
        refreshBtn.setFont(FONT_BODY);
        refreshBtn.setForeground(TEXT_WHITE);
        refreshBtn.setBackground(CARD_BG);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        refreshBtn.addActionListener(e -> refresh());
        header.add(refreshBtn);
        
        return header;
    }
    
    private JPanel createContentArea() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        
        // Orders grid
        ordersGrid = new JPanel(new MigLayout("wrap 2, gap 14 14", "[grow,fill][grow,fill]", ""));
        ordersGrid.setOpaque(false);
        
        JScrollPane scrollPane = new JScrollPane(ordersGrid);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Empty state (initially hidden)
        emptyLabel = new JLabel();
        emptyLabel.setVisible(false);
        
        return panel;
    }
    
    private void setupOrderListener() {
        orderListener = updatedOrders -> {
            SwingUtilities.invokeLater(() -> {
                refreshOrdersGrid();
                
                // Beep if new ready orders appeared
                long readyCount = updatedOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.READY)
                    .count();
                
                if (readyCount > lastReadyCount && lastReadyCount >= 0) {
                    Toolkit.getDefaultToolkit().beep();
                    ToastNotification.info(SwingUtilities.getWindowAncestor(this),
                        "🔔 Có món mới cần lấy!");
                }
                lastReadyCount = (int) readyCount;
            });
        };
        orderManager.addListener(orderListener);
    }
    
    private void refreshOrdersGrid() {
        ordersGrid.removeAll();
        
        // Get orders with READY status
        List<KitchenOrder> allOrders = orderManager.getPendingOrders();
        List<KitchenOrder> readyOrders = new ArrayList<>();
        
        for (KitchenOrder order : allOrders) {
            // Show orders that have at least one READY item
            boolean hasReadyItems = order.getItems().stream().anyMatch(OrderItem::isReady);
            if (hasReadyItems || order.getStatus() == OrderStatus.READY) {
                readyOrders.add(order);
            }
        }
        
        // Update stats
        int totalReadyItems = 0;
        for (KitchenOrder order : readyOrders) {
            totalReadyItems += (int) order.getItems().stream().filter(OrderItem::isReady).count();
        }
        statsLabel.setText(totalReadyItems + " món cần lấy | " + readyOrders.size() + " bàn");
        
        if (readyOrders.isEmpty()) {
            // Show empty state
            ordersGrid.add(createEmptyState(), "span 2, center, gaptop 60");
        } else {
            // Add order cards
            for (KitchenOrder order : readyOrders) {
                JPanel card = createOrderCard(order);
                ordersGrid.add(card, "grow");
            }
        }
        
        ordersGrid.revalidate();
        ordersGrid.repaint();
    }
    
    private JPanel createEmptyState() {
        JPanel panel = new JPanel(new MigLayout("wrap, insets 40", "[center]", "[center]"));
        panel.setOpaque(false);
        
        JLabel icon = new JLabel("☕");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        panel.add(icon);
        
        JLabel text = new JLabel("Chưa có món nào sẵn sàng");
        text.setFont(FONT_HEADER);
        text.setForeground(TEXT_MUTED);
        panel.add(text, "gaptop 16");
        
        JLabel subtext = new JLabel("Bếp sẽ gọi khi có món cần lấy");
        subtext.setFont(FONT_BODY);
        subtext.setForeground(TEXT_MUTED);
        panel.add(subtext, "gaptop 8");
        
        return panel;
    }
    
    private JPanel createOrderCard(KitchenOrder order) {
        long minutes = order.getMinutesElapsed();
        boolean isUrgent = minutes > 10;
        Color accentColor = isUrgent ? URGENT_COLOR : READY_COLOR;
        
        JPanel card = new JPanel(new MigLayout("wrap, insets 10, gap 6", "[grow]", "")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background
                g2d.setColor(CARD_BG);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Top accent bar
                g2d.setColor(accentColor);
                g2d.fillRoundRect(0, 0, getWidth(), 3, 10, 10);
                
                // Urgent glow
                if (isUrgent) {
                    g2d.setColor(new Color(239, 68, 68, 20));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }
                
                g2d.dispose();
            }
        };
        card.setOpaque(false);
        card.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
        
        // Header: Table name + Time
        JPanel headerRow = new JPanel(new MigLayout("insets 0", "[]push[]", "[center]"));
        headerRow.setOpaque(false);
        
        JLabel tableLabel = new JLabel("🪑 " + order.getTableName());
        tableLabel.setFont(FONT_HEADER);
        tableLabel.setForeground(TEXT_WHITE);
        headerRow.add(tableLabel);
        
        String timeStr = minutes < 60 ? minutes + " phút" : (minutes/60) + "h" + (minutes%60) + "'";
        JLabel timeLabel = new JLabel("⏱ " + timeStr);
        timeLabel.setFont(FONT_SMALL);
        timeLabel.setForeground(isUrgent ? URGENT_COLOR : TEXT_MUTED);
        headerRow.add(timeLabel);
        
        card.add(headerRow, "growx");
        
        // Order code
        JLabel codeLabel = new JLabel(order.getOrderCode());
        codeLabel.setFont(FONT_SMALL);
        codeLabel.setForeground(TEXT_MUTED);
        card.add(codeLabel);
        
        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 30));
        card.add(sep, "growx, gaptop 8, gapbottom 8");
        
        // Ready items list
        JPanel itemsPanel = new JPanel(new MigLayout("wrap, insets 0, gap 4", "[grow]", ""));
        itemsPanel.setOpaque(false);
        
        int readyItemCount = 0;
        for (OrderItem item : order.getItems()) {
            if (item.isReady()) {
                readyItemCount++;
                JPanel itemRow = new JPanel(new MigLayout("insets 0, gap 8", "[][grow]", ""));
                itemRow.setOpaque(false);
                
                JLabel checkIcon = new JLabel("✅");
                checkIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
                itemRow.add(checkIcon);
                
                JLabel itemLabel = new JLabel(item.getQuantity() + "x " + item.getName());
                itemLabel.setFont(FONT_BODY);
                itemLabel.setForeground(TEXT_WHITE);
                itemRow.add(itemLabel, "growx");
                
                itemsPanel.add(itemRow, "growx");
            }
        }
        
        // Show pending items count if any
        long pendingCount = order.getItems().stream().filter(i -> !i.isReady()).count();
        if (pendingCount > 0) {
            JLabel pendingLabel = new JLabel("⏳ Còn " + pendingCount + " món đang làm");
            pendingLabel.setFont(FONT_SMALL);
            pendingLabel.setForeground(COOKING_COLOR);
            itemsPanel.add(pendingLabel, "gaptop 4");
        }
        
        card.add(itemsPanel, "growx");
        
        // Action buttons
        JPanel actionPanel = new JPanel(new MigLayout("insets 0, gap 8", "[grow][grow]", ""));
        actionPanel.setOpaque(false);
        
        // Picked up button
        JButton pickupBtn = new JButton("✓ Đã lấy (" + readyItemCount + ")");
        pickupBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pickupBtn.setBackground(READY_COLOR);
        pickupBtn.setForeground(Color.WHITE);
        pickupBtn.setBorderPainted(false);
        pickupBtn.setFocusPainted(false);
        pickupBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pickupBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        pickupBtn.addActionListener(e -> markAsPickedUp(order));
        actionPanel.add(pickupBtn, "grow, h 34!");
        
        // Served button (marks as fully served)
        JButton servedBtn = new JButton("✓ Đã phục vụ");
        servedBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        servedBtn.setBackground(PRIMARY);
        servedBtn.setForeground(Color.WHITE);
        servedBtn.setBorderPainted(false);
        servedBtn.setFocusPainted(false);
        servedBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        servedBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        servedBtn.addActionListener(e -> markAsServed(order));
        actionPanel.add(servedBtn, "grow, h 34!");
        
        card.add(actionPanel, "growx, gaptop 12");
        
        return card;
    }
    
    private void markAsPickedUp(KitchenOrder order) {
        // Mark all READY items as SERVED in database
        int updatedCount = 0;
        for (OrderItem item : order.getItems()) {
            if (item.isReady() && item.getOrderDetailId() > 0) {
                boolean success = orderDAO.updateOrderDetailStatus(
                    item.getOrderDetailId(), ItemStatus.SERVED);
                if (success) updatedCount++;
            }
        }
        
        logger.info("Waiter picked up {} items from order {}", updatedCount, order.getOrderCode());
        
        // Remove from kitchen manager
        orderManager.completeOrder(order.getId());
        
        ToastNotification.success(SwingUtilities.getWindowAncestor(this),
            "Đã lấy " + updatedCount + " món - " + order.getTableName());
        
        refresh();
    }
    
    private void markAsServed(KitchenOrder order) {
        // Mark ALL items as SERVED (including any still cooking)
        int updatedCount = 0;
        for (OrderItem item : order.getItems()) {
            if (item.getOrderDetailId() > 0) {
                boolean success = orderDAO.updateOrderDetailStatus(
                    item.getOrderDetailId(), ItemStatus.SERVED);
                if (success) updatedCount++;
            }
        }
        
        logger.info("Waiter marked order {} as fully served ({} items)", 
            order.getOrderCode(), updatedCount);
        
        // Remove from kitchen manager
        orderManager.completeOrder(order.getId());
        orderManager.markServed(order.getTableName());
        
        ToastNotification.success(SwingUtilities.getWindowAncestor(this),
            "Đã phục vụ xong - " + order.getTableName());
        
        refresh();
    }
    
    public void refresh() {
        orderManager.loadFromDatabase();
        refreshOrdersGrid();
    }
    
    private void startAutoRefresh() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> refresh());
            }
        }, 5000, 5000); // Refresh every 5 seconds
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
