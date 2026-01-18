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
 * - Top món bán chạy với progress bar
 * - Thống kê nhân viên
 * - Xuất báo cáo CSV
 * - Data thật từ database + demo fallback
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
    private static final Color ERROR = Color.decode(AppConfig.Colors.ERROR);
    
    // Gradient colors for table header
    private static final Color HEADER_START = new Color(45, 55, 72);
    private static final Color HEADER_END = new Color(74, 85, 104);
    
    private final User currentUser;
    private final NumberFormat currencyFormat;
    private final ReportService reportService;
    
    private JComboBox<String> periodFilter;
    private JPanel statsCardsPanel;
    private JTable revenueTable;
    private DefaultTableModel revenueModel;
    private JTable topProductsTable;
    private DefaultTableModel topProductsModel;
    private JLabel totalRevenueLabel;
    
    // Current period data
    private LocalDate fromDate;
    private LocalDate toDate;
    private int maxQuantity = 1; // For progress bar calculation
    
    public ReportsPanel(User user) {
        this.currentUser = user;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        this.reportService = ReportService.getInstance();
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
        splitPane.setDividerLocation(620);
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
        JButton exportBtn = createButton("📊 Xuất CSV", SUCCESS, this::exportReport);
        header.add(exportBtn);
        
        // Refresh
        JButton refreshBtn = createButton("🔄", SURFACE, this::refresh);
        refreshBtn.setForeground(TEXT_PRIMARY);
        header.add(refreshBtn);
        
        return header;
    }
    
    private JPanel createStatsCards() {
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 12", "[grow][grow][grow][grow]", ""));
        panel.setOpaque(false);
        
        // Placeholder cards - will be updated with real data
        panel.add(createStatCard("💰", "Tổng doanh thu", "0 ₫", "+0%", SUCCESS, "totalRevenue"), "grow");
        panel.add(createStatCard("🧾", "Số đơn hàng", "0", "+0%", SUCCESS, "totalOrders"), "grow");
        panel.add(createStatCard("👥", "Khách hàng", "0", "+0%", SUCCESS, "totalGuests"), "grow");
        panel.add(createStatCard("🍽️", "TB/Đơn", "0 ₫", "+0%", WARNING, "avgPerOrder"), "grow");
        
        return panel;
    }
    
    private JPanel createStatCard(String icon, String title, String value, String change, Color changeColor, String cardId) {
        JPanel card = new JPanel(new MigLayout("wrap, insets 20, gap 6", "[grow]", "")) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Subtle gradient background
                GradientPaint gp = new GradientPaint(0, 0, SURFACE, getWidth(), getHeight(), 
                    new Color(SURFACE.getRed() + 5, SURFACE.getGreen() + 5, SURFACE.getBlue() + 8));
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2d.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 16");
        card.setName(cardId);
        
        // Icon + Title row
        JPanel header = new JPanel(new MigLayout("insets 0, gap 10", "[][grow]", ""));
        header.setOpaque(false);
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        header.add(iconLabel);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        titleLabel.setForeground(TEXT_SECONDARY);
        header.add(titleLabel);
        
        card.add(header, "growx");
        
        // Value - larger font
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 26));
        valueLabel.setForeground(TEXT_PRIMARY);
        valueLabel.setName("value");
        card.add(valueLabel);
        
        // Change indicator
        JLabel changeLabel = new JLabel(change + " so với kỳ trước");
        changeLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 11));
        changeLabel.setForeground(changeColor);
        changeLabel.setName("change");
        card.add(changeLabel);
        
        return card;
    }
    
    private JPanel createRevenueSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        
        JLabel title = new JLabel("📈 Doanh thu theo ngày");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);
        
        String[] columns = {"Ngày", "Số đơn", "Doanh thu", "Giảm giá", "Thực thu"};
        revenueModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        revenueTable = new JTable(revenueModel);
        revenueTable.setRowHeight(44);
        revenueTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        revenueTable.setShowVerticalLines(false);
        revenueTable.setGridColor(new Color(BORDER.getRed(), BORDER.getGreen(), BORDER.getBlue(), 80));
        revenueTable.setSelectionBackground(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 50));
        revenueTable.setIntercellSpacing(new Dimension(0, 1));
        
        // Custom header with gradient
        JTableHeader tableHeader = revenueTable.getTableHeader();
        tableHeader.setDefaultRenderer(new GradientHeaderRenderer());
        tableHeader.setPreferredSize(new Dimension(tableHeader.getPreferredSize().width, 40));
        
        // Row striping and hover effect
        revenueTable.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
        
        // Right-align numeric columns
        DefaultTableCellRenderer rightRenderer = new StripedTableCellRenderer();
        ((StripedTableCellRenderer)rightRenderer).setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 1; i < 5; i++) {
            revenueTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }
        
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(SURFACE);
        tableContainer.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        tableContainer.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        
        JScrollPane scrollPane = new JScrollPane(revenueTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(SURFACE);
        tableContainer.add(scrollPane, BorderLayout.CENTER);
        
        // Summary row at bottom
        JPanel summary = new JPanel(new MigLayout("insets 16", "[]push[]", "")) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, 
                    new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 40),
                    getWidth(), 0, 
                    new Color(SUCCESS.getRed(), SUCCESS.getGreen(), SUCCESS.getBlue(), 30));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        summary.setOpaque(false);
        
        JLabel totalLabel = new JLabel("💰 Tổng cộng:");
        totalLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 15));
        totalLabel.setForeground(TEXT_PRIMARY);
        summary.add(totalLabel);
        
        totalRevenueLabel = new JLabel("0 ₫");
        totalRevenueLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 20));
        totalRevenueLabel.setForeground(PRIMARY);
        summary.add(totalRevenueLabel);
        
        tableContainer.add(summary, BorderLayout.SOUTH);
        
        panel.add(tableContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createTopProductsSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        
        JLabel title = new JLabel("🏆 Top món bán chạy");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);
        
        String[] columns = {"#", "Tên món", "SL", "Doanh thu"};
        topProductsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        topProductsTable = new JTable(topProductsModel);
        topProductsTable.setRowHeight(42);
        topProductsTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        topProductsTable.setShowVerticalLines(false);
        topProductsTable.setGridColor(new Color(BORDER.getRed(), BORDER.getGreen(), BORDER.getBlue(), 80));
        topProductsTable.setIntercellSpacing(new Dimension(0, 1));
        
        // Custom header
        topProductsTable.getTableHeader().setDefaultRenderer(new GradientHeaderRenderer());
        topProductsTable.getTableHeader().setPreferredSize(new Dimension(0, 40));
        
        topProductsTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        topProductsTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        topProductsTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        topProductsTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        
        // Rank column with medal
        topProductsTable.getColumnModel().getColumn(0).setCellRenderer(new MedalRenderer());
        
        // Name column with progress bar
        topProductsTable.getColumnModel().getColumn(1).setCellRenderer(new ProgressNameRenderer());
        
        // Striped renderer for other columns
        topProductsTable.getColumnModel().getColumn(2).setCellRenderer(new StripedTableCellRenderer());
        topProductsTable.getColumnModel().getColumn(3).setCellRenderer(new StripedTableCellRenderer());
        
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(SURFACE);
        tableContainer.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        tableContainer.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        
        JScrollPane scrollPane = new JScrollPane(topProductsTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(SURFACE);
        tableContainer.add(scrollPane, BorderLayout.CENTER);
        
        panel.add(tableContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ========== Data Loading ==========
    
    private void loadReportData() {
        calculateDateRange();
        
        // Try to load real data
        boolean hasRealData = loadRealData();
        
        // If no real data, show demo data
        if (!hasRealData) {
            loadDemoData();
        }
    }
    
    private void calculateDateRange() {
        String period = (String) periodFilter.getSelectedItem();
        toDate = LocalDate.now();
        
        fromDate = switch (period) {
            case "Hôm nay" -> toDate;
            case "7 ngày qua" -> toDate.minusDays(6);
            case "30 ngày qua" -> toDate.minusDays(29);
            case "Tháng này" -> toDate.withDayOfMonth(1);
            case "Quý này" -> {
                int quarter = (toDate.getMonthValue() - 1) / 3;
                yield toDate.withMonth(quarter * 3 + 1).withDayOfMonth(1);
            }
            default -> toDate.minusDays(6);
        };
    }
    
    private boolean loadRealData() {
        try {
            // Load summary
            ReportSummary summary = reportService.getSummary(fromDate, toDate);
            ReportSummary prevSummary = reportService.getPreviousPeriodSummary(fromDate, toDate);
            
            if (summary.totalOrders() == 0) {
                logger.info("No real data found for period {} to {}", fromDate, toDate);
                return false;
            }
            
            // Update stats cards
            updateStatsCards(summary, prevSummary);
            
            // Load daily revenue
            List<DailyRevenue> dailyRevenue = reportService.getDailyRevenue(fromDate, toDate);
            loadRevenueTable(dailyRevenue);
            
            // Load top products
            List<TopProduct> topProducts = reportService.getTopProducts(fromDate, toDate, 10);
            loadTopProductsTable(topProducts);
            
            logger.info("Loaded real data: {} orders, {} revenue", summary.totalOrders(), summary.totalRevenue());
            return true;
            
        } catch (Exception e) {
            logger.error("Error loading real data", e);
            return false;
        }
    }
    
    private void loadDemoData() {
        // Demo stats
        updateStatCard("totalRevenue", "12,500,000 ₫", "+15%", SUCCESS);
        updateStatCard("totalOrders", "48", "+8%", SUCCESS);
        updateStatCard("totalGuests", "156", "+12%", SUCCESS);
        updateStatCard("avgPerOrder", "260,000 ₫", "-3%", WARNING);
        
        loadRevenueDemoData();
        loadTopProductsDemoData();
    }
    
    private void updateStatsCards(ReportSummary current, ReportSummary previous) {
        // Total Revenue
        String revenueStr = formatCurrency(current.totalRevenue());
        String revenueChange = calculateChangePercent(current.totalRevenue(), previous.totalRevenue());
        Color revenueColor = current.totalRevenue().compareTo(previous.totalRevenue()) >= 0 ? SUCCESS : ERROR;
        updateStatCard("totalRevenue", revenueStr, revenueChange, revenueColor);
        
        // Total Orders
        String ordersChange = calculateChangePercent(current.totalOrders(), previous.totalOrders());
        Color ordersColor = current.totalOrders() >= previous.totalOrders() ? SUCCESS : ERROR;
        updateStatCard("totalOrders", String.valueOf(current.totalOrders()), ordersChange, ordersColor);
        
        // Total Guests
        String guestsChange = calculateChangePercent(current.totalGuests(), previous.totalGuests());
        Color guestsColor = current.totalGuests() >= previous.totalGuests() ? SUCCESS : ERROR;
        updateStatCard("totalGuests", String.valueOf(current.totalGuests()), guestsChange, guestsColor);
        
        // Avg per order
        String avgStr = formatCurrency(current.avgPerOrder());
        String avgChange = calculateChangePercent(current.avgPerOrder(), previous.avgPerOrder());
        Color avgColor = current.avgPerOrder().compareTo(previous.avgPerOrder()) >= 0 ? SUCCESS : WARNING;
        updateStatCard("avgPerOrder", avgStr, avgChange, avgColor);
    }
    
    private void updateStatCard(String cardId, String value, String change, Color changeColor) {
        for (Component comp : statsCardsPanel.getComponents()) {
            if (comp instanceof JPanel card && cardId.equals(card.getName())) {
                for (Component inner : card.getComponents()) {
                    if (inner instanceof JLabel label) {
                        if ("value".equals(label.getName())) {
                            label.setText(value);
                        } else if ("change".equals(label.getName())) {
                            label.setText(change + " so với kỳ trước");
                            label.setForeground(changeColor);
                        }
                    }
                }
            }
        }
    }
    
    private void loadRevenueTable(List<DailyRevenue> data) {
        revenueModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        
        for (DailyRevenue dr : data) {
            revenueModel.addRow(new Object[]{
                dr.date().format(fmt),
                dr.orderCount(),
                formatCurrency(dr.grossRevenue()),
                formatCurrency(dr.discount()),
                formatCurrency(dr.netRevenue())
            });
            total = total.add(dr.netRevenue());
        }
        
        totalRevenueLabel.setText(formatCurrency(total));
    }
    
    private void loadTopProductsTable(List<TopProduct> data) {
        topProductsModel.setRowCount(0);
        
        // Find max quantity for progress bar
        maxQuantity = data.stream().mapToInt(TopProduct::quantity).max().orElse(1);
        
        for (TopProduct tp : data) {
            String medal = switch (tp.rank()) {
                case 1 -> "🥇";
                case 2 -> "🥈";
                case 3 -> "🥉";
                default -> String.valueOf(tp.rank());
            };
            
            // Store quantity as metadata for progress bar
            String nameWithMeta = tp.name() + "|||" + tp.quantity();
            
            topProductsModel.addRow(new Object[]{
                medal,
                nameWithMeta,
                tp.quantity(),
                formatCurrency(tp.revenue())
            });
        }
    }
    
    private void loadRevenueDemoData() {
        revenueModel.setRowCount(0);
        
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        Random rand = new Random(42);
        BigDecimal total = BigDecimal.ZERO;
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            int orders = 5 + rand.nextInt(10);
            long revenue = (800000L + rand.nextInt(700000)) * orders / 7;
            long discount = revenue * rand.nextInt(10) / 100;
            long net = revenue - discount;
            
            revenueModel.addRow(new Object[]{
                date.format(fmt),
                orders,
                String.format("%,d ₫", revenue),
                String.format("%,d ₫", discount),
                String.format("%,d ₫", net)
            });
            
            total = total.add(BigDecimal.valueOf(net));
        }
        
        totalRevenueLabel.setText(formatCurrency(total));
    }
    
    private void loadTopProductsDemoData() {
        topProductsModel.setRowCount(0);
        maxQuantity = 52;
        
        Object[][] demoData = {
            {"🥇", "Phở bò tái|||45", 45, "2,475,000 ₫"},
            {"🥈", "Cơm chiên dương châu|||38", 38, "2,470,000 ₫"},
            {"🥉", "Cà phê sữa đá|||52", 52, "1,508,000 ₫"},
            {"4", "Bún bò Huế|||28", 28, "1,680,000 ₫"},
            {"5", "Sinh tố bơ|||25", 25, "1,125,000 ₫"},
            {"6", "Gỏi cuốn|||22", 22, "770,000 ₫"},
            {"7", "Trà đào|||20", 20, "700,000 ₫"},
            {"8", "Chả giò chiên|||18", 18, "810,000 ₫"},
        };
        
        for (Object[] row : demoData) {
            topProductsModel.addRow(row);
        }
    }
    
    // ========== Helper Methods ==========
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return String.format("%,d ₫", amount.longValue());
    }
    
    private String calculateChangePercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : "0%";
        }
        BigDecimal change = current.subtract(previous)
            .multiply(BigDecimal.valueOf(100))
            .divide(previous, 0, java.math.RoundingMode.HALF_UP);
        return (change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + change + "%";
    }
    
    private String calculateChangePercent(int current, int previous) {
        return calculateChangePercent(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }
    
    private void exportReport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu báo cáo");
        fileChooser.setSelectedFile(new java.io.File("BaoCaoDoanhThu_" + 
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv"));
        
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "CSV Files (*.csv)", "csv"));
        
        int result = fileChooser.showSaveDialog(SwingUtilities.getWindowAncestor(this));
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new java.io.File(file.getAbsolutePath() + ".csv");
            }
            
            try (java.io.PrintWriter writer = new java.io.PrintWriter(
                    new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
                
                writer.print('\ufeff');
                
                writer.println("BÁO CÁO DOANH THU");
                writer.println("Ngày xuất:," + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                writer.println("Kỳ báo cáo:," + periodFilter.getSelectedItem());
                writer.println();
                
                writer.println("DOANH THU THEO NGÀY");
                writer.println("Ngày,Số đơn,Doanh thu,Giảm giá,Thực thu");
                
                for (int i = 0; i < revenueModel.getRowCount(); i++) {
                    StringBuilder row = new StringBuilder();
                    for (int j = 0; j < revenueModel.getColumnCount(); j++) {
                        if (j > 0) row.append(",");
                        Object value = revenueModel.getValueAt(i, j);
                        if (value != null) {
                            row.append(value.toString().replace(",", ""));
                        }
                    }
                    writer.println(row);
                }
                
                writer.println();
                writer.println("TOP MÓN BÁN CHẠY");
                writer.println("#,Tên món,Số lượng,Doanh thu");
                
                for (int i = 0; i < topProductsModel.getRowCount(); i++) {
                    StringBuilder row = new StringBuilder();
                    for (int j = 0; j < topProductsModel.getColumnCount(); j++) {
                        if (j > 0) row.append(",");
                        Object value = topProductsModel.getValueAt(i, j);
                        if (value != null) {
                            String strVal = value.toString();
                            if (strVal.contains("|||")) {
                                strVal = strVal.split("\\|\\|\\|")[0];
                            }
                            row.append(strVal.replace(",", ""));
                        }
                    }
                    writer.println(row);
                }
                
                logger.info("Report exported to: {}", file.getAbsolutePath());
                ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                    "Đã xuất báo cáo: " + file.getName());
                    
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file.getParentFile());
                }
                
            } catch (Exception e) {
                logger.error("Error exporting report", e);
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), 
                    "Lỗi xuất báo cáo: " + e.getMessage());
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
    
    // ========== Custom Renderers ==========
    
    private class GradientHeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
            label.setForeground(Color.WHITE);
            label.setHorizontalAlignment(column == 0 ? CENTER : (column > 1 ? RIGHT : LEFT));
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            
            return new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    GradientPaint gp = new GradientPaint(0, 0, HEADER_START, getWidth(), 0, HEADER_END);
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    g2d.dispose();
                    
                    label.setBounds(0, 0, getWidth(), getHeight());
                    label.paint(g);
                }
                
                @Override
                public Dimension getPreferredSize() {
                    return label.getPreferredSize();
                }
            };
        }
    }
    
    private class StripedTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                setBackground(row % 2 == 0 ? SURFACE : new Color(SURFACE.getRed() - 8, SURFACE.getGreen() - 8, SURFACE.getBlue() - 5));
            }
            
            setForeground(TEXT_PRIMARY);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            
            return this;
        }
    }
    
    private class MedalRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            setHorizontalAlignment(CENTER);
            String rank = value != null ? value.toString() : "";
            
            if (rank.startsWith("🥇") || rank.startsWith("🥈") || rank.startsWith("🥉")) {
                setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            } else {
                setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
            }
            
            if (!isSelected) {
                setBackground(row % 2 == 0 ? SURFACE : new Color(SURFACE.getRed() - 8, SURFACE.getGreen() - 8, SURFACE.getBlue() - 5));
            }
            setForeground(TEXT_PRIMARY);
            
            return this;
        }
    }
    
    private class ProgressNameRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            String fullValue = value != null ? value.toString() : "";
            String name = fullValue;
            int quantity = 0;
            
            if (fullValue.contains("|||")) {
                String[] parts = fullValue.split("\\|\\|\\|");
                name = parts[0];
                quantity = Integer.parseInt(parts[1]);
            }
            
            final String displayName = name;
            final int qty = quantity;
            final boolean selected = isSelected;
            final int rowNum = row;
            
            return new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Background
                    if (selected) {
                        g2d.setColor(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 50));
                    } else {
                        g2d.setColor(rowNum % 2 == 0 ? SURFACE : new Color(SURFACE.getRed() - 8, SURFACE.getGreen() - 8, SURFACE.getBlue() - 5));
                    }
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    
                    // Progress bar background
                    int barHeight = 6;
                    int barY = getHeight() - barHeight - 4;
                    int barWidth = getWidth() - 20;
                    
                    g2d.setColor(BORDER);
                    g2d.fillRoundRect(10, barY, barWidth, barHeight, barHeight, barHeight);
                    
                    // Progress bar fill
                    float progress = maxQuantity > 0 ? (float) qty / maxQuantity : 0;
                    int fillWidth = (int) (barWidth * progress);
                    
                    GradientPaint gp = new GradientPaint(10, barY, PRIMARY, 10 + fillWidth, barY, SUCCESS);
                    g2d.setPaint(gp);
                    g2d.fillRoundRect(10, barY, fillWidth, barHeight, barHeight, barHeight);
                    
                    // Text
                    g2d.setColor(TEXT_PRIMARY);
                    g2d.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
                    g2d.drawString(displayName, 10, getHeight() / 2);
                    
                    g2d.dispose();
                }
            };
        }
    }
}
