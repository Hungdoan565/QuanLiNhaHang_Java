package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.Category;
import com.restaurant.model.Product;
import com.restaurant.model.User;
import com.restaurant.service.CategoryService;
import com.restaurant.service.ProductService;
import com.restaurant.service.ServiceResult;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Menu Management Panel - Quản lý thực đơn
 * 
 * Features:
 * - CRUD Categories
 * - CRUD Products
 * - Search & Filter
 * - Grid/List view toggle
 */
public class MenuPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(MenuPanel.class);
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color BORDER = Color.decode(AppConfig.Colors.BORDER);
    private static final Color SUCCESS_COLOR = Color.decode(AppConfig.Colors.SUCCESS);
    private static final Color ERROR_COLOR = Color.decode(AppConfig.Colors.ERROR);
    
    private final User currentUser;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final List<Category> categories = new ArrayList<>();
    private final List<Product> products = new ArrayList<>();
    
    // Components
    private JTabbedPane tabbedPane;
    private JTable productTable;
    private DefaultTableModel productTableModel;
    private JTable categoryTable;
    private DefaultTableModel categoryTableModel;
    private JTextField searchField;
    private JComboBox<Category> categoryFilter;
    
    public MenuPanel(User user) {
        this.currentUser = user;
        this.categoryService = CategoryService.getInstance();
        this.productService = ProductService.getInstance();
        initializeUI();
        loadData();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        
        // Tabbed pane for Products and Categories
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        tabbedPane.putClientProperty(FlatClientProperties.STYLE, 
            "tabHeight: 40; tabSelectionHeight: 3; tabSelectionColor: " + colorToHex(PRIMARY));
        
        // Products tab
        JPanel productsTab = createProductsTab();
        tabbedPane.addTab("🍽️ Món ăn", productsTab);
        
        // Categories tab
        JPanel categoriesTab = createCategoriesTab();
        tabbedPane.addTab("📂 Danh mục", categoriesTab);
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    // ===========================================
    // Products Tab
    // ===========================================
    
    private JPanel createProductsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        
        // Toolbar
        JPanel toolbar = createProductToolbar();
        panel.add(toolbar, BorderLayout.NORTH);
        
        // Table
        JPanel tableContainer = createProductTable();
        panel.add(tableContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createProductToolbar() {
        JPanel toolbar = new JPanel(new MigLayout("insets 0", "[]16[]push[]8[]", ""));
        toolbar.setOpaque(false);
        
        // Search
        searchField = new JTextField(20);
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Tìm kiếm món...");
        searchField.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterProducts();
            }
        });
        toolbar.add(searchField);
        
        // Category filter
        categoryFilter = new JComboBox<>();
        categoryFilter.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        categoryFilter.addActionListener(e -> filterProducts());
        toolbar.add(categoryFilter);
        
        // Add button
        JButton addBtn = createButton("➕ Thêm món", PRIMARY, this::showAddProductDialog);
        toolbar.add(addBtn);
        
        // Refresh button
        JButton refreshBtn = createButton("🔄", SURFACE, this::refresh);
        refreshBtn.setForeground(TEXT_PRIMARY);
        toolbar.add(refreshBtn);
        
        return toolbar;
    }
    
    private JPanel createProductTable() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(SURFACE);
        container.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        
        // Table model
        String[] columns = {"ID", "Tên món", "Danh mục", "Giá bán", "Giá vốn", "Trạng thái", "Thao tác"};
        productTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only action column editable
            }
        };
        
        productTable = new JTable(productTableModel);
        productTable.setRowHeight(48);
        productTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        productTable.getTableHeader().setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.setShowVerticalLines(false);
        productTable.setGridColor(BORDER);
        
        // Hide ID column
        productTable.getColumnModel().getColumn(0).setMinWidth(0);
        productTable.getColumnModel().getColumn(0).setMaxWidth(0);
        
        // Set column widths
        productTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        productTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        productTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        productTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        productTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        productTable.getColumnModel().getColumn(6).setPreferredWidth(120);
        
        // Status column renderer
        productTable.getColumnModel().getColumn(5).setCellRenderer(new StatusCellRenderer());
        
        // Action column
        productTable.getColumnModel().getColumn(6).setCellRenderer(new ActionCellRenderer());
        productTable.getColumnModel().getColumn(6).setCellEditor(new ActionCellEditor());
        
        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(null);
        container.add(scrollPane, BorderLayout.CENTER);
        
        return container;
    }
    
    // ===========================================
    // Categories Tab
    // ===========================================
    
    private JPanel createCategoriesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        
        // Toolbar
        JPanel toolbar = new JPanel(new MigLayout("insets 0", "push[]8[]", ""));
        toolbar.setOpaque(false);
        
        JButton addBtn = createButton("➕ Thêm danh mục", PRIMARY, this::showAddCategoryDialog);
        toolbar.add(addBtn);
        
        JButton refreshBtn = createButton("🔄", SURFACE, this::refresh);
        refreshBtn.setForeground(TEXT_PRIMARY);
        toolbar.add(refreshBtn);
        
        panel.add(toolbar, BorderLayout.NORTH);
        
        // Table
        JPanel tableContainer = createCategoryTable();
        panel.add(tableContainer, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createCategoryTable() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(SURFACE);
        container.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        
        String[] columns = {"ID", "Tên danh mục", "Icon", "Máy in", "Số món", "Thao tác"};
        categoryTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
        };
        
        categoryTable = new JTable(categoryTableModel);
        categoryTable.setRowHeight(48);
        categoryTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        categoryTable.getTableHeader().setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        categoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryTable.setShowVerticalLines(false);
        categoryTable.setGridColor(BORDER);
        
        // Hide ID column
        categoryTable.getColumnModel().getColumn(0).setMinWidth(0);
        categoryTable.getColumnModel().getColumn(0).setMaxWidth(0);
        
        // Action column
        categoryTable.getColumnModel().getColumn(5).setCellRenderer(new ActionCellRenderer());
        categoryTable.getColumnModel().getColumn(5).setCellEditor(new ActionCellEditor());
        
        JScrollPane scrollPane = new JScrollPane(categoryTable);
        scrollPane.setBorder(null);
        container.add(scrollPane, BorderLayout.CENTER);
        
        return container;
    }
    
    // ===========================================
    // Data Loading
    // ===========================================
    
    private void loadData() {
        loadCategories();
        loadProducts();
    }
    
    private void loadCategories() {
        categories.clear();
        try {
            categories.addAll(categoryService.getAllCategories());
        } catch (Exception e) {
            logger.error("Error loading categories", e);
            // Fallback to empty list, user will see empty table
        }
        
        // Update category filter
        categoryFilter.removeAllItems();
        categoryFilter.addItem(new Category(0, "Tất cả danh mục"));
        for (Category cat : categories) {
            categoryFilter.addItem(cat);
        }
        
        // Update category table
        categoryTableModel.setRowCount(0);
        for (Category cat : categories) {
            int productCount = categoryService.getProductCount(cat.getId());
            categoryTableModel.addRow(new Object[]{
                cat.getId(),
                cat.getName(),
                cat.getIcon(),
                cat.getPrinterName(),
                productCount,
                "actions"
            });
        }
    }
    
    private void loadProducts() {
        products.clear();
        try {
            products.addAll(productService.getAllProducts());
        } catch (Exception e) {
            logger.error("Error loading products", e);
        }
        refreshProductTable();
    }
    
    private void refreshProductTable() {
        productTableModel.setRowCount(0);
        for (Product p : products) {
            productTableModel.addRow(new Object[]{
                p.getId(),
                p.getName(),
                p.getCategoryName(),
                p.getFormattedPrice(),
                p.getCostPrice() != null ? String.format("%,.0f ₫", p.getCostPrice()) : "-",
                p.isAvailable() ? "Còn hàng" : "Hết hàng",
                "actions"
            });
        }
    }
    
    private void filterProducts() {
        String search = searchField.getText().toLowerCase().trim();
        Category selectedCat = (Category) categoryFilter.getSelectedItem();
        
        productTableModel.setRowCount(0);
        for (Product p : products) {
            boolean matchSearch = search.isEmpty() || p.getName().toLowerCase().contains(search);
            boolean matchCategory = selectedCat == null || selectedCat.getId() == 0 || 
                                    p.getCategoryId() == selectedCat.getId();
            
            if (matchSearch && matchCategory) {
                productTableModel.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    p.getCategoryName(),
                    p.getFormattedPrice(),
                    p.getCostPrice() != null ? String.format("%,.0f ₫", p.getCostPrice()) : "-",
                    p.isAvailable() ? "Còn hàng" : "Hết hàng",
                    "actions"
                });
            }
        }
    }
    
    // ===========================================
    // Dialogs
    // ===========================================
    
    private void showAddProductDialog() {
        showProductDialog(null);
    }
    
    private void showProductDialog(Product product) {
        boolean isEdit = product != null;
        
        JPanel panel = new JPanel(new MigLayout("wrap 2, insets 16", "[][grow, fill]", ""));
        
        // Name
        panel.add(new JLabel("Tên món: *"));
        JTextField nameField = new JTextField(product != null ? product.getName() : "", 25);
        panel.add(nameField);
        
        // Category
        panel.add(new JLabel("Danh mục: *"));
        JComboBox<Category> catCombo = new JComboBox<>();
        for (Category cat : categories) {
            catCombo.addItem(cat);
            if (product != null && product.getCategoryId() == cat.getId()) {
                catCombo.setSelectedItem(cat);
            }
        }
        panel.add(catCombo);
        
        // Price
        panel.add(new JLabel("Giá bán: *"));
        JTextField priceField = new JTextField(product != null ? product.getPrice().toString() : "", 15);
        panel.add(priceField);
        
        // Cost price
        panel.add(new JLabel("Giá vốn:"));
        JTextField costField = new JTextField(product != null && product.getCostPrice() != null ? 
                                              product.getCostPrice().toString() : "", 15);
        panel.add(costField);
        
        // Description
        panel.add(new JLabel("Mô tả:"));
        JTextArea descArea = new JTextArea(product != null ? product.getDescription() : "", 3, 25);
        descArea.setLineWrap(true);
        panel.add(new JScrollPane(descArea));
        
        // Available
        JCheckBox availableCheck = new JCheckBox("Còn hàng", product == null || product.isAvailable());
        panel.add(new JLabel(""));
        panel.add(availableCheck);
        
        int result = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            panel,
            isEdit ? "Sửa món" : "Thêm món mới",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String priceStr = priceField.getText().trim();
            
            if (name.isEmpty() || priceStr.isEmpty()) {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Vui lòng điền đầy đủ thông tin!");
                return;
            }
            
            try {
                BigDecimal price = new BigDecimal(priceStr);
                BigDecimal cost = costField.getText().isEmpty() ? BigDecimal.ZERO : new BigDecimal(costField.getText());
                Category cat = (Category) catCombo.getSelectedItem();
                
                if (isEdit) {
                    product.setName(name);
                    product.setPrice(price);
                    product.setCostPrice(cost);
                    product.setCategory(cat);
                    product.setDescription(descArea.getText());
                    product.setAvailable(availableCheck.isSelected());
                    
                    ServiceResult<Product> sr = productService.updateProduct(product);
                    if (sr.isSuccess()) {
                        ToastNotification.success(SwingUtilities.getWindowAncestor(this), sr.getMessage());
                        loadData();
                    } else {
                        ToastNotification.error(SwingUtilities.getWindowAncestor(this), sr.getMessage());
                    }
                } else {
                    Product newProduct = new Product();
                    newProduct.setName(name);
                    newProduct.setPrice(price);
                    newProduct.setCostPrice(cost);
                    newProduct.setCategoryId(cat.getId());
                    newProduct.setCategory(cat);
                    newProduct.setDescription(descArea.getText());
                    newProduct.setAvailable(availableCheck.isSelected());
                    
                    ServiceResult<Product> sr = productService.createProduct(newProduct);
                    if (sr.isSuccess()) {
                        ToastNotification.success(SwingUtilities.getWindowAncestor(this), sr.getMessage());
                        loadData();
                    } else {
                        ToastNotification.error(SwingUtilities.getWindowAncestor(this), sr.getMessage());
                    }
                }
                
            } catch (NumberFormatException e) {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Giá không hợp lệ!");
            }
        }
    }
    
    private void showAddCategoryDialog() {
        showCategoryDialog(null);
    }
    
    private void showCategoryDialog(Category category) {
        boolean isEdit = category != null;
        
        JPanel panel = new JPanel(new MigLayout("wrap 2, insets 16", "[][grow, fill]", ""));
        
        panel.add(new JLabel("Tên danh mục: *"));
        JTextField nameField = new JTextField(category != null ? category.getName() : "", 20);
        panel.add(nameField);
        
        panel.add(new JLabel("Icon:"));
        JTextField iconField = new JTextField(category != null ? category.getIcon() : "🍽️", 5);
        panel.add(iconField);
        
        panel.add(new JLabel("Máy in:"));
        JComboBox<String> printerCombo = new JComboBox<>(new String[]{
            "Kitchen_Printer", "Bar_Printer", "Cashier_Printer"
        });
        if (category != null) {
            printerCombo.setSelectedItem(category.getPrinterName());
        }
        panel.add(printerCombo);
        
        int result = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            panel,
            isEdit ? "Sửa danh mục" : "Thêm danh mục",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Vui lòng nhập tên!");
                return;
            }
            
            if (isEdit) {
                category.setName(name);
                category.setIcon(iconField.getText());
                category.setPrinterName((String) printerCombo.getSelectedItem());
                
                ServiceResult<Category> sr = categoryService.updateCategory(category);
                if (sr.isSuccess()) {
                    ToastNotification.success(SwingUtilities.getWindowAncestor(this), sr.getMessage());
                    loadCategories();
                } else {
                    ToastNotification.error(SwingUtilities.getWindowAncestor(this), sr.getMessage());
                }
            } else {
                Category newCat = new Category();
                newCat.setName(name);
                newCat.setIcon(iconField.getText());
                newCat.setPrinterName((String) printerCombo.getSelectedItem());
                
                ServiceResult<Category> sr = categoryService.createCategory(newCat);
                if (sr.isSuccess()) {
                    ToastNotification.success(SwingUtilities.getWindowAncestor(this), sr.getMessage());
                    loadCategories();
                } else {
                    ToastNotification.error(SwingUtilities.getWindowAncestor(this), sr.getMessage());
                }
            }
        }
    }
    
    private void editProduct(int productId) {
        Product product = products.stream().filter(p -> p.getId() == productId).findFirst().orElse(null);
        if (product != null) {
            showProductDialog(product);
        }
    }
    
    private void deleteProduct(int productId) {
        Product product = products.stream().filter(p -> p.getId() == productId).findFirst().orElse(null);
        if (product == null) return;
        
        int confirm = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            "Bạn có chắc muốn xóa \"" + product.getName() + "\"?",
            "Xác nhận xóa",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            ServiceResult<Void> sr = productService.deleteProduct(productId);
            if (sr.isSuccess()) {
                ToastNotification.success(SwingUtilities.getWindowAncestor(this), sr.getMessage());
                loadData();
            } else {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), sr.getMessage());
            }
        }
    }
    
    private void editCategory(int categoryId) {
        Category category = categories.stream().filter(c -> c.getId() == categoryId).findFirst().orElse(null);
        if (category != null) {
            showCategoryDialog(category);
        }
    }
    
    private void deleteCategory(int categoryId) {
        Category category = categories.stream().filter(c -> c.getId() == categoryId).findFirst().orElse(null);
        if (category == null) return;
        
        int confirm = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            "Bạn có chắc muốn xóa danh mục \"" + category.getName() + "\"?",
            "Xác nhận xóa",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            ServiceResult<Void> sr = categoryService.deleteCategory(categoryId);
            if (sr.isSuccess()) {
                ToastNotification.success(SwingUtilities.getWindowAncestor(this), sr.getMessage());
                loadCategories();
            } else {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), sr.getMessage());
            }
        }
    }
    
    // ===========================================
    // Helpers
    // ===========================================
    
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
        ToastNotification.info(SwingUtilities.getWindowAncestor(this), "Đã làm mới dữ liệu");
    }
    
    private String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    // ===========================================
    // Custom Renderers
    // ===========================================
    
    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = (String) value;
            if ("Còn hàng".equals(status)) {
                setForeground(SUCCESS_COLOR);
            } else {
                setForeground(ERROR_COLOR);
            }
            setHorizontalAlignment(CENTER);
            return this;
        }
    }
    
    private class ActionCellRenderer extends JPanel implements TableCellRenderer {
        private final JButton editBtn = new JButton("✏️");
        private final JButton deleteBtn = new JButton("🗑️");
        
        public ActionCellRenderer() {
            setLayout(new MigLayout("insets 4, gap 4", "[][]", ""));
            setOpaque(true);
            
            editBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            editBtn.setBorderPainted(false);
            editBtn.setContentAreaFilled(false);
            editBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            deleteBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            deleteBtn.setBorderPainted(false);
            deleteBtn.setContentAreaFilled(false);
            deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            add(editBtn);
            add(deleteBtn);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                                                       boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : SURFACE);
            return this;
        }
    }
    
    private class ActionCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new MigLayout("insets 4, gap 4", "[][]", ""));
        private final JButton editBtn = new JButton("✏️");
        private final JButton deleteBtn = new JButton("🗑️");
        private int currentId;
        private JTable currentTable;
        
        public ActionCellEditor() {
            editBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            editBtn.setBorderPainted(false);
            editBtn.setContentAreaFilled(false);
            editBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            editBtn.addActionListener(e -> {
                stopCellEditing();
                if (currentTable == productTable) {
                    editProduct(currentId);
                } else {
                    editCategory(currentId);
                }
            });
            
            deleteBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            deleteBtn.setBorderPainted(false);
            deleteBtn.setContentAreaFilled(false);
            deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            deleteBtn.addActionListener(e -> {
                stopCellEditing();
                if (currentTable == productTable) {
                    deleteProduct(currentId);
                } else {
                    deleteCategory(currentId);
                }
            });
            
            panel.setOpaque(true);
            panel.setBackground(SURFACE);
            panel.add(editBtn);
            panel.add(deleteBtn);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentTable = table;
            currentId = (int) table.getValueAt(row, 0);
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "actions";
        }
    }
}
