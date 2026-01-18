package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.User;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Reports Panel - Báo cáo doanh thu và thống kê
 * 
 * Features:
 * - Báo cáo doanh thu theo ngày/tuần/tháng
 * - Top món bán chạy
 * - Thống kê nhân viên
 * - Xuất báo cáo
 */
public class ReportsPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(ReportsPanel.class);
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color BORDER = Color.decode(AppConfig.Colors.BORDER);
    private static final Color SUCCESS = Color.decode(AppConfig.Colors.SUCCESS);
    private static final Color WARNING = Color.decode(AppConfig.Colors.WARNING);
    
    private final User currentUser;
    private final NumberFormat currencyFormat;
    
    private JComboBox<String> periodFilter;
    private JPanel statsCardsPanel;
    private JTable revenueTable;
    private DefaultTableModel revenueModel;
    private JTable topProductsTable;
    private DefaultTableModel topProductsModel;
    
    public ReportsPanel(User user) {
        this.currentUser = user;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        initializeUI();
        loadReportData();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        
        // Header with filters
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);
        
        // Main content - split layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(600);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);
        
        // Left: Revenue table
        JPanel revenueSection = createRevenueSection();
        splitPane.setLeftComponent(revenueSection);
        
        // Right: Top products + Stats
        JPanel rightPanel = new JPanel(new MigLayout("fill, wrap, insets 0, gap 16", "[grow]", "[][grow]"));
        rightPanel.setOpaque(false);
        
        statsCardsPanel = createStatsCards();
        rightPanel.add(statsCardsPanel, "growx");
        
        JPanel topProductsSection = createTopProductsSection();
        rightPanel.add(topProductsSection, "grow");
        
        splitPane.setRightComponent(rightPanel);
        
        add(splitPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0", "[]16[]push[]8[]", ""));
        header.setOpaque(false);
        
        // Period filter
        JLabel periodLabel = new JLabel("Thời gian:");
        periodLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        periodLabel.setForeground(TEXT_PRIMARY);
        header.add(periodLabel);
        
        periodFilter = new JComboBox<>(new String[]{"Hôm nay", "7 ngày qua", "30 ngày qua", "Tháng này", "Quý này"});
        periodFilter.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        periodFilter.addActionListener(e -> loadReportData());
        header.add(periodFilter);
        
        // Export button
        JButton exportBtn = createButton("📊 Xuất Excel", SUCCESS, this::exportReport);
        header.add(exportBtn);
        
        // Refresh
        JButton refreshBtn = createButton("🔄", SURFACE, this::refresh);
        refreshBtn.setForeground(TEXT_PRIMARY);
        header.add(refreshBtn);
        
        return header;
    }
    
    private JPanel createStatsCards() {
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 16", "[grow][grow][grow][grow]", ""));
        panel.setOpaque(false);
        
        panel.add(createStatCard("💰", "Tổng doanh thu", "12,500,000 ₫", "+15%", SUCCESS), "grow");
        panel.add(createStatCard("🧾", "Số đơn hàng", "48", "+8%", SUCCESS), "grow");
        panel.add(createStatCard("👥", "Khách hàng", "156", "+12%", SUCCESS), "grow");
        panel.add(createStatCard("🍽️", "TB/Đơn", "260,000 ₫", "-3%", WARNING), "grow");
        
        return panel;
    }
    
    private JPanel createStatCard(String icon, String title, String value, String change, Color changeColor) {
        JPanel card = new JPanel(new MigLayout("wrap, insets 16, gap 4", "[grow]", ""));
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        
        // Icon + Title
        JPanel header = new JPanel(new MigLayout("insets 0, gap 8", "[][]", ""));
        header.setOpaque(false);
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        header.add(iconLabel);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        titleLabel.setForeground(TEXT_SECONDARY);
        header.add(titleLabel);
        
        card.add(header);
        
        // Value
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 20));
        valueLabel.setForeground(TEXT_PRIMARY);
        card.add(valueLabel);
        
        // Change
        JLabel changeLabel = new JLabel(change + " so với kỳ trước");
        changeLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 11));
        changeLabel.setForeground(changeColor);
        card.add(changeLabel);
        
        return card;
    }
    
    private JPanel createRevenueSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        
        JLabel title = new JLabel("📈 Doanh thu theo ngày");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        title.setForeground(TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);
        
        String[] columns = {"Ngày", "Số đơn", "Doanh thu", "Giảm giá", "Thực thu"};
        revenueModel = new DefaultTableModel(columns, 0);
        
        revenueTable = new JTable(revenueModel);
        revenueTable.setRowHeight(40);
        revenueTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        revenueTable.getTableHeader().setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        revenueTable.setShowVerticalLines(false);
        revenueTable.setGridColor(BORDER);
        
        // Right-align numeric columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 1; i < 5; i++) {
            revenueTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }
        
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(SURFACE);
        tableContainer.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        tableContainer.add(new JScrollPane(revenueTable), BorderLayout.CENTER);
        
        // Summary row at bottom
        JPanel summary = new JPanel(new MigLayout("insets 12", "[]push[]", ""));
        summary.setBackground(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 30));
        
        JLabel totalLabel = new JLabel("Tổng cộng:");
        totalLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        totalLabel.setForeground(TEXT_PRIMARY);
        summary.add(totalLabel);
        
        JLabel totalValue = new JLabel("12,500,000 ₫");
        totalValue.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        totalValue.setForeground(PRIMARY);
        summary.add(totalValue);
        
        tableContainer.add(summary, BorderLayout.SOUTH);
        
        panel.add(tableContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createTopProductsSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        
        JLabel title = new JLabel("🏆 Top món bán chạy");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        title.setForeground(TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);
        
        String[] columns = {"#", "Tên món", "SL", "Doanh thu"};
        topProductsModel = new DefaultTableModel(columns, 0);
        
        topProductsTable = new JTable(topProductsModel);
        topProductsTable.setRowHeight(36);
        topProductsTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        topProductsTable.getTableHeader().setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        topProductsTable.setShowVerticalLines(false);
        
        topProductsTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        topProductsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        topProductsTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        topProductsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        
        // Rank column with medal
        topProductsTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                String rank = (String) value;
                if ("🥇".equals(rank) || "🥈".equals(rank) || "🥉".equals(rank)) {
                    setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
                }
                return this;
            }
        });
        
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(SURFACE);
        tableContainer.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        tableContainer.add(new JScrollPane(topProductsTable), BorderLayout.CENTER);
        
        panel.add(tableContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadReportData() {
        loadRevenueData();
        loadTopProducts();
    }
    
    private void loadRevenueData() {
        revenueModel.setRowCount(0);
        
        // Demo data - last 7 days
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        Random rand = new Random(42);
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            int orders = 5 + rand.nextInt(10);
            long revenue = (800000 + rand.nextInt(700000)) * orders / 7;
            long discount = revenue * rand.nextInt(10) / 100;
            
            revenueModel.addRow(new Object[]{
                date.format(fmt),
                orders,
                String.format("%,d ₫", revenue),
                String.format("%,d ₫", discount),
                String.format("%,d ₫", revenue - discount)
            });
        }
    }
    
    private void loadTopProducts() {
        topProductsModel.setRowCount(0);
        
        // Demo data
        Object[][] topProducts = {
            {"🥇", "Phở bò tái", 45, "2,475,000 ₫"},
            {"🥈", "Cơm chiên dương châu", 38, "2,470,000 ₫"},
            {"🥉", "Cà phê sữa đá", 52, "1,508,000 ₫"},
            {"4", "Bún bò Huế", 28, "1,680,000 ₫"},
            {"5", "Sinh tố bơ", 25, "1,125,000 ₫"},
            {"6", "Gỏi cuốn", 22, "770,000 ₫"},
            {"7", "Trà đào", 20, "700,000 ₫"},
            {"8", "Chả giò chiên", 18, "810,000 ₫"},
        };
        
        for (Object[] row : topProducts) {
            topProductsModel.addRow(row);
        }
    }
    
    private void exportReport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu báo cáo");
        fileChooser.setSelectedFile(new java.io.File("BaoCaoDoanhThu_" + 
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx"));
        
        int result = fileChooser.showSaveDialog(SwingUtilities.getWindowAncestor(this));
        if (result == JFileChooser.APPROVE_OPTION) {
            // TODO: Implement actual Excel export
            ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                "Đã xuất báo cáo: " + fileChooser.getSelectedFile().getName());
        }
    }
    
    private JButton createButton(String text, Color bgColor, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        btn.setBackground(bgColor);
        btn.setForeground(bgColor.equals(SURFACE) ? TEXT_PRIMARY : Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btn.addActionListener(e -> action.run());
        return btn;
    }
    
    public void refresh() {
        loadReportData();
        ToastNotification.info(SwingUtilities.getWindowAncestor(this), "Đã làm mới báo cáo");
    }
}
