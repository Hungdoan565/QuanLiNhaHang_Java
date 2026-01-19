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
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Kitchen Display System (KDS) - Professional Kanban-style Layout
 * 
 * Features:
 * - 3 Column Kanban: CHỜ LÀM → ĐANG NẤU → SẴN SÀNG
 * - Real-time updates from POS
 * - Color-coded urgency (green → yellow → red)
 * - Per-item completion checkboxes
 * - Full-screen mode for wall TVs
 * - Auto-refresh every 5 seconds
 */
public class KitchenPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(KitchenPanel.class);
    
    // Dark theme colors for kitchen display
    private static final Color BACKGROUND = Color.decode("#0D1117");
    private static final Color SURFACE = Color.decode("#161B22");
    private static final Color CARD_BG = Color.decode("#21262D");
    private static final Color TEXT_WHITE = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(255, 255, 255, 160);
    
    // Status colors
    private static final Color COL_WAITING = Color.decode("#F59E0B");   // Amber
    private static final Color COL_COOKING = Color.decode("#F97316");   // Orange
    private static final Color COL_READY = Color.decode("#22C55E");     // Green
    private static final Color STATUS_URGENT = Color.decode("#EF4444"); // Red
    
    // Fonts
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_CARD_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_TIMER = new Font("Segoe UI", Font.BOLD, 24);
    
    private final User currentUser;
    private final KitchenOrderManager orderManager;
    private List<KitchenOrder> orders = new ArrayList<>();
    
    // UI Components
    private JPanel waitingColumn;
    private JPanel cookingColumn;
    private JPanel readyColumn;
    private JLabel statsLabel;
    private JLabel avgTimeLabel;
    private Timer refreshTimer;
    private Consumer<List<KitchenOrder>> orderListener;
    
    // Counters for each column
    private int waitingCount = 0;
    private int cookingCount = 0;
    private int readyCount = 0;
    
    public KitchenPanel(User user) {
        this.currentUser = user;
        this.orderManager = KitchenOrderManager.getInstance();
        initializeUI();
        setupOrderListener();
        loadOrders();
        startAutoRefresh();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        
        // Header bar
        add(createHeaderBar(), BorderLayout.NORTH);
        
        // Main content - 3 Kanban columns
        JPanel columnsPanel = createKanbanColumns();
        JScrollPane scrollPane = new JScrollPane(columnsPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderBar() {
        JPanel header = new JPanel(new MigLayout("insets 16 24, fill", "[][grow][][]", "[center]"));
        header.setBackground(SURFACE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255,255,255,20)));
        
        // Title with icon
        JLabel titleIcon = new JLabel("🍳");
        titleIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        header.add(titleIcon);
        
        JLabel title = new JLabel("Màn Hình Bếp");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_WHITE);
        header.add(title, "gapleft 8");
        
        // Stats panel
        JPanel statsPanel = new JPanel(new MigLayout("insets 0, gap 24", "[][][]", ""));
        statsPanel.setOpaque(false);
        
        statsLabel = new JLabel("0 đơn");
        statsLabel.setFont(FONT_HEADER);
        statsLabel.setForeground(TEXT_MUTED);
        statsPanel.add(statsLabel);
        
        avgTimeLabel = new JLabel("~ 0 phút");
        avgTimeLabel.setFont(FONT_HEADER);
        avgTimeLabel.setForeground(TEXT_MUTED);
        statsPanel.add(avgTimeLabel);
        
        header.add(statsPanel, "gapleft 32");
        
        // Spacer
        header.add(new JLabel(), "growx");
        
        // Refresh button
        JButton refreshBtn = createHeaderButton("🔄 Làm mới", e -> refresh());
        header.add(refreshBtn);
        
        // Full screen button
        JButton fullscreenBtn = createHeaderButton("⛶ Toàn màn hình", e -> toggleFullscreen());
        header.add(fullscreenBtn, "gapleft 8");
        
        return header;
    }
    
    private JButton createHeaderButton(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY);
        btn.setForeground(TEXT_WHITE);
        btn.setBackground(CARD_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btn.addActionListener(action);
        return btn;
    }
    
    private JPanel createKanbanColumns() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 16, gap 16", "[grow,33%][grow,33%][grow,33%]", "[grow]"));
        panel.setOpaque(false);
        
        // Column 1: CHỜ LÀM (Waiting)
        JPanel col1 = createColumn("🆕 CHỜ LÀM", COL_WAITING);
        waitingColumn = (JPanel) ((JScrollPane) col1.getComponent(1)).getViewport().getView();
        panel.add(col1, "grow");
        
        // Column 2: ĐANG NẤU (Cooking)
        JPanel col2 = createColumn("🔥 ĐANG NẤU", COL_COOKING);
        cookingColumn = (JPanel) ((JScrollPane) col2.getComponent(1)).getViewport().getView();
        panel.add(col2, "grow");
        
        // Column 3: SẴN SÀNG (Ready)
        JPanel col3 = createColumn("✅ SẴN SÀNG", COL_READY);
        readyColumn = (JPanel) ((JScrollPane) col3.getComponent(1)).getViewport().getView();
        panel.add(col3, "grow");
        
        return panel;
    }
    
    private JPanel createColumn(String title, Color headerColor) {
        JPanel column = new JPanel(new MigLayout("fill, wrap, insets 0, gap 0", "[grow]", "[][grow]"));
        column.setOpaque(false);
        
        // Column header
        JPanel header = new JPanel(new MigLayout("insets 12 16", "[]push[]", "[center]"));
        header.setBackground(headerColor);
        header.putClientProperty(FlatClientProperties.STYLE, "arc: 12 12 0 0");
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel);
        
        JLabel countLabel = new JLabel("0");
        countLabel.setFont(FONT_HEADER);
        countLabel.setForeground(new Color(255, 255, 255, 200));
        countLabel.setName("countLabel");
        header.add(countLabel);
        
        column.add(header, "growx, h 48!");
        
        // Cards container
        JPanel cardsContainer = new JPanel(new MigLayout("wrap, insets 8, gap 8", "[grow]", ""));
        cardsContainer.setBackground(SURFACE);
        
        JScrollPane scroll = new JScrollPane(cardsContainer);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        column.add(scroll, "grow");
        
        return column;
    }
    
    private void setupOrderListener() {
        orderListener = updatedOrders -> {
            SwingUtilities.invokeLater(() -> {
                this.orders = new ArrayList<>(updatedOrders);
                refreshColumns();
                
                // Beep for new orders
                if (!orders.isEmpty()) {
                    Toolkit.getDefaultToolkit().beep();
                }
            });
        };
        orderManager.addListener(orderListener);
    }
    
    private void loadOrders() {
        orders = orderManager.getPendingOrders();
        refreshColumns();
    }
    
    private void refreshColumns() {
        waitingColumn.removeAll();
        cookingColumn.removeAll();
        readyColumn.removeAll();
        
        waitingCount = 0;
        cookingCount = 0;
        readyCount = 0;
        
        long totalTime = 0;
        int orderCount = 0;
        
        // Sort orders by time (oldest first)
        orders.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
        
        for (KitchenOrder order : orders) {
            totalTime += order.getMinutesElapsed();
            orderCount++;
            
            JPanel card = createOrderCard(order);
            
            switch (order.getStatus()) {
                case WAITING -> {
                    waitingColumn.add(card, "growx");
                    waitingCount++;
                }
                case PREPARING -> {
                    cookingColumn.add(card, "growx");
                    cookingCount++;
                }
                case READY -> {
                    readyColumn.add(card, "growx");
                    readyCount++;
                }
            }
        }
        
        // Add empty state if no cards
        if (waitingCount == 0) addEmptyState(waitingColumn, "Không có đơn chờ");
        if (cookingCount == 0) addEmptyState(cookingColumn, "Không có đơn đang nấu");
        if (readyCount == 0) addEmptyState(readyColumn, "Không có đơn sẵn sàng");
        
        // Update column counts
        updateColumnCount(waitingColumn.getParent().getParent(), waitingCount);
        updateColumnCount(cookingColumn.getParent().getParent(), cookingCount);
        updateColumnCount(readyColumn.getParent().getParent(), readyCount);
        
        // Update header stats
        int total = waitingCount + cookingCount + readyCount;
        statsLabel.setText(total + " đơn đang xử lý");
        avgTimeLabel.setText("~ " + (orderCount > 0 ? (totalTime / orderCount) : 0) + " phút/đơn");
        
        waitingColumn.revalidate();
        waitingColumn.repaint();
        cookingColumn.revalidate();
        cookingColumn.repaint();
        readyColumn.revalidate();
        readyColumn.repaint();
    }
    
    private void updateColumnCount(Container column, int count) {
        if (column instanceof JPanel panel) {
            for (Component c : panel.getComponents()) {
                if (c instanceof JPanel header) {
                    for (Component h : header.getComponents()) {
                        if (h instanceof JLabel label && "countLabel".equals(label.getName())) {
                            label.setText(String.valueOf(count));
                        }
                    }
                }
            }
        }
    }
    
    private void addEmptyState(JPanel column, String message) {
        JPanel empty = new JPanel(new MigLayout("wrap, insets 24", "[center]", "[center]"));
        empty.setOpaque(false);
        
        JLabel icon = new JLabel("☕");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        icon.setForeground(TEXT_MUTED);
        empty.add(icon);
        
        JLabel text = new JLabel(message);
        text.setFont(FONT_BODY);
        text.setForeground(TEXT_MUTED);
        empty.add(text);
        
        column.add(empty, "growx");
    }
    
    private JPanel createOrderCard(KitchenOrder order) {
        long minutes = order.getMinutesElapsed();
        boolean isUrgent = minutes > 15;
        Color accentColor = isUrgent ? STATUS_URGENT : getStatusColor(order.getStatus());
        
        JPanel card = new JPanel(new MigLayout("wrap, insets 12, gap 6", "[grow]", "")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background
                g2d.setColor(CARD_BG);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Left accent bar
                g2d.setColor(accentColor);
                g2d.fillRoundRect(0, 0, 5, getHeight(), 12, 12);
                g2d.fillRect(3, 0, 2, getHeight());
                
                // Urgent glow effect
                if (isUrgent) {
                    g2d.setColor(new Color(239, 68, 68, 30));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }
                
                g2d.dispose();
            }
        };
        card.setOpaque(false);
        
        // Header row: Table name + Timer
        JPanel headerRow = new JPanel(new MigLayout("insets 0", "[]push[]", "[center]"));
        headerRow.setOpaque(false);
        
        JLabel tableLabel = new JLabel(order.getTableName());
        tableLabel.setFont(FONT_CARD_TITLE);
        tableLabel.setForeground(TEXT_WHITE);
        headerRow.add(tableLabel);
        
        // Timer with icon
        String timeStr = minutes < 60 ? minutes + "'" : (minutes/60) + "h" + (minutes%60) + "'";
        JLabel timerLabel = new JLabel("⏱ " + timeStr);
        timerLabel.setFont(FONT_TIMER);
        timerLabel.setForeground(isUrgent ? STATUS_URGENT : (minutes > 10 ? COL_WAITING : TEXT_MUTED));
        headerRow.add(timerLabel);
        
        card.add(headerRow, "growx");
        
        // Order code
        JLabel codeLabel = new JLabel(order.getOrderCode());
        codeLabel.setFont(FONT_BODY);
        codeLabel.setForeground(TEXT_MUTED);
        card.add(codeLabel);
        
        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255,255,255,30));
        card.add(sep, "growx, gaptop 4, gapbottom 4");
        
        // Items list with checkboxes
        JPanel itemsPanel = new JPanel(new MigLayout("wrap, insets 0, gap 4", "[grow]", ""));
        itemsPanel.setOpaque(false);
        
        for (String itemStr : order.getItemStrings()) {
            boolean isDone = itemStr.startsWith("✓");
            String displayText = isDone ? itemStr.substring(1).trim() : itemStr;
            
            JPanel itemRow = new JPanel(new MigLayout("insets 0, gap 8", "[][grow]", ""));
            itemRow.setOpaque(false);
            
            JCheckBox check = new JCheckBox();
            check.setSelected(isDone);
            check.setOpaque(false);
            check.setFocusPainted(false);
            check.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            check.addActionListener(e -> {
                // Toggle item status
                // In real implementation, this would update the item in the database
                logger.debug("Item toggled: {}", displayText);
            });
            itemRow.add(check);
            
            JLabel itemLabel = new JLabel(displayText);
            itemLabel.setFont(FONT_BODY);
            itemLabel.setForeground(isDone ? TEXT_MUTED : TEXT_WHITE);
            if (isDone) {
                itemLabel.setText("<html><s>" + displayText + "</s></html>");
            }
            itemRow.add(itemLabel, "growx");
            
            itemsPanel.add(itemRow, "growx");
        }
        
        card.add(itemsPanel, "growx");
        
        // Action buttons based on status
        JPanel actionsPanel = new JPanel(new MigLayout("insets 0, gap 8", "[grow][grow]", ""));
        actionsPanel.setOpaque(false);
        
        if (order.getStatus() == OrderStatus.WAITING) {
            JButton startBtn = createActionButton("🔥 Bắt đầu nấu", COL_COOKING);
            startBtn.addActionListener(e -> {
                order.setStatus(OrderStatus.PREPARING);
                refreshColumns();
                ToastNotification.info(SwingUtilities.getWindowAncestor(this), 
                    "Bắt đầu nấu: " + order.getTableName());
            });
            actionsPanel.add(startBtn, "grow, span 2, h 40!");
        } else if (order.getStatus() == OrderStatus.PREPARING) {
            JButton doneBtn = createActionButton("✅ Hoàn thành", COL_READY);
            doneBtn.addActionListener(e -> {
                order.setStatus(OrderStatus.READY);
                refreshColumns();
                ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                    "Sẵn sàng: " + order.getTableName());
                Toolkit.getDefaultToolkit().beep(); // Alert waiter
            });
            actionsPanel.add(doneBtn, "grow, span 2, h 40!");
        } else if (order.getStatus() == OrderStatus.READY) {
            JButton ringBtn = createActionButton("🔔 Gọi phục vụ", COL_READY);
            ringBtn.addActionListener(e -> {
                Toolkit.getDefaultToolkit().beep();
                ToastNotification.info(SwingUtilities.getWindowAncestor(this), 
                    "Đã gọi phục vụ: " + order.getTableName());
            });
            actionsPanel.add(ringBtn, "grow, h 40!");
            
            JButton clearBtn = createActionButton("✓ Đã lấy", CARD_BG);
            clearBtn.addActionListener(e -> {
                orderManager.completeOrder(order.getId());
                refreshColumns();
            });
            actionsPanel.add(clearBtn, "grow, h 40!");
        }
        
        card.add(actionsPanel, "growx, gaptop 8");
        
        return card;
    }
    
    private JButton createActionButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(TEXT_WHITE);
        btn.setBackground(bgColor);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        return btn;
    }
    
    private Color getStatusColor(OrderStatus status) {
        return switch (status) {
            case WAITING -> COL_WAITING;
            case PREPARING -> COL_COOKING;
            case READY -> COL_READY;
            default -> TEXT_MUTED;
        };
    }
    
    private void toggleFullscreen() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame frame) {
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            
            if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH && 
                frame.isUndecorated()) {
                // Exit fullscreen
                frame.dispose();
                frame.setUndecorated(false);
                frame.setExtendedState(JFrame.NORMAL);
                frame.setVisible(true);
            } else {
                // Enter fullscreen
                frame.dispose();
                frame.setUndecorated(true);
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.setVisible(true);
                
                // ESC to exit
                getRootPane().registerKeyboardAction(
                    e -> toggleFullscreen(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW
                );
            }
        }
    }
    
    private void startAutoRefresh() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    orders = orderManager.getPendingOrders();
                    refreshColumns();
                });
            }
        }, 5000, 5000);
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
