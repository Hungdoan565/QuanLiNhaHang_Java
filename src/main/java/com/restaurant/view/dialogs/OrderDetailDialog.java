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
    
    private final LocalDate date;
    private final ReportService reportService;
    
    public OrderDetailDialog(Window parent, LocalDate date) {
        super(parent, "Chi tiết đơn hàng", ModalityType.APPLICATION_MODAL);
        this.date = date;
        this.reportService = ReportService.getInstance();
        
        initializeUI();
        loadData();
        
        setSize(650, 700);
        setLocationRelativeTo(parent);
    }
    
    private void initializeUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 16));
        mainPanel.setBackground(BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        mainPanel.add(createHeader(), BorderLayout.NORTH);
        
        // Scrollable content
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        
        JScrollPane scrollPane = new JScrollPane(createOrdersPanel());
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        contentWrapper.add(scrollPane, BorderLayout.CENTER);
        
        mainPanel.add(contentWrapper, BorderLayout.CENTER);
        
        // Footer
        mainPanel.add(createFooter(), BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0", "[]push[]", ""));
        header.setOpaque(false);
        
        String dateStr = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        JLabel titleLabel = new JLabel("📋 Chi tiết ngày " + dateStr);
        titleLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 20));
        titleLabel.setForeground(TEXT_PRIMARY);
        header.add(titleLabel);
        
        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        closeBtn.setBackground(SURFACE);
        closeBtn.setForeground(TEXT_SECONDARY);
        closeBtn.setBorderPainted(false);
        closeBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        closeBtn.addActionListener(e -> dispose());
        header.add(closeBtn);
        
        return header;
    }
    
    private JPanel ordersContainer;
    
    private JPanel createOrdersPanel() {
        ordersContainer = new JPanel(new MigLayout("wrap, fillx, insets 0, gap 12", "[grow]", ""));
        ordersContainer.setOpaque(false);
        return ordersContainer;
    }
    
    private void loadData() {
        List<OrderWithDetails> orders = reportService.getOrdersByDate(date);
        ordersContainer.removeAll();
        
        if (orders.isEmpty()) {
            JLabel emptyLabel = new JLabel("Không có đơn hàng nào trong ngày này");
            emptyLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.ITALIC, 14));
            emptyLabel.setForeground(TEXT_SECONDARY);
            ordersContainer.add(emptyLabel, "center");
        } else {
            for (OrderWithDetails order : orders) {
                ordersContainer.add(createOrderCard(order), "growx");
            }
        }
        
        ordersContainer.revalidate();
        ordersContainer.repaint();
    }
    
    private JPanel createOrderCard(OrderWithDetails order) {
        JPanel card = new JPanel(new MigLayout("wrap, fillx, insets 12, gap 8", "[grow]", ""));
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        
        // Order header
        JPanel orderHeader = new JPanel(new MigLayout("insets 0", "[]8[]push[]", ""));
        orderHeader.setOpaque(false);
        
        String time = order.completedAt().format(DateTimeFormatter.ofPattern("HH:mm"));
        JLabel codeLabel = new JLabel("🧾 " + order.orderCode());
        codeLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        codeLabel.setForeground(PRIMARY);
        orderHeader.add(codeLabel);
        
        JLabel tableLabel = new JLabel("| " + order.tableName() + " | " + time);
        tableLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        tableLabel.setForeground(TEXT_SECONDARY);
        orderHeader.add(tableLabel);
        
        JLabel totalLabel = new JLabel(formatCurrency(order.totalAmount()));
        totalLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 15));
        totalLabel.setForeground(SUCCESS);
        orderHeader.add(totalLabel);
        
        card.add(orderHeader, "growx");
        
        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        card.add(sep, "growx, gaptop 4, gapbottom 4");
        
        // Order items
        for (OrderItemDetail item : order.items()) {
            JPanel itemRow = new JPanel(new MigLayout("insets 0", "[]push[]", ""));
            itemRow.setOpaque(false);
            
            JLabel nameLabel = new JLabel("• " + item.productName() + " x" + item.quantity());
            nameLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
            nameLabel.setForeground(TEXT_PRIMARY);
            itemRow.add(nameLabel);
            
            JLabel priceLabel = new JLabel(formatCurrency(item.subtotal()));
            priceLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
            priceLabel.setForeground(TEXT_SECONDARY);
            itemRow.add(priceLabel);
            
            card.add(itemRow, "growx");
        }
        
        return card;
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
        closeBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        
        return footer;
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return String.format("%,d ₫", amount.longValue());
    }
}
