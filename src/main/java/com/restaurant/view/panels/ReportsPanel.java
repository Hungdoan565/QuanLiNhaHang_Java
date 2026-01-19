package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.User;
import com.restaurant.service.ReportService;
import com.restaurant.service.ReportService.*;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reports Panel - Báo cáo doanh thu và thống kê
 * 
 * - Kết nối database thật qua ReportService
 * - Fallback demo data nếu chưa có dữ liệu
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
    private final ReportService reportService;
    
    private JComboBox<String> periodFilter;
    private JTable revenueTable;
    private DefaultTableModel revenueModel;
    private JTable topProductsTable;
    private DefaultTableModel topProductsModel;
    private JLabel totalRevenueLabel;
    
    // Stats card labels
    private JLabel statRevenueValue;
    private JLabel statOrdersValue;
    private JLabel statGuestsValue;
    private JLabel statAvgValue;
    
    // Date range
    private LocalDate fromDate;
    private LocalDate toDate;
    
    public ReportsPanel(User user) {
        this.currentUser = user;
        this.reportService = ReportService.getInstance();
        logger.info("ReportsPanel constructor started");
        initializeUI();
        loadReportData();
        logger.info("ReportsPanel constructor completed");
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        
        add(createHeader(), BorderLayout.NORTH);
        
        JPanel mainContent = new JPanel(new MigLayout("fill, insets 0, gap 16", "[60%][40%]", "[grow]"));
        mainContent.setOpaque(false);
        
        mainContent.add(createRevenueSection(), "grow");
        mainContent.add(createRightPanel(), "grow");
        
        add(mainContent, BorderLayout.CENTER);
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0", "[]16[]push[]8[]", ""));
        header.setOpaque(false);
        
        JLabel periodLabel = new JLabel("Thời gian:");
        periodLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        periodLabel.setForeground(TEXT_PRIMARY);
        header.add(periodLabel);
        
        periodFilter = new JComboBox<>(new String[]{"Hôm nay", "7 ngày qua", "30 ngày qua", "Tháng này"});
        periodFilter.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        periodFilter.addActionListener(e -> loadReportData());
        header.add(periodFilter);
        
        JButton exportBtn = createButton("📊 Xuất CSV", SUCCESS, this::exportReport);
        header.add(exportBtn);
        
        JButton refreshBtn = createButton("🔄", SURFACE, this::refresh);
        refreshBtn.setForeground(TEXT_PRIMARY);
        header.add(refreshBtn);
        
        return header;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, wrap, insets 0, gap 12", "[grow]", "[][grow]"));
        panel.setOpaque(false);
        
        panel.add(createStatsCards(), "growx");
        panel.add(createTopProductsSection(), "grow");
        
        return panel;
    }
    
    private JPanel createStatsCards() {
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 8", "[grow][grow]", "[grow][grow]"));
        panel.setOpaque(false);
        
        // Create stat cards and store references
        JPanel revenueCard = createStatCard("💰", "Tổng doanh thu");
        statRevenueValue = (JLabel) ((JPanel)revenueCard).getComponent(1);
        panel.add(revenueCard, "grow");
        
        JPanel ordersCard = createStatCard("🧾", "Số đơn hàng");
        statOrdersValue = (JLabel) ((JPanel)ordersCard).getComponent(1);
        panel.add(ordersCard, "grow");
        
        JPanel guestsCard = createStatCard("👥", "Khách hàng");
        statGuestsValue = (JLabel) ((JPanel)guestsCard).getComponent(1);
        panel.add(guestsCard, "grow");
        
        JPanel avgCard = createStatCard("🍽️", "TB/Đơn");
        statAvgValue = (JLabel) ((JPanel)avgCard).getComponent(1);
        panel.add(avgCard, "grow");
        
        return panel;
    }
    
    private JPanel createStatCard(String icon, String title) {
        JPanel card = new JPanel(new MigLayout("wrap, insets 12", "[grow]", ""));
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        
        JLabel iconLabel = new JLabel(icon + " " + title);
        iconLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 11));
        iconLabel.setForeground(TEXT_SECONDARY);
        card.add(iconLabel);
        
        JLabel valueLabel = new JLabel("--");
        valueLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        valueLabel.setForeground(TEXT_PRIMARY);
        card.add(valueLabel);
        
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
        
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(SURFACE);
        tableContainer.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        tableContainer.add(new JScrollPane(revenueTable), BorderLayout.CENTER);
        
        // Summary
        JPanel summary = new JPanel(new MigLayout("insets 12", "[]push[]", ""));
        summary.setBackground(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 30));
        
        JLabel totalLabel = new JLabel("💰 Tổng cộng:");
        totalLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        totalLabel.setForeground(TEXT_PRIMARY);
        summary.add(totalLabel);
        
        totalRevenueLabel = new JLabel("0 ₫");
        totalRevenueLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        totalRevenueLabel.setForeground(PRIMARY);
        summary.add(totalRevenueLabel);
        
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
        topProductsTable.getColumnModel().getColumn(2).setPreferredWidth(40);
        topProductsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(SURFACE);
        tableContainer.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        tableContainer.add(new JScrollPane(topProductsTable), BorderLayout.CENTER);
        
        panel.add(tableContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ========== Data Loading ==========
    
    private void loadReportData() {
        calculateDateRange();
        
        // Always load real data from database
        loadRealData();
    }
    
    private void calculateDateRange() {
        String period = (String) periodFilter.getSelectedItem();
        toDate = LocalDate.now();
        
        switch (period) {
            case "Hôm nay" -> fromDate = toDate;
            case "7 ngày qua" -> fromDate = toDate.minusDays(6);
            case "30 ngày qua" -> fromDate = toDate.minusDays(29);
            case "Tháng này" -> fromDate = toDate.withDayOfMonth(1);
            default -> fromDate = toDate.minusDays(6);
        }
        
        logger.debug("Date range: {} to {}", fromDate, toDate);
    }
    
    private void loadRealData() {
        try {
            // Load summary stats - ALWAYS show real data
            ReportSummary summary = reportService.getSummary(fromDate, toDate);
            
            // Update stats cards with real values (even if 0)
            statRevenueValue.setText(formatCurrency(summary.totalRevenue()));
            statOrdersValue.setText(String.valueOf(summary.totalOrders()));
            statGuestsValue.setText(String.valueOf(summary.totalGuests()));
            statAvgValue.setText(formatCurrency(summary.avgPerOrder()));
            
            // Load daily revenue
            List<DailyRevenue> dailyData = reportService.getDailyRevenue(fromDate, toDate);
            revenueModel.setRowCount(0);
            BigDecimal total = BigDecimal.ZERO;
            
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
            if (dailyData.isEmpty()) {
                // Show "no data" row for today if empty
                revenueModel.addRow(new Object[]{
                    toDate.format(fmt),
                    "0",
                    "0 ₫",
                    "0 ₫",
                    "0 ₫"
                });
            } else {
                for (DailyRevenue dr : dailyData) {
                    revenueModel.addRow(new Object[]{
                        dr.date().format(fmt),
                        dr.orderCount(),
                        formatCurrency(dr.grossRevenue()),
                        formatCurrency(dr.discount()),
                        formatCurrency(dr.netRevenue())
                    });
                    total = total.add(dr.netRevenue());
                }
            }
            totalRevenueLabel.setText(formatCurrency(total));
            
            // Load top products
            List<TopProduct> topProducts = reportService.getTopProducts(fromDate, toDate, 10);
            topProductsModel.setRowCount(0);
            
            if (topProducts.isEmpty()) {
                topProductsModel.addRow(new Object[]{
                    "-", "Chưa có dữ liệu", "-", "-"
                });
            } else {
                for (TopProduct tp : topProducts) {
                    String medal = switch (tp.rank()) {
                        case 1 -> "🥇";
                        case 2 -> "🥈";
                        case 3 -> "🥉";
                        default -> String.valueOf(tp.rank());
                    };
                    
                    topProductsModel.addRow(new Object[]{
                        medal,
                        tp.name(),
                        tp.quantity(),
                        formatCurrency(tp.revenue())
                    });
                }
            }
            
            logger.info("Loaded real data: {} orders, {} revenue for {} to {}", 
                summary.totalOrders(), summary.totalRevenue(), fromDate, toDate);
            
        } catch (Exception e) {
            logger.error("Error loading real data", e);
            // Show error state, not demo data
            statRevenueValue.setText("Lỗi");
            statOrdersValue.setText("--");
            statGuestsValue.setText("--");
            statAvgValue.setText("--");
        }
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return String.format("%,d ₫", amount.longValue());
    }
    
    private void exportReport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu báo cáo");
        fileChooser.setSelectedFile(new File("BaoCaoDoanhThu_" + 
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv"));
        
        int result = fileChooser.showSaveDialog(SwingUtilities.getWindowAncestor(this));
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8))) {
                
                writer.print('\ufeff'); // BOM for Excel
                writer.println("BÁO CÁO DOANH THU - RESTAURANT POS");
                writer.println("Ngày xuất:," + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                writer.println("Kỳ báo cáo:," + periodFilter.getSelectedItem());
                writer.println("Từ ngày:," + fromDate + ",Đến ngày:," + toDate);
                writer.println();
                
                // Stats
                writer.println("THỐNG KÊ TỔNG QUAN");
                writer.println("Tổng doanh thu:," + statRevenueValue.getText().replace(",", ""));
                writer.println("Số đơn hàng:," + statOrdersValue.getText());
                writer.println("Khách hàng:," + statGuestsValue.getText());
                writer.println("TB/Đơn:," + statAvgValue.getText().replace(",", ""));
                writer.println();
                
                // Revenue table
                writer.println("DOANH THU THEO NGÀY");
                writer.println("Ngày,Số đơn,Doanh thu,Giảm giá,Thực thu");
                for (int i = 0; i < revenueModel.getRowCount(); i++) {
                    StringBuilder row = new StringBuilder();
                    for (int j = 0; j < revenueModel.getColumnCount(); j++) {
                        if (j > 0) row.append(",");
                        String val = revenueModel.getValueAt(i, j).toString();
                        row.append(val.replace(",", "").replace(" ₫", ""));
                    }
                    writer.println(row);
                }
                writer.println();
                writer.println("Tổng cộng:," + totalRevenueLabel.getText().replace(",", "").replace(" ₫", ""));
                writer.println();
                
                // Top products
                writer.println("TOP MÓN BÁN CHẠY");
                writer.println("#,Tên món,Số lượng,Doanh thu");
                for (int i = 0; i < topProductsModel.getRowCount(); i++) {
                    StringBuilder row = new StringBuilder();
                    for (int j = 0; j < topProductsModel.getColumnCount(); j++) {
                        if (j > 0) row.append(",");
                        String val = topProductsModel.getValueAt(i, j).toString();
                        row.append(val.replace(",", "").replace(" ₫", ""));
                    }
                    writer.println(row);
                }
                
                writer.flush();
                logger.info("Report exported to: {}", file.getAbsolutePath());
                
                ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                    "Đã xuất: " + file.getName());
                
                // Open folder
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file.getParentFile());
                }
                
            } catch (Exception e) {
                logger.error("Export error", e);
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Lỗi: " + e.getMessage());
            }
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
