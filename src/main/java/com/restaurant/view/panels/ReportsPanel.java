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
        
        // Export dropdown button
        JButton exportBtn = new JButton("📥 Xuất báo cáo ▾");
        exportBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        exportBtn.setBackground(SUCCESS);
        exportBtn.setForeground(Color.WHITE);
        exportBtn.setBorderPainted(false);
        exportBtn.setFocusPainted(false);
        exportBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        exportBtn.addActionListener(e -> showExportMenu(exportBtn));
        header.add(exportBtn);
        
        JButton refreshBtn = createButton("🔄", SURFACE, this::refresh);
        refreshBtn.setForeground(TEXT_PRIMARY);
        header.add(refreshBtn);
        
        return header;
    }
    
    private void showExportMenu(Component parent) {
        JPopupMenu menu = new JPopupMenu();
        menu.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        
        JMenuItem csvItem = new JMenuItem("📄 Xuất CSV (.csv)");
        csvItem.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        csvItem.addActionListener(e -> exportToCSV());
        menu.add(csvItem);
        
        JMenuItem excelItem = new JMenuItem("📊 Xuất Excel (.xlsx)");
        excelItem.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        excelItem.addActionListener(e -> exportToExcel());
        menu.add(excelItem);
        
        menu.addSeparator();
        
        JMenuItem printItem = new JMenuItem("🖨️ In báo cáo");
        printItem.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        printItem.addActionListener(e -> printReport());
        menu.add(printItem);
        
        menu.show(parent, 0, parent.getHeight());
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
        
        JLabel title = new JLabel("📈 Doanh thu theo ngày (Double-click để xem chi tiết)");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        title.setForeground(TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);
        
        String[] columns = {"Ngày", "Số đơn", "Doanh thu", "Giảm giá", "Thực thu"};
        revenueModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Non-editable
            }
        };
        
        revenueTable = new JTable(revenueModel);
        revenueTable.setRowHeight(40);
        revenueTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        revenueTable.getTableHeader().setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        revenueTable.setShowVerticalLines(false);
        revenueTable.setGridColor(BORDER);
        revenueTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        revenueTable.setSelectionBackground(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 50));
        revenueTable.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        
        // Double-click to show order details
        revenueTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showOrderDetails();
                }
            }
        });
        
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
    
    private void showOrderDetails() {
        int selectedRow = revenueTable.getSelectedRow();
        if (selectedRow < 0) return;
        
        // Get date from first column (format: dd/MM)
        String dateStr = (String) revenueModel.getValueAt(selectedRow, 0);
        
        // Parse date - add current year
        try {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
            java.time.MonthDay monthDay = java.time.MonthDay.parse(dateStr, fmt);
            LocalDate selectedDate = monthDay.atYear(LocalDate.now().getYear());
            
            // Handle year boundary (e.g., viewing December data in January)
            if (selectedDate.isAfter(LocalDate.now())) {
                selectedDate = monthDay.atYear(LocalDate.now().getYear() - 1);
            }
            
            // Show dialog
            com.restaurant.view.dialogs.OrderDetailDialog dialog = 
                new com.restaurant.view.dialogs.OrderDetailDialog(
                    SwingUtilities.getWindowAncestor(this), selectedDate);
            dialog.setVisible(true);
            
        } catch (Exception e) {
            logger.error("Error parsing date: {}", dateStr, e);
        }
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
    
    private void exportToCSV() {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String fromDateStr = fromDate.format(dateFmt);
        String toDateStr = toDate.format(dateFmt);
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Xuất báo cáo CSV");
        fileChooser.setSelectedFile(new File("BaoCaoDoanhThu_" + 
            fromDate.format(DateTimeFormatter.ofPattern("ddMMyyyy")) + "_" +
            toDate.format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".csv"));
        
        int result = fileChooser.showSaveDialog(SwingUtilities.getWindowAncestor(this));
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8))) {
                
                writer.print('\ufeff'); // BOM for Excel UTF-8
                
                // Header with clear formatting
                writer.println("╔══════════════════════════════════════════════════════════════╗");
                writer.println("║           BÁO CÁO DOANH THU - RESTAURANT POS                 ║");
                writer.println("╚══════════════════════════════════════════════════════════════╝");
                writer.println();
                writer.println("Ngày xuất báo cáo:," + LocalDate.now().format(dateFmt));
                writer.println("Kỳ báo cáo:," + periodFilter.getSelectedItem());
                writer.println("Từ ngày:," + fromDateStr);
                writer.println("Đến ngày:," + toDateStr);
                writer.println();
                
                // Summary stats in clear table format
                writer.println("═══════════════════════════════════════════════════════════════");
                writer.println("                     THỐNG KÊ TỔNG QUAN                        ");
                writer.println("═══════════════════════════════════════════════════════════════");
                writer.println();
                writer.println("Chỉ số,Giá trị");
                writer.println("Tổng doanh thu," + cleanCurrency(statRevenueValue.getText()));
                writer.println("Số đơn hàng," + statOrdersValue.getText());
                writer.println("Số khách hàng," + statGuestsValue.getText());
                writer.println("Trung bình/đơn," + cleanCurrency(statAvgValue.getText()));
                writer.println();
                
                // Revenue table
                writer.println("═══════════════════════════════════════════════════════════════");
                writer.println("                    DOANH THU THEO NGÀY                         ");
                writer.println("═══════════════════════════════════════════════════════════════");
                writer.println();
                writer.println("Ngày,Số đơn,Doanh thu,Giảm giá,Thực thu");
                for (int i = 0; i < revenueModel.getRowCount(); i++) {
                    String dateCol = revenueModel.getValueAt(i, 0).toString();
                    // Convert dd/MM to dd/MM/yyyy
                    String fullDate = dateCol + "/" + LocalDate.now().getYear();
                    writer.println(fullDate + "," +
                        revenueModel.getValueAt(i, 1) + "," +
                        cleanCurrency(revenueModel.getValueAt(i, 2).toString()) + "," +
                        cleanCurrency(revenueModel.getValueAt(i, 3).toString()) + "," +
                        cleanCurrency(revenueModel.getValueAt(i, 4).toString()));
                }
                writer.println();
                writer.println("TỔNG CỘNG,," + cleanCurrency(totalRevenueLabel.getText()) + ",,");
                writer.println();
                
                // Top products
                writer.println("═══════════════════════════════════════════════════════════════");
                writer.println("                    TOP MÓN BÁN CHẠY                           ");
                writer.println("═══════════════════════════════════════════════════════════════");
                writer.println();
                writer.println("Hạng,Tên món,Số lượng bán,Doanh thu");
                for (int i = 0; i < topProductsModel.getRowCount(); i++) {
                    String rank = topProductsModel.getValueAt(i, 0).toString()
                        .replace("🥇", "1").replace("🥈", "2").replace("🥉", "3");
                    writer.println(rank + "," +
                        "\"" + topProductsModel.getValueAt(i, 1) + "\"," +
                        topProductsModel.getValueAt(i, 2) + "," +
                        cleanCurrency(topProductsModel.getValueAt(i, 3).toString()));
                }
                writer.println();
                writer.println("═══════════════════════════════════════════════════════════════");
                writer.println("                          HẾT BÁO CÁO                           ");
                writer.println("═══════════════════════════════════════════════════════════════");
                
                writer.flush();
                logger.info("CSV report exported to: {}", file.getAbsolutePath());
                
                ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                    "✅ Đã xuất: " + file.getName());
                
                // Open folder
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file.getParentFile());
                }
                
            } catch (Exception e) {
                logger.error("CSV export error", e);
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Lỗi xuất CSV: " + e.getMessage());
            }
        }
    }
    
    private void exportToExcel() {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Xuất báo cáo Excel");
        fileChooser.setSelectedFile(new File("BaoCaoDoanhThu_" + 
            fromDate.format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".xlsx"));
        
        int result = fileChooser.showSaveDialog(SwingUtilities.getWindowAncestor(this));
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            if (!file.getName().toLowerCase().endsWith(".xlsx")) {
                file = new File(file.getAbsolutePath() + ".xlsx");
            }
            
            try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = 
                    new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                
                org.apache.poi.xssf.usermodel.XSSFSheet sheet = 
                    workbook.createSheet("Báo cáo doanh thu");
                
                // Create styles
                org.apache.poi.xssf.usermodel.XSSFCellStyle titleStyle = workbook.createCellStyle();
                org.apache.poi.xssf.usermodel.XSSFFont titleFont = workbook.createFont();
                titleFont.setBold(true);
                titleFont.setFontHeightInPoints((short) 18);
                titleFont.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                    new byte[]{(byte)255, (byte)255, (byte)255}, null));
                titleStyle.setFont(titleFont);
                titleStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                    new byte[]{(byte)40, (byte)167, (byte)69}, null)); // Green
                titleStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                titleStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
                
                org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = workbook.createCellStyle();
                org.apache.poi.xssf.usermodel.XSSFFont headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setFontHeightInPoints((short) 11);
                headerFont.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                    new byte[]{(byte)255, (byte)255, (byte)255}, null));
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                    new byte[]{(byte)52, (byte)58, (byte)64}, null)); // Dark gray
                headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
                headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                
                org.apache.poi.xssf.usermodel.XSSFCellStyle sectionStyle = workbook.createCellStyle();
                org.apache.poi.xssf.usermodel.XSSFFont sectionFont = workbook.createFont();
                sectionFont.setBold(true);
                sectionFont.setFontHeightInPoints((short) 12);
                sectionStyle.setFont(sectionFont);
                sectionStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                    new byte[]{(byte)230, (byte)230, (byte)230}, null)); // Light gray
                sectionStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                
                org.apache.poi.xssf.usermodel.XSSFCellStyle dataStyle = workbook.createCellStyle();
                dataStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                dataStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                dataStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                dataStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                
                org.apache.poi.xssf.usermodel.XSSFCellStyle currencyStyle = workbook.createCellStyle();
                currencyStyle.cloneStyleFrom(dataStyle);
                currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
                currencyStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT);
                
                org.apache.poi.xssf.usermodel.XSSFCellStyle totalStyle = workbook.createCellStyle();
                org.apache.poi.xssf.usermodel.XSSFFont totalFont = workbook.createFont();
                totalFont.setBold(true);
                totalStyle.setFont(totalFont);
                totalStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                    new byte[]{(byte)255, (byte)243, (byte)205}, null)); // Light yellow
                totalStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                totalStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.DOUBLE);
                
                int rowNum = 0;
                
                // Title row (merged)
                org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
                titleRow.setHeightInPoints(30);
                org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("📊 BÁO CÁO DOANH THU - RESTAURANT POS");
                titleCell.setCellStyle(titleStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));
                
                rowNum++; // Empty row
                
                // Info section
                createInfoRow(sheet, rowNum++, "Ngày xuất:", LocalDate.now().format(dateFmt), sectionStyle, dataStyle);
                createInfoRow(sheet, rowNum++, "Kỳ báo cáo:", (String)periodFilter.getSelectedItem(), sectionStyle, dataStyle);
                createInfoRow(sheet, rowNum++, "Từ ngày:", fromDate.format(dateFmt), sectionStyle, dataStyle);
                createInfoRow(sheet, rowNum++, "Đến ngày:", toDate.format(dateFmt), sectionStyle, dataStyle);
                
                rowNum++; // Empty row
                
                // Summary section
                org.apache.poi.ss.usermodel.Row summaryHeader = sheet.createRow(rowNum++);
                org.apache.poi.ss.usermodel.Cell summaryCell = summaryHeader.createCell(0);
                summaryCell.setCellValue("📈 THỐNG KÊ TỔNG QUAN");
                summaryCell.setCellStyle(sectionStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 4));
                
                createSummaryRow(sheet, rowNum++, "Tổng doanh thu", cleanCurrency(statRevenueValue.getText()), headerStyle, currencyStyle);
                createSummaryRow(sheet, rowNum++, "Số đơn hàng", statOrdersValue.getText(), headerStyle, dataStyle);
                createSummaryRow(sheet, rowNum++, "Số khách", statGuestsValue.getText(), headerStyle, dataStyle);
                createSummaryRow(sheet, rowNum++, "Trung bình/đơn", cleanCurrency(statAvgValue.getText()), headerStyle, currencyStyle);
                
                rowNum++; // Empty row
                
                // Revenue table header
                org.apache.poi.ss.usermodel.Row revHeader = sheet.createRow(rowNum++);
                org.apache.poi.ss.usermodel.Cell revCell = revHeader.createCell(0);
                revCell.setCellValue("📅 DOANH THU THEO NGÀY");
                revCell.setCellStyle(sectionStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 4));
                
                org.apache.poi.ss.usermodel.Row revColHeader = sheet.createRow(rowNum++);
                String[] revColumns = {"Ngày", "Số đơn", "Doanh thu", "Giảm giá", "Thực thu"};
                for (int i = 0; i < revColumns.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = revColHeader.createCell(i);
                    cell.setCellValue(revColumns[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                // Revenue data
                for (int i = 0; i < revenueModel.getRowCount(); i++) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(revenueModel.getValueAt(i, 0) + "/" + LocalDate.now().getYear());
                    row.getCell(0).setCellStyle(dataStyle);
                    
                    row.createCell(1).setCellValue(Integer.parseInt(revenueModel.getValueAt(i, 1).toString()));
                    row.getCell(1).setCellStyle(dataStyle);
                    
                    for (int j = 2; j < 5; j++) {
                        org.apache.poi.ss.usermodel.Cell cell = row.createCell(j);
                        try {
                            cell.setCellValue(Long.parseLong(cleanCurrency(revenueModel.getValueAt(i, j).toString())));
                        } catch (NumberFormatException e) {
                            cell.setCellValue(0);
                        }
                        cell.setCellStyle(currencyStyle);
                    }
                }
                
                // Total row
                org.apache.poi.ss.usermodel.Row totalRow = sheet.createRow(rowNum++);
                totalRow.createCell(0).setCellValue("TỔNG CỘNG");
                totalRow.getCell(0).setCellStyle(totalStyle);
                totalRow.createCell(4).setCellValue(Long.parseLong(cleanCurrency(totalRevenueLabel.getText())));
                totalRow.getCell(4).setCellStyle(totalStyle);
                
                rowNum++; // Empty row
                
                // Top products
                org.apache.poi.ss.usermodel.Row topHeader = sheet.createRow(rowNum++);
                org.apache.poi.ss.usermodel.Cell topCell = topHeader.createCell(0);
                topCell.setCellValue("🏆 TOP MÓN BÁN CHẠY");
                topCell.setCellStyle(sectionStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 3));
                
                org.apache.poi.ss.usermodel.Row topColHeader = sheet.createRow(rowNum++);
                String[] topColumns = {"Hạng", "Tên món", "Số lượng", "Doanh thu"};
                for (int i = 0; i < topColumns.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = topColHeader.createCell(i);
                    cell.setCellValue(topColumns[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                for (int i = 0; i < topProductsModel.getRowCount(); i++) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                    String rank = topProductsModel.getValueAt(i, 0).toString()
                        .replace("🥇", "1").replace("🥈", "2").replace("🥉", "3");
                    row.createCell(0).setCellValue(rank);
                    row.getCell(0).setCellStyle(dataStyle);
                    row.createCell(1).setCellValue(topProductsModel.getValueAt(i, 1).toString());
                    row.getCell(1).setCellStyle(dataStyle);
                    row.createCell(2).setCellValue(Integer.parseInt(topProductsModel.getValueAt(i, 2).toString()));
                    row.getCell(2).setCellStyle(dataStyle);
                    try {
                        row.createCell(3).setCellValue(Long.parseLong(cleanCurrency(topProductsModel.getValueAt(i, 3).toString())));
                    } catch (NumberFormatException e) {
                        row.createCell(3).setCellValue(0);
                    }
                    row.getCell(3).setCellStyle(currencyStyle);
                }
                
                // Auto-size columns
                for (int i = 0; i < 5; i++) {
                    sheet.autoSizeColumn(i);
                    sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 10000));
                }
                
                // Write to file
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    workbook.write(fos);
                }
                
                logger.info("Excel report exported to: {}", file.getAbsolutePath());
                
                ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                    "✅ Đã xuất Excel: " + file.getName());
                
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
                
            } catch (Exception e) {
                logger.error("Excel export error", e);
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Lỗi xuất Excel: " + e.getMessage());
            }
        }
    }
    
    private void createInfoRow(org.apache.poi.xssf.usermodel.XSSFSheet sheet, int rowNum, 
            String label, String value, 
            org.apache.poi.xssf.usermodel.XSSFCellStyle labelStyle, 
            org.apache.poi.xssf.usermodel.XSSFCellStyle valueStyle) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum);
        org.apache.poi.ss.usermodel.Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        org.apache.poi.ss.usermodel.Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
    }
    
    private void createSummaryRow(org.apache.poi.xssf.usermodel.XSSFSheet sheet, int rowNum,
            String label, String value,
            org.apache.poi.xssf.usermodel.XSSFCellStyle labelStyle,
            org.apache.poi.xssf.usermodel.XSSFCellStyle valueStyle) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum);
        org.apache.poi.ss.usermodel.Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        org.apache.poi.ss.usermodel.Cell valueCell = row.createCell(1);
        try {
            valueCell.setCellValue(Long.parseLong(value));
        } catch (NumberFormatException e) {
            valueCell.setCellValue(value);
        }
        valueCell.setCellStyle(valueStyle);
    }
    
    private void printReport() {
        ToastNotification.info(SwingUtilities.getWindowAncestor(this), 
            "🖨️ Chức năng in đang phát triển...");
        // TODO: Implement print functionality using java.awt.print
    }
    
    private String cleanCurrency(String value) {
        return value.replace(",", "").replace(" ₫", "").replace("₫", "").trim();
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
