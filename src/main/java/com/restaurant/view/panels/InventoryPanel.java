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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Inventory Panel - Quản lý kho nguyên liệu
 * 
 * Features:
 * - Danh sách nguyên liệu và số lượng tồn
 * - Nhập/Xuất kho
 * - Cảnh báo hết hàng
 * - Lịch sử giao dịch
 */
public class InventoryPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(InventoryPanel.class);
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color BORDER = Color.decode(AppConfig.Colors.BORDER);
    private static final Color SUCCESS_COLOR = Color.decode(AppConfig.Colors.SUCCESS);
    private static final Color WARNING_COLOR = Color.decode(AppConfig.Colors.WARNING);
    private static final Color ERROR_COLOR = Color.decode(AppConfig.Colors.ERROR);
    
    private final User currentUser;
    private final List<InventoryItem> items = new ArrayList<>();
    private final List<StockTransaction> transactions = new ArrayList<>();
    
    private JTabbedPane tabbedPane;
    private JTable inventoryTable;
    private DefaultTableModel inventoryModel;
    private JTable transactionTable;
    private DefaultTableModel transactionModel;
    private JTextField searchField;
    
    public InventoryPanel(User user) {
        this.currentUser = user;
        initializeUI();
        loadData();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        tabbedPane.putClientProperty(FlatClientProperties.STYLE, 
            "tabHeight: 40; tabSelectionHeight: 3; tabSelectionColor: " + colorToHex(PRIMARY));
        
        // Inventory tab
        tabbedPane.addTab("📦 Tồn kho", createInventoryTab());
        
        // Transaction history tab
        tabbedPane.addTab("📋 Lịch sử nhập/xuất", createTransactionsTab());
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createInventoryTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        
        // Toolbar
        JPanel toolbar = new JPanel(new MigLayout("insets 0", "[]16[]push[]8[]8[]", ""));
        toolbar.setOpaque(false);
        
        searchField = new JTextField(20);
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Tìm nguyên liệu...");
        searchField.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                filterInventory();
            }
        });
        toolbar.add(searchField);
        
        // Stats
        JLabel statsLabel = new JLabel();
        updateStats(statsLabel);
        toolbar.add(statsLabel);
        
        // Buttons
        JButton addBtn = createButton("➕ Thêm NL", PRIMARY, this::showAddItemDialog);
        JButton importBtn = createButton("📥 Nhập kho", SUCCESS_COLOR, this::showImportDialog);
        JButton exportBtn = createButton("📤 Xuất kho", WARNING_COLOR, this::showExportDialog);
        
        toolbar.add(addBtn);
        toolbar.add(importBtn);
        toolbar.add(exportBtn);
        
        panel.add(toolbar, BorderLayout.NORTH);
        
        // Table
        String[] columns = {"ID", "Tên nguyên liệu", "Đơn vị", "Tồn kho", "Tối thiểu", "Giá TB", "Trạng thái", "Thao tác"};
        inventoryModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7;
            }
        };
        
        inventoryTable = new JTable(inventoryModel);
        inventoryTable.setRowHeight(48);
        inventoryTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        inventoryTable.getTableHeader().setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        inventoryTable.setShowVerticalLines(false);
        inventoryTable.setGridColor(BORDER);
        
        // Hide ID
        inventoryTable.getColumnModel().getColumn(0).setMinWidth(0);
        inventoryTable.getColumnModel().getColumn(0).setMaxWidth(0);
        
        // Status renderer
        inventoryTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                String status = (String) value;
                if ("Đủ hàng".equals(status)) {
                    setForeground(SUCCESS_COLOR);
                } else if ("Sắp hết".equals(status)) {
                    setForeground(WARNING_COLOR);
                } else {
                    setForeground(ERROR_COLOR);
                }
                return this;
            }
        });
        
        // Action buttons
        inventoryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = inventoryTable.rowAtPoint(e.getPoint());
                int col = inventoryTable.columnAtPoint(e.getPoint());
                if (col == 7 && row >= 0) {
                    int itemId = (int) inventoryModel.getValueAt(row, 0);
                    editItem(itemId);
                }
            }
        });
        
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(SURFACE);
        tableContainer.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        tableContainer.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        
        panel.add(tableContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createTransactionsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        
        // Filter toolbar
        JPanel toolbar = new JPanel(new MigLayout("insets 0", "[]8[]push[]", ""));
        toolbar.setOpaque(false);
        
        JComboBox<String> typeFilter = new JComboBox<>(new String[]{"Tất cả", "Nhập kho", "Xuất kho", "Điều chỉnh"});
        typeFilter.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        toolbar.add(new JLabel("Loại:"));
        toolbar.add(typeFilter);
        
        JButton refreshBtn = createButton("🔄 Làm mới", SURFACE, this::refresh);
        refreshBtn.setForeground(TEXT_PRIMARY);
        toolbar.add(refreshBtn);
        
        panel.add(toolbar, BorderLayout.NORTH);
        
        // Transaction table
        String[] columns = {"ID", "Thời gian", "Loại", "Nguyên liệu", "Số lượng", "Ghi chú", "Người thực hiện"};
        transactionModel = new DefaultTableModel(columns, 0);
        
        transactionTable = new JTable(transactionModel);
        transactionTable.setRowHeight(40);
        transactionTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        transactionTable.getTableHeader().setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        transactionTable.setShowVerticalLines(false);
        
        // Hide ID
        transactionTable.getColumnModel().getColumn(0).setMinWidth(0);
        transactionTable.getColumnModel().getColumn(0).setMaxWidth(0);
        
        // Type renderer
        transactionTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String type = (String) value;
                if ("Nhập kho".equals(type)) {
                    setForeground(SUCCESS_COLOR);
                } else if ("Xuất kho".equals(type)) {
                    setForeground(ERROR_COLOR);
                } else {
                    setForeground(WARNING_COLOR);
                }
                return this;
            }
        });
        
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(SURFACE);
        tableContainer.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        tableContainer.add(new JScrollPane(transactionTable), BorderLayout.CENTER);
        
        panel.add(tableContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadData() {
        loadInventory();
        loadTransactions();
    }
    
    private void loadInventory() {
        items.clear();
        
        // Demo data
        items.add(new InventoryItem(1, "Cà phê hạt rang", "kg", 5.5, 2.0, new BigDecimal("350000")));
        items.add(new InventoryItem(2, "Sữa tươi", "lít", 15.0, 10.0, new BigDecimal("32000")));
        items.add(new InventoryItem(3, "Đường", "kg", 8.0, 3.0, new BigDecimal("22000")));
        items.add(new InventoryItem(4, "Bột mì", "kg", 1.5, 5.0, new BigDecimal("18000"))); // Low stock
        items.add(new InventoryItem(5, "Trứng gà", "quả", 120, 50, new BigDecimal("3500")));
        items.add(new InventoryItem(6, "Thịt bò", "kg", 0.5, 2.0, new BigDecimal("280000"))); // Critical
        items.add(new InventoryItem(7, "Rau xà lách", "kg", 3.0, 1.0, new BigDecimal("25000")));
        items.add(new InventoryItem(8, "Nước mắm", "lít", 4.0, 2.0, new BigDecimal("45000")));
        
        refreshInventoryTable();
    }
    
    private void loadTransactions() {
        transactions.clear();
        
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        transactions.add(new StockTransaction(1, LocalDateTime.now().minusHours(2), "Nhập kho", 
            "Cà phê hạt rang", 3.0, "Nhập từ NCC ABC", "admin"));
        transactions.add(new StockTransaction(2, LocalDateTime.now().minusHours(5), "Xuất kho",
            "Sữa tươi", -5.0, "Sử dụng trong ngày", "chef1"));
        transactions.add(new StockTransaction(3, LocalDateTime.now().minusDays(1), "Nhập kho",
            "Trứng gà", 100, "Nhập hàng tuần", "admin"));
        transactions.add(new StockTransaction(4, LocalDateTime.now().minusDays(1), "Điều chỉnh",
            "Bột mì", -2.0, "Hao hụt", "admin"));
        
        refreshTransactionTable();
    }
    
    private void refreshInventoryTable() {
        inventoryModel.setRowCount(0);
        for (InventoryItem item : items) {
            inventoryModel.addRow(new Object[]{
                item.id,
                item.name,
                item.unit,
                String.format("%.1f", item.quantity),
                String.format("%.1f", item.minStock),
                String.format("%,.0f ₫", item.avgPrice),
                item.getStatus(),
                "✏️"
            });
        }
    }
    
    private void refreshTransactionTable() {
        transactionModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        
        for (StockTransaction t : transactions) {
            transactionModel.addRow(new Object[]{
                t.id,
                t.timestamp.format(fmt),
                t.type,
                t.itemName,
                (t.quantity > 0 ? "+" : "") + String.format("%.1f", t.quantity),
                t.note,
                t.user
            });
        }
    }
    
    private void filterInventory() {
        String search = searchField.getText().toLowerCase().trim();
        inventoryModel.setRowCount(0);
        
        for (InventoryItem item : items) {
            if (search.isEmpty() || item.name.toLowerCase().contains(search)) {
                inventoryModel.addRow(new Object[]{
                    item.id,
                    item.name,
                    item.unit,
                    String.format("%.1f", item.quantity),
                    String.format("%.1f", item.minStock),
                    String.format("%,.0f ₫", item.avgPrice),
                    item.getStatus(),
                    "✏️"
                });
            }
        }
    }
    
    private void updateStats(JLabel label) {
        long lowStock = items.stream().filter(i -> i.quantity <= i.minStock).count();
        label.setText("⚠️ " + lowStock + " nguyên liệu cần nhập thêm");
        label.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        label.setForeground(lowStock > 0 ? WARNING_COLOR : TEXT_SECONDARY);
    }
    
    private void showAddItemDialog() {
        JPanel panel = new JPanel(new MigLayout("wrap 2, insets 16", "[][grow,fill]", ""));
        
        panel.add(new JLabel("Tên nguyên liệu: *"));
        JTextField nameField = new JTextField(20);
        panel.add(nameField);
        
        panel.add(new JLabel("Đơn vị: *"));
        JComboBox<String> unitCombo = new JComboBox<>(new String[]{"kg", "g", "lít", "ml", "quả", "cái", "gói", "hộp"});
        panel.add(unitCombo);
        
        panel.add(new JLabel("Số lượng ban đầu:"));
        JTextField qtyField = new JTextField("0", 10);
        panel.add(qtyField);
        
        panel.add(new JLabel("Mức tối thiểu:"));
        JTextField minField = new JTextField("1", 10);
        panel.add(minField);
        
        panel.add(new JLabel("Giá trung bình:"));
        JTextField priceField = new JTextField("0", 10);
        panel.add(priceField);
        
        int result = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this),
            panel, "Thêm nguyên liệu", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Vui lòng nhập tên!");
                return;
            }
            
            try {
                InventoryItem item = new InventoryItem(
                    items.size() + 1,
                    name,
                    (String) unitCombo.getSelectedItem(),
                    Double.parseDouble(qtyField.getText()),
                    Double.parseDouble(minField.getText()),
                    new BigDecimal(priceField.getText())
                );
                items.add(item);
                refreshInventoryTable();
                ToastNotification.success(SwingUtilities.getWindowAncestor(this), "Đã thêm: " + name);
            } catch (NumberFormatException e) {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Số liệu không hợp lệ!");
            }
        }
    }
    
    private void showImportDialog() {
        JPanel panel = new JPanel(new MigLayout("wrap 2, insets 16", "[][grow,fill]", ""));
        
        panel.add(new JLabel("Nguyên liệu: *"));
        JComboBox<InventoryItem> itemCombo = new JComboBox<>();
        for (InventoryItem item : items) {
            itemCombo.addItem(item);
        }
        panel.add(itemCombo);
        
        panel.add(new JLabel("Số lượng nhập: *"));
        JTextField qtyField = new JTextField(10);
        panel.add(qtyField);
        
        panel.add(new JLabel("Đơn giá:"));
        JTextField priceField = new JTextField(10);
        panel.add(priceField);
        
        panel.add(new JLabel("Ghi chú:"));
        JTextField noteField = new JTextField(20);
        panel.add(noteField);
        
        int result = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this),
            panel, "Nhập kho", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                InventoryItem item = (InventoryItem) itemCombo.getSelectedItem();
                double qty = Double.parseDouble(qtyField.getText());
                
                if (qty <= 0) {
                    ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Số lượng phải > 0!");
                    return;
                }
                
                item.quantity += qty;
                
                transactions.add(0, new StockTransaction(
                    transactions.size() + 1,
                    LocalDateTime.now(),
                    "Nhập kho",
                    item.name,
                    qty,
                    noteField.getText(),
                    currentUser.getUsername()
                ));
                
                refreshInventoryTable();
                refreshTransactionTable();
                ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                    "Đã nhập " + qty + " " + item.unit + " " + item.name);
            } catch (NumberFormatException e) {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Số lượng không hợp lệ!");
            }
        }
    }
    
    private void showExportDialog() {
        JPanel panel = new JPanel(new MigLayout("wrap 2, insets 16", "[][grow,fill]", ""));
        
        panel.add(new JLabel("Nguyên liệu: *"));
        JComboBox<InventoryItem> itemCombo = new JComboBox<>();
        for (InventoryItem item : items) {
            itemCombo.addItem(item);
        }
        panel.add(itemCombo);
        
        panel.add(new JLabel("Số lượng xuất: *"));
        JTextField qtyField = new JTextField(10);
        panel.add(qtyField);
        
        panel.add(new JLabel("Lý do:"));
        JComboBox<String> reasonCombo = new JComboBox<>(new String[]{
            "Sử dụng trong ngày", "Hao hụt", "Hư hỏng", "Trả nhà cung cấp", "Khác"
        });
        panel.add(reasonCombo);
        
        panel.add(new JLabel("Ghi chú:"));
        JTextField noteField = new JTextField(20);
        panel.add(noteField);
        
        int result = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this),
            panel, "Xuất kho", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                InventoryItem item = (InventoryItem) itemCombo.getSelectedItem();
                double qty = Double.parseDouble(qtyField.getText());
                
                if (qty <= 0) {
                    ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Số lượng phải > 0!");
                    return;
                }
                
                if (qty > item.quantity) {
                    ToastNotification.error(SwingUtilities.getWindowAncestor(this), 
                        "Không đủ hàng! Còn " + item.quantity + " " + item.unit);
                    return;
                }
                
                item.quantity -= qty;
                
                String note = reasonCombo.getSelectedItem() + 
                    (noteField.getText().isEmpty() ? "" : " - " + noteField.getText());
                
                transactions.add(0, new StockTransaction(
                    transactions.size() + 1,
                    LocalDateTime.now(),
                    "Xuất kho",
                    item.name,
                    -qty,
                    note,
                    currentUser.getUsername()
                ));
                
                refreshInventoryTable();
                refreshTransactionTable();
                ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                    "Đã xuất " + qty + " " + item.unit + " " + item.name);
            } catch (NumberFormatException e) {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Số lượng không hợp lệ!");
            }
        }
    }
    
    private void editItem(int itemId) {
        InventoryItem item = items.stream().filter(i -> i.id == itemId).findFirst().orElse(null);
        if (item == null) return;
        
        JPanel panel = new JPanel(new MigLayout("wrap 2, insets 16", "[][grow,fill]", ""));
        
        panel.add(new JLabel("Tên:"));
        JTextField nameField = new JTextField(item.name, 20);
        panel.add(nameField);
        
        panel.add(new JLabel("Mức tối thiểu:"));
        JTextField minField = new JTextField(String.valueOf(item.minStock), 10);
        panel.add(minField);
        
        panel.add(new JLabel("Giá TB:"));
        JTextField priceField = new JTextField(item.avgPrice.toString(), 10);
        panel.add(priceField);
        
        int result = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this),
            panel, "Sửa nguyên liệu", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            item.name = nameField.getText().trim();
            item.minStock = Double.parseDouble(minField.getText());
            item.avgPrice = new BigDecimal(priceField.getText());
            refreshInventoryTable();
            ToastNotification.success(SwingUtilities.getWindowAncestor(this), "Đã cập nhật!");
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
        loadData();
        ToastNotification.info(SwingUtilities.getWindowAncestor(this), "Đã làm mới");
    }
    
    private String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    // ===========================================
    // Inner Classes
    // ===========================================
    
    private static class InventoryItem {
        int id;
        String name;
        String unit;
        double quantity;
        double minStock;
        BigDecimal avgPrice;
        
        InventoryItem(int id, String name, String unit, double quantity, double minStock, BigDecimal avgPrice) {
            this.id = id;
            this.name = name;
            this.unit = unit;
            this.quantity = quantity;
            this.minStock = minStock;
            this.avgPrice = avgPrice;
        }
        
        String getStatus() {
            if (quantity <= 0) return "Hết hàng";
            if (quantity <= minStock) return "Sắp hết";
            return "Đủ hàng";
        }
        
        @Override
        public String toString() {
            return name + " (" + quantity + " " + unit + ")";
        }
    }
    
    private static class StockTransaction {
        int id;
        LocalDateTime timestamp;
        String type;
        String itemName;
        double quantity;
        String note;
        String user;
        
        StockTransaction(int id, LocalDateTime timestamp, String type, String itemName, 
                        double quantity, String note, String user) {
            this.id = id;
            this.timestamp = timestamp;
            this.type = type;
            this.itemName = itemName;
            this.quantity = quantity;
            this.note = note;
            this.user = user;
        }
    }
}
