package com.restaurant.view.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.service.ReportService;
import com.restaurant.service.ReportService.OrderWithDetails;
import com.restaurant.service.ReportService.OrderItemDetail;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dialog hiển thị chi tiết các đơn hàng trong một ngày
 * 
 * UX Features:
 * - Vertical-only scroll (no horizontal overflow)
 * - Summary statistics at top
 * - Collapsible order cards
 * - Max item name length to prevent overflow
 * - Responsive layout
 */
public class OrderDetailDialog extends JDialog {
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color BORDER = Color.decode(AppConfig.Colors.BORDER);
    private static final Color SUCCESS = Color.decode(AppConfig.Colors.SUCCESS);
    private static final Color WARNING = Color.decode(AppConfig.Colors.WARNING);
    
    private static final int MAX_PRODUCT_NAME_LENGTH = 25;
    
    private final LocalDate date;
    private final ReportService reportService;
    private List<OrderWithDetails> orders;
    
    public OrderDetailDialog(Window parent, LocalDate date) {
        super(parent, "Chi tiết đơn hàng", ModalityType.APPLICATION_MODAL);
        this.date = date;
        this.reportService = ReportService.getInstance();
        
        // Load data first
        orders = reportService.getOrdersByDate(date);
        
        initializeUI();
        
        // Size based on content but capped
        int height = Math.min(750, 250 + orders.size() * 120);
        setSize(600, height);
        setMinimumSize(new Dimension(500, 400));
        setLocationRelativeTo(parent);
    }
    
    private void initializeUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        
        // Header with close button
        mainPanel.add(createHeader(), BorderLayout.NORTH);
        
        // Content area
        JPanel contentArea = new JPanel(new BorderLayout(0, 12));
        contentArea.setOpaque(false);
        
        // Summary stats bar
        contentArea.add(createSummaryBar(), BorderLayout.NORTH);
        
        // Scrollable orders list
        JScrollPane scrollPane = createScrollableOrdersList();
        contentArea.add(scrollPane, BorderLayout.CENTER);
        
        mainPanel.add(contentArea, BorderLayout.CENTER);
        
        // Footer
        mainPanel.add(createFooter(), BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0", "[]push[]", ""));
        header.setOpaque(false);
        
        String dateStr = date.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", 
            new java.util.Locale("vi", "VN")));
        JLabel titleLabel = new JLabel("📋 " + dateStr);
        titleLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        titleLabel.setForeground(TEXT_PRIMARY);
        header.add(titleLabel);
        
        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        closeBtn.setBackground(SURFACE);
        closeBtn.setForeground(TEXT_SECONDARY);
        closeBtn.setBorderPainted(false);
        closeBtn.setPreferredSize(new Dimension(32, 32));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        closeBtn.addActionListener(e -> dispose());
        header.add(closeBtn);
        
        return header;
    }
    
    private JPanel createSummaryBar() {
        JPanel bar = new JPanel(new MigLayout("insets 10 12", "[]push[]push[]", ""));
        bar.setBackground(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 30));
        bar.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        bar.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        
        // Total orders
        JPanel ordersPanel = createStatMini("🧾", orders.size() + " đơn");
        bar.add(ordersPanel);
        
        // Total items
        int totalItems = orders.stream()
            .mapToInt(o -> o.items().stream().mapToInt(OrderItemDetail::quantity).sum())
            .sum();
        JPanel itemsPanel = createStatMini("📦", totalItems + " món");
        bar.add(itemsPanel);
        
        // Total revenue
        BigDecimal totalRevenue = orders.stream()
            .map(OrderWithDetails::totalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        JPanel revenuePanel = createStatMini("💰", formatCurrency(totalRevenue));
        bar.add(revenuePanel);
        
        return bar;
    }
    
    private JPanel createStatMini(String icon, String value) {
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 4", "[][]", ""));
        panel.setOpaque(false);
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        panel.add(iconLabel);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        valueLabel.setForeground(TEXT_PRIMARY);
        panel.add(valueLabel);
        
        return panel;
    }
    
    private JScrollPane createScrollableOrdersList() {
        JPanel ordersContainer = new JPanel();
        ordersContainer.setLayout(new BoxLayout(ordersContainer, BoxLayout.Y_AXIS));
        ordersContainer.setOpaque(false);
        ordersContainer.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        
        if (orders.isEmpty()) {
            JPanel emptyPanel = new JPanel(new MigLayout("fill", "[center]", "[center]"));
            emptyPanel.setOpaque(false);
            emptyPanel.setPreferredSize(new Dimension(400, 150));
            
            JLabel emptyIcon = new JLabel("📭");
            emptyIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
            emptyPanel.add(emptyIcon, "wrap");
            
            JLabel emptyLabel = new JLabel("Không có đơn hàng trong ngày này");
            emptyLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.ITALIC, 14));
            emptyLabel.setForeground(TEXT_SECONDARY);
            emptyPanel.add(emptyLabel);
            
            ordersContainer.add(emptyPanel);
        } else {
            for (int i = 0; i < orders.size(); i++) {
                JPanel card = createOrderCard(orders.get(i), i + 1);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                ordersContainer.add(card);
                
                // Add spacing between cards
                if (i < orders.size() - 1) {
                    ordersContainer.add(Box.createRigidArea(new Dimension(0, 10)));
                }
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(ordersContainer);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(BACKGROUND);
        scrollPane.getViewport().setBackground(BACKGROUND);
        
        return scrollPane;
    }
    
    private JPanel createOrderCard(OrderWithDetails order, int index) {
        JPanel card = new JPanel(new MigLayout("wrap, fillx, insets 10 12 10 12, gap 6", "[grow]", ""));
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height + 500));
        
        // Order header row
        JPanel headerRow = new JPanel(new MigLayout("insets 0, gap 6", "[][]push[]", ""));
        headerRow.setOpaque(false);
        
        // Index badge
        JLabel indexBadge = new JLabel(String.valueOf(index));
        indexBadge.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
        indexBadge.setForeground(Color.WHITE);
        indexBadge.setOpaque(true);
        indexBadge.setBackground(PRIMARY);
        indexBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        indexBadge.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        headerRow.add(indexBadge);
        
        // Order code + table + time
        String time = order.completedAt().format(DateTimeFormatter.ofPattern("HH:mm"));
        String tableName = order.tableName() != null ? order.tableName() : "N/A";
        JLabel infoLabel = new JLabel(order.orderCode() + " • " + tableName + " • " + time);
        infoLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        infoLabel.setForeground(TEXT_PRIMARY);
        headerRow.add(infoLabel);
        
        // Total
        JLabel totalLabel = new JLabel(formatCurrency(order.totalAmount()));
        totalLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        totalLabel.setForeground(SUCCESS);
        headerRow.add(totalLabel);
        
        card.add(headerRow, "growx");
        
        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        card.add(sep, "growx, gaptop 4, gapbottom 4");
        
        // Items list - compact view
        JPanel itemsPanel = new JPanel(new MigLayout("wrap, insets 0, gap 2", "[grow]", ""));
        itemsPanel.setOpaque(false);
        
        for (OrderItemDetail item : order.items()) {
            JPanel itemRow = createItemRow(item);
            itemsPanel.add(itemRow, "growx");
        }
        
        card.add(itemsPanel, "growx");
        
        return card;
    }
    
    private JPanel createItemRow(OrderItemDetail item) {
        JPanel row = new JPanel(new MigLayout("insets 0", "[grow][]", ""));
        row.setOpaque(false);
        
        // Truncate long product names
        String name = item.productName();
        if (name.length() > MAX_PRODUCT_NAME_LENGTH) {
            name = name.substring(0, MAX_PRODUCT_NAME_LENGTH - 2) + "...";
        }
        
        JLabel nameLabel = new JLabel("• " + name + " ×" + item.quantity());
        nameLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        nameLabel.setForeground(TEXT_PRIMARY);
        row.add(nameLabel, "growx");
        
        JLabel priceLabel = new JLabel(formatCurrency(item.subtotal()));
        priceLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        priceLabel.setForeground(TEXT_SECONDARY);
        row.add(priceLabel);
        
        return row;
    }
    
    private JPanel createFooter() {
        JPanel footer = new JPanel(new MigLayout("insets 8 0 0 0", "push[]", ""));
        footer.setOpaque(false);
        
        JButton closeBtn = new JButton("Đóng");
        closeBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        closeBtn.setBackground(PRIMARY);
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBorderPainted(false);
        closeBtn.setPreferredSize(new Dimension(100, 36));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        closeBtn.addActionListener(e -> dispose());
        
        // ESC key to close
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        footer.add(closeBtn);
        
        return footer;
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return String.format("%,d ₫", amount.longValue());
    }
}
