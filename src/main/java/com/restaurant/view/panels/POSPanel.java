package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.Category;
import com.restaurant.model.Product;
import com.restaurant.model.Table;
import com.restaurant.model.Table.TableStatus;
import com.restaurant.model.User;
import com.restaurant.service.CategoryService;
import com.restaurant.service.ProductService;
import com.restaurant.service.TableService;
import com.restaurant.util.KitchenOrderManager;
import com.restaurant.util.KitchenOrderManager.KitchenOrder;
import com.restaurant.util.KitchenOrderManager.OrderItem;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * POS Panel - Enhanced với Menu Grid và Table Status Management
 * 
 * Features:
 * - Split view: Table Map / Menu Grid
 * - Click-based product selection
 * - Table status: Available, Occupied, Reserved, Cleaning
 * - Order management với quantity controls
 * - Kitchen connection (Gửi bếp)
 * - Payment dialog
 */
public class POSPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(POSPanel.class);
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color SUCCESS = Color.decode(AppConfig.Colors.SUCCESS);
    private static final Color WARNING = Color.decode(AppConfig.Colors.WARNING);
    private static final Color ERROR = Color.decode(AppConfig.Colors.ERROR);
    private static final Color BORDER = Color.decode(AppConfig.Colors.BORDER);
    
    private final User currentUser;
    private final TableService tableService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final NumberFormat currencyFormat;
    
    // Data
    private final List<Table> tables = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private final List<Product> products = new ArrayList<>();
    private final List<OrderItem> orderItems = new ArrayList<>();
    
    // State
    private Table selectedTable;
    private JPanel selectedTableCard;
    private Category selectedCategory;
    private boolean showingMenu = false;
    
    // UI Components - Left Panel
    private JPanel leftPanel;
    private CardLayout leftCardLayout;
    private JPanel tableGrid;
    private JPanel menuPanel;
    private JPanel categoryTabs;
    private JPanel productGrid;
    private JToggleButton tableMapBtn;
    private JToggleButton menuBtn;
    private JComboBox<String> areaFilter;
    
    // UI Components - Right Panel (Order Section)
    private JPanel orderSectionContent;
    private JLabel orderTableName;
    private JLabel orderGuestInfo;
    private JPanel orderItemsPanel;
    private JLabel orderSubtotal;
    private JButton openTableBtn;
    private JButton reserveBtn;
    private JButton cleaningBtn;
    private JButton sendKitchenBtn;
    private JButton payBtn;
    private JPanel actionButtonsPanel;
    
    public POSPanel(User user) {
        this.currentUser = user;
        this.tableService = TableService.getInstance();
        this.categoryService = CategoryService.getInstance();
        this.productService = ProductService.getInstance();
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        
        initializeUI();
        loadData();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(16, 16));
        setBackground(BACKGROUND);
        
        // Left: Table Map / Menu Grid (toggle)
        leftPanel = createLeftPanel();
        add(leftPanel, BorderLayout.CENTER);
        
        // Right: Order Panel
        JPanel orderSection = createOrderSection();
        add(orderSection, BorderLayout.EAST);
    }
    
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        
        // Header with toggle and filter
        JPanel header = createLeftHeader();
        panel.add(header, BorderLayout.NORTH);
        
        // Card layout for Table Map / Menu
        leftCardLayout = new CardLayout();
        JPanel contentPanel = new JPanel(leftCardLayout);
        contentPanel.setOpaque(false);
        
        // Table Map
        contentPanel.add(createTableMapPanel(), "tables");
        
        // Menu Grid
        contentPanel.add(createMenuPanel(), "menu");
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createLeftHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0", "[][]16[]16[]push[]", ""));
        header.setOpaque(false);
        
        // Toggle buttons
        ButtonGroup toggleGroup = new ButtonGroup();
        
        tableMapBtn = createToggleButton("🪑 Sơ đồ bàn", true);
        tableMapBtn.addActionListener(e -> {
            showingMenu = false;
            leftCardLayout.show((Container) leftPanel.getComponent(1), "tables");
            areaFilter.setVisible(true);
        });
        toggleGroup.add(tableMapBtn);
        header.add(tableMapBtn);
        
        menuBtn = createToggleButton("🍜 Thực đơn", false);
        menuBtn.addActionListener(e -> {
            if (selectedTable == null || !selectedTable.hasActiveOrder()) {
                ToastNotification.warning(SwingUtilities.getWindowAncestor(this), 
                    "Vui lòng chọn bàn có khách để đặt món");
                tableMapBtn.setSelected(true);
                return;
            }
            showingMenu = true;
            leftCardLayout.show((Container) leftPanel.getComponent(1), "menu");
            areaFilter.setVisible(false);
        });
        toggleGroup.add(menuBtn);
        header.add(menuBtn);
        
        // Separator
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 30));
        header.add(sep);
        
        // Area filter (only for table map)
        JLabel filterLabel = new JLabel("Khu vực:");
        filterLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        filterLabel.setForeground(TEXT_PRIMARY);
        header.add(filterLabel);
        
        areaFilter = new JComboBox<>(new String[]{"Tất cả", "Tầng 1", "Tầng 2", "Phòng VIP"});
        areaFilter.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        areaFilter.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        areaFilter.addActionListener(e -> refreshTableGrid());
        header.add(areaFilter);
        
        // Legend
        JPanel legend = createLegend();
        header.add(legend);
        
        return header;
    }
    
    private JToggleButton createToggleButton(String text, boolean selected) {
        JToggleButton btn = new JToggleButton(text);
        btn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        btn.setBackground(selected ? PRIMARY : SURFACE);
        btn.setForeground(selected ? Color.WHITE : TEXT_PRIMARY);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setSelected(selected);
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        
        btn.addChangeListener(e -> {
            if (btn.isSelected()) {
                btn.setBackground(PRIMARY);
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(SURFACE);
                btn.setForeground(TEXT_PRIMARY);
            }
        });
        
        return btn;
    }
    
    private JPanel createLegend() {
        JPanel legend = new JPanel(new MigLayout("insets 0, gap 12", "", ""));
        legend.setOpaque(false);
        
        for (TableStatus status : TableStatus.values()) {
            JPanel item = new JPanel(new MigLayout("insets 0, gap 4", "[][]", ""));
            item.setOpaque(false);
            
            JPanel dot = new JPanel();
            dot.setPreferredSize(new Dimension(10, 10));
            dot.setBackground(Color.decode(status.getColorHex()));
            dot.putClientProperty(FlatClientProperties.STYLE, "arc: 999");
            item.add(dot);
            
            JLabel label = new JLabel(status.getDisplayName());
            label.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 11));
            label.setForeground(TEXT_SECONDARY);
            item.add(label);
            
            legend.add(item);
        }
        
        return legend;
    }
    
    private JPanel createTableMapPanel() {
        tableGrid = new JPanel(new MigLayout("wrap 4, gap 12", "[grow][grow][grow][grow]", ""));
        tableGrid.setOpaque(false);
        
        JScrollPane scrollPane = new JScrollPane(tableGrid);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createMenuPanel() {
        menuPanel = new JPanel(new BorderLayout(0, 12));
        menuPanel.setOpaque(false);
        
        // Category tabs
        categoryTabs = new JPanel(new MigLayout("insets 0, gap 8", "", ""));
        categoryTabs.setOpaque(false);
        
        JScrollPane catScroll = new JScrollPane(categoryTabs);
        catScroll.setBorder(null);
        catScroll.setOpaque(false);
        catScroll.getViewport().setOpaque(false);
        catScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        catScroll.setPreferredSize(new Dimension(0, 45));
        
        menuPanel.add(catScroll, BorderLayout.NORTH);
        
        // Product grid
        productGrid = new JPanel(new MigLayout("wrap 5, gap 12", "[grow][grow][grow][grow][grow]", ""));
        productGrid.setOpaque(false);
        
        JScrollPane prodScroll = new JScrollPane(productGrid);
        prodScroll.setBorder(null);
        prodScroll.setOpaque(false);
        prodScroll.getViewport().setOpaque(false);
        prodScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        prodScroll.getVerticalScrollBar().setUnitIncrement(16);
        
        menuPanel.add(prodScroll, BorderLayout.CENTER);
        
        return menuPanel;
    }
    
    private JPanel createOrderSection() {
        JPanel panel = new JPanel(new MigLayout("fill, wrap, insets 16", "[grow]", "[][grow][]"));
        panel.setBackground(SURFACE);
        panel.setPreferredSize(new Dimension(350, 0));
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));
        
        // Header
        JLabel title = new JLabel("Chi tiết đơn hàng");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        panel.add(title);
        
        // Dynamic content
        orderSectionContent = new JPanel(new CardLayout());
        orderSectionContent.setOpaque(false);
        
        orderSectionContent.add(createEmptyState(), "empty");
        orderSectionContent.add(createOrderDetailPanel(), "detail");
        
        panel.add(orderSectionContent, "grow");
        
        // Action buttons
        actionButtonsPanel = createActionButtons();
        panel.add(actionButtonsPanel, "dock south, growx");
        
        return panel;
    }
    
    private JPanel createEmptyState() {
        JPanel panel = new JPanel(new MigLayout("fill, wrap", "[center]", "[center]"));
        panel.setOpaque(false);
        
        JLabel icon = new JLabel("🪑");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        panel.add(icon, "center");
        
        JLabel text = new JLabel("Chọn một bàn để bắt đầu");
        text.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        text.setForeground(TEXT_SECONDARY);
        panel.add(text, "center");
        
        return panel;
    }
    
    private JPanel createOrderDetailPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, wrap, insets 0", "[grow]", "[][][][grow][]"));
        panel.setOpaque(false);
        
        // Table info
        JPanel infoCard = new JPanel(new MigLayout("wrap, insets 12", "[grow]", ""));
        infoCard.setBackground(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 20));
        infoCard.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        
        orderTableName = new JLabel("🪑 --");
        orderTableName.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        orderTableName.setForeground(TEXT_PRIMARY);
        infoCard.add(orderTableName);
        
        orderGuestInfo = new JLabel("👤 -- khách | ⏱ -- phút");
        orderGuestInfo.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        orderGuestInfo.setForeground(TEXT_SECONDARY);
        infoCard.add(orderGuestInfo);
        
        panel.add(infoCard, "growx");
        
        // Order items header
        JPanel itemsHeader = new JPanel(new MigLayout("insets 8 0", "[]push[]", ""));
        itemsHeader.setOpaque(false);
        
        JLabel itemsTitle = new JLabel("Danh sách món");
        itemsTitle.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        itemsTitle.setForeground(TEXT_PRIMARY);
        itemsHeader.add(itemsTitle);
        
        panel.add(itemsHeader, "growx");
        
        // Order items list
        orderItemsPanel = new JPanel(new MigLayout("wrap, insets 0, gap 6", "[grow]", ""));
        orderItemsPanel.setOpaque(false);
        
        JScrollPane itemsScroll = new JScrollPane(orderItemsPanel);
        itemsScroll.setBorder(null);
        itemsScroll.setOpaque(false);
        itemsScroll.getViewport().setOpaque(false);
        itemsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        panel.add(itemsScroll, "grow");
        
        // Subtotal
        JPanel subtotalPanel = new JPanel(new MigLayout("insets 12", "[]push[]", ""));
        subtotalPanel.setBackground(BACKGROUND);
        subtotalPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        
        JLabel subtotalLabel = new JLabel("Tạm tính:");
        subtotalLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        subtotalLabel.setForeground(TEXT_PRIMARY);
        subtotalPanel.add(subtotalLabel);
        
        orderSubtotal = new JLabel("0 ₫");
        orderSubtotal.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 20));
        orderSubtotal.setForeground(SUCCESS);
        subtotalPanel.add(orderSubtotal);
        
        panel.add(subtotalPanel, "growx");
        
        return panel;
    }
    
    private JPanel createActionButtons() {
        JPanel panel = new JPanel(new MigLayout("wrap, insets 8 0 0 0, gap 8", "[grow]", ""));
        panel.setOpaque(false);
        
        // Row 1: Table status buttons (for empty table)
        JPanel statusRow = new JPanel(new MigLayout("insets 0, gap 6", "[grow][grow][grow]", ""));
        statusRow.setOpaque(false);
        
        openTableBtn = createActionBtn("🚪 Mở bàn", PRIMARY);
        openTableBtn.addActionListener(e -> openNewTable());
        statusRow.add(openTableBtn, "grow");
        
        reserveBtn = createActionBtn("📅 Đặt trước", WARNING);
        reserveBtn.addActionListener(e -> reserveTable());
        statusRow.add(reserveBtn, "grow");
        
        cleaningBtn = createActionBtn("🧹 Đang dọn", TEXT_SECONDARY);
        cleaningBtn.addActionListener(e -> setTableCleaning());
        statusRow.add(cleaningBtn, "grow");
        
        panel.add(statusRow, "growx");
        
        // Row 2: Order action buttons (for occupied table)
        JPanel orderRow = new JPanel(new MigLayout("insets 0, gap 6", "[grow][grow]", ""));
        orderRow.setOpaque(false);
        
        sendKitchenBtn = createActionBtn("🍳 Gửi bếp", WARNING);
        sendKitchenBtn.addActionListener(e -> sendToKitchen());
        sendKitchenBtn.setVisible(false);
        orderRow.add(sendKitchenBtn, "grow");
        
        payBtn = createActionBtn("💳 Thanh toán", SUCCESS);
        payBtn.addActionListener(e -> processPayment());
        payBtn.setVisible(false);
        orderRow.add(payBtn, "grow");
        
        panel.add(orderRow, "growx");
        
        return panel;
    }
    
    private JButton createActionBtn(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btn.setPreferredSize(new Dimension(0, 40));
        return btn;
    }
    
    // ==================== DATA LOADING ====================
    
    private void loadData() {
        loadTables();
        loadCategories();
        loadProducts();
    }
    
    private void loadTables() {
        tables.clear();
        try {
            tables.addAll(tableService.getAllTables());
        } catch (Exception e) {
            logger.error("Error loading tables", e);
        }
        refreshTableGrid();
    }
    
    private void loadCategories() {
        categories.clear();
        try {
            categories.addAll(categoryService.getAllCategories());
        } catch (Exception e) {
            logger.error("Error loading categories", e);
        }
        refreshCategoryTabs();
    }
    
    private void loadProducts() {
        products.clear();
        try {
            products.addAll(productService.getAllProducts());
        } catch (Exception e) {
            logger.error("Error loading products", e);
        }
    }
    
    // ==================== TABLE GRID ====================
    
    private void refreshTableGrid() {
        tableGrid.removeAll();
        
        String selectedArea = (String) areaFilter.getSelectedItem();
        
        for (Table table : tables) {
            if ("Tất cả".equals(selectedArea) || table.getArea().equals(selectedArea)) {
                JPanel card = createTableCard(table);
                tableGrid.add(card, "grow, w 140!, h 120!");
            }
        }
        
        tableGrid.revalidate();
        tableGrid.repaint();
    }
    
    private JPanel createTableCard(Table table) {
        Color statusColor = Color.decode(table.getStatus().getColorHex());
        boolean isSelected = selectedTable != null && selectedTable.getId() == table.getId();
        
        JPanel card = new JPanel(new MigLayout("fill, wrap, insets 10", "[center]", "[]2[]2[]2[]")) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(statusColor);
                g2d.fillRoundRect(0, 0, getWidth(), 4, 8, 8);
                g2d.dispose();
            }
        };
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createLineBorder(isSelected ? PRIMARY : BORDER, isSelected ? 2 : 1));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Table name
        JLabel nameLabel = new JLabel(table.getName());
        nameLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 15));
        nameLabel.setForeground(TEXT_PRIMARY);
        card.add(nameLabel, "center");
        
        // Capacity
        String capText = table.hasActiveOrder() 
            ? "👤 " + table.getGuestCount() + "/" + table.getCapacity()
            : "👤 " + table.getCapacity() + " chỗ";
        JLabel capLabel = new JLabel(capText);
        capLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 11));
        capLabel.setForeground(TEXT_SECONDARY);
        card.add(capLabel, "center");
        
        // Status badge
        JPanel badge = new JPanel(new MigLayout("insets 2 8", "[]", ""));
        badge.setBackground(new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), 50));
        badge.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        JLabel statusLabel = new JLabel(table.getStatusDisplay());
        statusLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 10));
        statusLabel.setForeground(statusColor);
        badge.add(statusLabel);
        card.add(badge, "center");
        
        // Duration (if occupied)
        if (table.hasActiveOrder() && table.getOccupiedSince() != null) {
            long mins = Duration.between(table.getOccupiedSince(), LocalDateTime.now()).toMinutes();
            JLabel timeLabel = new JLabel("⏱ " + mins + " phút");
            timeLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 10));
            timeLabel.setForeground(TEXT_SECONDARY);
            card.add(timeLabel, "center");
        }
        
        // Click handlers
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectTable(table, card);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected) card.setBorder(BorderFactory.createLineBorder(statusColor, 2));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (selectedTable == null || selectedTable.getId() != table.getId()) {
                    card.setBorder(BorderFactory.createLineBorder(BORDER, 1));
                }
            }
        });
        
        return card;
    }
    
    // ==================== CATEGORY & PRODUCT GRID ====================
    
    private void refreshCategoryTabs() {
        categoryTabs.removeAll();
        
        boolean first = true;
        for (Category cat : categories) {
            JToggleButton btn = createCategoryTab(cat, first);
            categoryTabs.add(btn);
            if (first) {
                selectedCategory = cat;
                first = false;
            }
        }
        
        categoryTabs.revalidate();
        categoryTabs.repaint();
        refreshProductGrid();
    }
    
    private JToggleButton createCategoryTab(Category cat, boolean selected) {
        JToggleButton btn = new JToggleButton(cat.getIcon() + " " + cat.getName());
        btn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        btn.setBackground(selected ? PRIMARY : BACKGROUND);
        btn.setForeground(selected ? Color.WHITE : TEXT_PRIMARY);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setSelected(selected);
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btn.setPreferredSize(new Dimension(120, 35));
        
        btn.addActionListener(e -> {
            selectedCategory = cat;
            // Update all tabs
            for (Component c : categoryTabs.getComponents()) {
                if (c instanceof JToggleButton tb) {
                    tb.setSelected(false);
                    tb.setBackground(BACKGROUND);
                    tb.setForeground(TEXT_PRIMARY);
                }
            }
            btn.setSelected(true);
            btn.setBackground(PRIMARY);
            btn.setForeground(Color.WHITE);
            refreshProductGrid();
        });
        
        return btn;
    }
    
    private void refreshProductGrid() {
        productGrid.removeAll();
        
        if (selectedCategory == null) {
            productGrid.revalidate();
            productGrid.repaint();
            return;
        }
        
        for (Product product : products) {
            if (product.getCategoryId() == selectedCategory.getId() && product.isAvailable()) {
                JPanel card = createProductCard(product);
                productGrid.add(card, "grow, w 120!, h 100!");
            }
        }
        
        productGrid.revalidate();
        productGrid.repaint();
    }
    
    private JPanel createProductCard(Product product) {
        JPanel card = new JPanel(new MigLayout("fill, wrap, insets 8", "[center]", "[][][]"));
        card.setBackground(BACKGROUND);
        card.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Icon mapping based on product name
        String icon = getProductIcon(product);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        card.add(iconLabel, "center");
        
        // Name (truncated)
        String name = product.getName();
        if (name.length() > 12) name = name.substring(0, 10) + "...";
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
        nameLabel.setForeground(TEXT_PRIMARY);
        card.add(nameLabel, "center");
        
        // Price
        JLabel priceLabel = new JLabel(formatShortPrice(product.getPrice()));
        priceLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
        priceLabel.setForeground(SUCCESS);
        card.add(priceLabel, "center");
        
        // Click to add
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                addProductToOrder(product);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 30));
                card.setBorder(BorderFactory.createLineBorder(PRIMARY, 2));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(BACKGROUND);
                card.setBorder(BorderFactory.createLineBorder(BORDER, 1));
            }
        });
        
        return card;
    }
    
    /**
     * Get appropriate icon for product based on name keywords
     */
    private String getProductIcon(Product product) {
        String name = product.getName().toLowerCase();
        
        // Drinks
        if (name.contains("cà phê") || name.contains("cafe") || name.contains("coffee")) return "☕";
        if (name.contains("trà") || name.contains("tea")) return "🍵";
        if (name.contains("sinh tố") || name.contains("smoothie")) return "🥤";
        if (name.contains("nước ép") || name.contains("cam") || name.contains("juice")) return "🍊";
        if (name.contains("coca") || name.contains("pepsi") || name.contains("sprite")) return "🥤";
        if (name.contains("nước suối") || name.contains("water")) return "💧";
        if (name.contains("sữa")) return "🥛";
        
        // Beer & Wine
        if (name.contains("bia") || name.contains("beer")) return "🍺";
        if (name.contains("rượu vang") || name.contains("wine")) return "🍷";
        if (name.contains("whisky") || name.contains("vodka") || name.contains("rượu")) return "🥃";
        
        // Main dishes
        if (name.contains("phở")) return "🍜";
        if (name.contains("bún")) return "🍜";
        if (name.contains("mì")) return "🍝";
        if (name.contains("cơm")) return "🍚";
        if (name.contains("cá")) return "🐟";
        if (name.contains("gà")) return "🍗";
        if (name.contains("bò")) return "🥩";
        if (name.contains("heo") || name.contains("sườn")) return "🍖";
        
        // Seafood
        if (name.contains("tôm") || name.contains("shrimp")) return "🦐";
        if (name.contains("cua") || name.contains("crab")) return "🦀";
        if (name.contains("mực") || name.contains("squid")) return "🦑";
        if (name.contains("nghêu") || name.contains("sò") || name.contains("ốc")) return "🐚";
        if (name.contains("ghẹ")) return "🦞";
        
        // Hotpot
        if (name.contains("lẩu")) return "🍲";
        
        // Appetizers
        if (name.contains("gỏi") || name.contains("salad")) return "🥗";
        if (name.contains("chả giò") || name.contains("nem")) return "🥟";
        if (name.contains("súp")) return "🥣";
        if (name.contains("khoai")) return "🍟";
        if (name.contains("đậu hũ") || name.contains("đậu phụ")) return "🧈";
        
        // Desserts
        if (name.contains("chè")) return "🍧";
        if (name.contains("kem")) return "🍨";
        if (name.contains("bánh flan") || name.contains("flan")) return "🍮";
        if (name.contains("bánh")) return "🍰";
        if (name.contains("trái cây") || name.contains("hoa quả")) return "🍉";
        if (name.contains("sữa chua")) return "🥛";
        
        // Default to category icon
        if (product.getCategory() != null) {
            return product.getCategory().getIcon();
        }
        return "🍽️";
    }
    
    private String formatShortPrice(BigDecimal price) {
        long value = price.longValue();
        if (value >= 1000000) {
            return String.format("%.1ftr", value / 1000000.0);
        } else if (value >= 1000) {
            return String.format("%dk", value / 1000);
        }
        return String.valueOf(value);
    }
    
    // ==================== TABLE SELECTION & ACTIONS ====================
    
    private void selectTable(Table table, JPanel card) {
        // Deselect previous
        if (selectedTableCard != null) {
            selectedTableCard.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        }
        
        selectedTable = table;
        selectedTableCard = card;
        card.setBorder(BorderFactory.createLineBorder(PRIMARY, 2));
        
        logger.info("Selected table: {}", table.getName());
        
        updateOrderSection();
        updateButtonStates();
    }
    
    private void updateOrderSection() {
        CardLayout cl = (CardLayout) orderSectionContent.getLayout();
        
        if (selectedTable == null) {
            cl.show(orderSectionContent, "empty");
            return;
        }
        
        if (selectedTable.hasActiveOrder()) {
            orderTableName.setText("🪑 " + selectedTable.getName());
            
            long mins = 0;
            if (selectedTable.getOccupiedSince() != null) {
                mins = Duration.between(selectedTable.getOccupiedSince(), LocalDateTime.now()).toMinutes();
            }
            orderGuestInfo.setText("👤 " + selectedTable.getGuestCount() + " khách | ⏱ " + mins + " phút");
            
            // Load demo items if empty
            if (orderItems.isEmpty()) {
                // Don't add demo items - let user add from menu
            }
            
            refreshOrderItems();
            cl.show(orderSectionContent, "detail");
        } else {
            cl.show(orderSectionContent, "empty");
        }
    }
    
    private void updateButtonStates() {
        if (selectedTable == null) {
            openTableBtn.setVisible(true);
            openTableBtn.setEnabled(false);
            reserveBtn.setVisible(true);
            reserveBtn.setEnabled(false);
            cleaningBtn.setVisible(true);
            cleaningBtn.setEnabled(false);
            sendKitchenBtn.setVisible(false);
            payBtn.setVisible(false);
            return;
        }
        
        switch (selectedTable.getStatus()) {
            case AVAILABLE -> {
                openTableBtn.setVisible(true);
                openTableBtn.setEnabled(true);
                reserveBtn.setVisible(true);
                reserveBtn.setEnabled(true);
                cleaningBtn.setVisible(true);
                cleaningBtn.setEnabled(true);
                sendKitchenBtn.setVisible(false);
                payBtn.setVisible(false);
            }
            case OCCUPIED -> {
                openTableBtn.setVisible(false);
                reserveBtn.setVisible(false);
                cleaningBtn.setVisible(false);
                sendKitchenBtn.setVisible(true);
                sendKitchenBtn.setEnabled(!orderItems.isEmpty());
                payBtn.setVisible(true);
                payBtn.setEnabled(true);
            }
            case RESERVED, CLEANING -> {
                openTableBtn.setVisible(true);
                openTableBtn.setEnabled(false);
                reserveBtn.setVisible(true);
                reserveBtn.setEnabled(false);
                cleaningBtn.setVisible(true);
                cleaningBtn.setEnabled(false);
                sendKitchenBtn.setVisible(false);
                payBtn.setVisible(false);
                
                // Allow converting reserved/cleaning to available
                JButton resetBtn = new JButton("✓ Đặt trống");
                // Would need to add this dynamically
            }
        }
    }
    
    // ==================== ORDER MANAGEMENT ====================
    
    private void addProductToOrder(Product product) {
        if (selectedTable == null || !selectedTable.hasActiveOrder()) {
            ToastNotification.warning(SwingUtilities.getWindowAncestor(this), "Vui lòng mở bàn trước");
            return;
        }
        
        // Check if product already in order
        for (OrderItem item : orderItems) {
            if (item.productId == product.getId()) {
                item.quantity++;
                refreshOrderItems();
                return;
            }
        }
        
        // Add new item
        orderItems.add(new OrderItem(product.getId(), product.getName(), 1, product.getPrice()));
        refreshOrderItems();
        
        ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
            "Đã thêm: " + product.getName());
    }
    
    private void refreshOrderItems() {
        orderItemsPanel.removeAll();
        
        if (orderItems.isEmpty()) {
            JLabel emptyLabel = new JLabel("Chưa có món - Chọn từ Thực đơn");
            emptyLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.ITALIC, 12));
            emptyLabel.setForeground(TEXT_SECONDARY);
            orderItemsPanel.add(emptyLabel, "center");
        } else {
            for (OrderItem item : orderItems) {
                orderItemsPanel.add(createOrderItemRow(item), "growx");
            }
        }
        
        updateSubtotal();
        updateButtonStates();
        
        orderItemsPanel.revalidate();
        orderItemsPanel.repaint();
    }
    
    private JPanel createOrderItemRow(OrderItem item) {
        JPanel row = new JPanel(new MigLayout("insets 6 8, gap 6", "[][grow][][]", ""));
        row.setBackground(BACKGROUND);
        row.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        
        // Quantity with +/- buttons
        JButton minusBtn = new JButton("-");
        minusBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        minusBtn.setBackground(SURFACE);
        minusBtn.setForeground(TEXT_PRIMARY);
        minusBtn.setBorderPainted(false);
        minusBtn.setPreferredSize(new Dimension(28, 28));
        minusBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        minusBtn.addActionListener(e -> {
            if (item.quantity > 1) {
                item.quantity--;
                refreshOrderItems();
            }
        });
        row.add(minusBtn);
        
        JLabel qtyLabel = new JLabel(String.valueOf(item.quantity));
        qtyLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        qtyLabel.setForeground(TEXT_PRIMARY);
        qtyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        qtyLabel.setPreferredSize(new Dimension(25, 20));
        row.add(qtyLabel);
        
        JButton plusBtn = new JButton("+");
        plusBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        plusBtn.setBackground(PRIMARY);
        plusBtn.setForeground(Color.WHITE);
        plusBtn.setBorderPainted(false);
        plusBtn.setPreferredSize(new Dimension(28, 28));
        plusBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        plusBtn.addActionListener(e -> {
            item.quantity++;
            refreshOrderItems();
        });
        row.add(plusBtn);
        
        // Item name
        String name = item.name.length() > 15 ? item.name.substring(0, 13) + "..." : item.name;
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        nameLabel.setForeground(TEXT_PRIMARY);
        row.add(nameLabel, "grow");
        
        // Item total
        BigDecimal total = item.price.multiply(new BigDecimal(item.quantity));
        JLabel priceLabel = new JLabel(formatShortPrice(total));
        priceLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        priceLabel.setForeground(SUCCESS);
        row.add(priceLabel);
        
        // Delete button
        JButton deleteBtn = new JButton("✕");
        deleteBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 10));
        deleteBtn.setBackground(ERROR);
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setPreferredSize(new Dimension(24, 24));
        deleteBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        deleteBtn.addActionListener(e -> {
            orderItems.remove(item);
            refreshOrderItems();
        });
        row.add(deleteBtn);
        
        return row;
    }
    
    private void updateSubtotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : orderItems) {
            subtotal = subtotal.add(item.price.multiply(new BigDecimal(item.quantity)));
        }
        orderSubtotal.setText(currencyFormat.format(subtotal));
    }
    
    // ==================== TABLE ACTIONS ====================
    
    private void openNewTable() {
        if (selectedTable == null || !selectedTable.isAvailable()) {
            ToastNotification.warning(SwingUtilities.getWindowAncestor(this), "Vui lòng chọn bàn trống");
            return;
        }
        
        // Guest count dialog
        JPanel dialogPanel = new JPanel(new MigLayout("wrap, insets 20", "[grow]", ""));
        dialogPanel.setBackground(SURFACE);
        
        JLabel title = new JLabel("🚪 Mở " + selectedTable.getName());
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        dialogPanel.add(title, "center");
        
        JLabel info = new JLabel("Chọn số khách (tối đa " + selectedTable.getCapacity() + ")");
        info.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        info.setForeground(TEXT_SECONDARY);
        dialogPanel.add(info, "center, gaptop 8");
        
        JPanel btnsPanel = new JPanel(new MigLayout("insets 12, gap 8", "[grow][grow][grow][grow]", ""));
        btnsPanel.setOpaque(false);
        
        int[] selectedCount = {1};
        JButton[] guestBtns = new JButton[Math.min(selectedTable.getCapacity(), 8)];
        
        for (int i = 0; i < guestBtns.length; i++) {
            int count = i + 1;
            JButton btn = new JButton(String.valueOf(count));
            btn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
            btn.setPreferredSize(new Dimension(50, 50));
            btn.setBorderPainted(false);
            btn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
            
            if (count == 1) {
                btn.setBackground(PRIMARY);
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(BACKGROUND);
                btn.setForeground(TEXT_PRIMARY);
            }
            
            btn.addActionListener(e -> {
                selectedCount[0] = count;
                for (JButton b : guestBtns) {
                    b.setBackground(BACKGROUND);
                    b.setForeground(TEXT_PRIMARY);
                }
                btn.setBackground(PRIMARY);
                btn.setForeground(Color.WHITE);
            });
            
            guestBtns[i] = btn;
            btnsPanel.add(btn, "grow");
        }
        
        dialogPanel.add(btnsPanel, "growx, gaptop 16");
        
        int result = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            dialogPanel, "Mở bàn",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            selectedTable.setStatus(TableStatus.OCCUPIED);
            selectedTable.setGuestCount(selectedCount[0]);
            selectedTable.setOccupiedSince(LocalDateTime.now());
            selectedTable.setCurrentOrderCode("ORD-" + String.format("%03d", (int)(Math.random() * 1000)));
            
            orderItems.clear();
            
            refreshTableGrid();
            updateOrderSection();
            updateButtonStates();
            
            // Auto switch to menu
            menuBtn.doClick();
            
            ToastNotification.success(SwingUtilities.getWindowAncestor(this),
                "Đã mở " + selectedTable.getName() + " - Chọn món từ Thực đơn");
        }
    }
    
    private void reserveTable() {
        if (selectedTable == null || !selectedTable.isAvailable()) return;
        
        String name = JOptionPane.showInputDialog(
            SwingUtilities.getWindowAncestor(this),
            "Tên khách đặt bàn:",
            "Đặt trước " + selectedTable.getName(),
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (name != null && !name.trim().isEmpty()) {
            selectedTable.setStatus(TableStatus.RESERVED);
            refreshTableGrid();
            updateOrderSection();
            updateButtonStates();
            
            ToastNotification.info(SwingUtilities.getWindowAncestor(this),
                selectedTable.getName() + " đã được đặt trước cho " + name);
        }
    }
    
    private void setTableCleaning() {
        if (selectedTable == null || !selectedTable.isAvailable()) return;
        
        selectedTable.setStatus(TableStatus.CLEANING);
        refreshTableGrid();
        updateOrderSection();
        updateButtonStates();
        
        ToastNotification.info(SwingUtilities.getWindowAncestor(this),
            selectedTable.getName() + " đang được dọn dẹp");
    }
    
    private void sendToKitchen() {
        if (orderItems.isEmpty()) {
            ToastNotification.warning(SwingUtilities.getWindowAncestor(this), "Chưa có món để gửi");
            return;
        }
        
        // Convert to kitchen order items
        List<KitchenOrderManager.OrderItem> kitchenItems = new ArrayList<>();
        for (com.restaurant.view.panels.POSPanel.OrderItem item : orderItems) {
            kitchenItems.add(new KitchenOrderManager.OrderItem(item.name, item.quantity));
        }
        
        // Create kitchen order and push
        String orderCode = selectedTable.getCurrentOrderCode();
        if (orderCode == null) {
            orderCode = "ORD-" + String.format("%03d", (int)(Math.random() * 1000));
        }
        
        KitchenOrder kitchenOrder = new KitchenOrder(
            orderCode,
            selectedTable.getName(),
            kitchenItems
        );
        
        KitchenOrderManager.getInstance().addOrder(kitchenOrder);
        
        ToastNotification.success(SwingUtilities.getWindowAncestor(this),
            "Đã gửi " + orderItems.size() + " món xuống bếp!");
        
        // Mark items as sent
        sendKitchenBtn.setEnabled(false);
        sendKitchenBtn.setText("✓ Đã gửi bếp");
    }
    
    private void processPayment() {
        if (selectedTable == null || !selectedTable.hasActiveOrder()) return;
        
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : orderItems) {
            subtotal = subtotal.add(item.price.multiply(new BigDecimal(item.quantity)));
        }
        BigDecimal vat = subtotal.multiply(new BigDecimal("0.08"));
        BigDecimal total = subtotal.add(vat);
        
        showPaymentDialog(subtotal, vat, total);
    }
    
    private void showPaymentDialog(BigDecimal subtotal, BigDecimal vat, BigDecimal total) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Thanh toán", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(480, 720);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        
        JPanel content = new JPanel(new MigLayout("fill, wrap, insets 20", "[grow]", ""));
        content.setBackground(SURFACE);
        
        // Header
        JLabel header = new JLabel("💳 Thanh toán " + selectedTable.getName());
        header.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 20));
        content.add(header, "center");
        
        // Summary
        JPanel summary = new JPanel(new MigLayout("wrap 2, insets 12", "[grow][]", ""));
        summary.setBackground(BACKGROUND);
        summary.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        
        summary.add(new JLabel("Tạm tính:"));
        summary.add(new JLabel(currencyFormat.format(subtotal)));
        summary.add(new JLabel("VAT (8%):"));
        summary.add(new JLabel(currencyFormat.format(vat)));
        summary.add(new JSeparator(), "span, growx, gaptop 8, gapbottom 8");
        
        JLabel totalLabel = new JLabel("TỔNG:");
        totalLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        summary.add(totalLabel);
        JLabel totalValue = new JLabel(currencyFormat.format(total));
        totalValue.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        totalValue.setForeground(SUCCESS);
        summary.add(totalValue);
        
        content.add(summary, "growx, gaptop 12");
        
        // Payment methods
        content.add(new JLabel("Phương thức thanh toán:"), "gaptop 12");
        
        JPanel methods = new JPanel(new MigLayout("insets 0, gap 8", "[grow][grow][grow]", ""));
        methods.setOpaque(false);
        
        String[] selectedMethod = {"CASH"};
        JButton cashBtn = createMethodBtn("💵 Tiền mặt", true);
        JButton transferBtn = createMethodBtn("📱 Chuyển khoản", false);
        JButton cardBtn = createMethodBtn("💳 Thẻ", false);
        
        // QR Code panel for transfer (hidden by default)
        JPanel qrPanel = new JPanel(new MigLayout("wrap, insets 12", "[center]", ""));
        qrPanel.setBackground(BACKGROUND);
        qrPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        qrPanel.setVisible(false);
        
        // Generate VietQR URL
        String bankId = "970436"; // VietcomBank
        String accountNo = "1029849106";
        String accountName = "DOAN VINH HUNG";
        long amount = total.longValue();
        String description = selectedTable.getName().replace(" ", "") + "_" + System.currentTimeMillis() % 10000;
        String vietQRUrl = String.format(
            "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
            bankId, accountNo, amount, description, accountName.replace(" ", "%20")
        );
        
        JLabel qrTitle = new JLabel("📱 Quét mã để thanh toán");
        qrTitle.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        qrTitle.setForeground(TEXT_PRIMARY);
        qrPanel.add(qrTitle, "center");
        
        // QR Code image
        try {
            java.net.URL url = new java.net.URL(vietQRUrl);
            ImageIcon qrIcon = new ImageIcon(url);
            // Scale to fit
            Image scaled = qrIcon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
            JLabel qrImage = new JLabel(new ImageIcon(scaled));
            qrPanel.add(qrImage, "center, gaptop 8");
        } catch (Exception ex) {
            JLabel qrPlaceholder = new JLabel("[QR Code]");
            qrPlaceholder.setPreferredSize(new Dimension(200, 200));
            qrPlaceholder.setHorizontalAlignment(SwingConstants.CENTER);
            qrPanel.add(qrPlaceholder, "center");
            logger.warn("Failed to load QR code: {}", ex.getMessage());
        }
        
        JLabel bankInfo = new JLabel("STK: " + accountNo + " - VietcomBank");
        bankInfo.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        bankInfo.setForeground(PRIMARY);
        qrPanel.add(bankInfo, "center, gaptop 8");
        
        JLabel amountInfo = new JLabel("Số tiền: " + currencyFormat.format(total));
        amountInfo.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        amountInfo.setForeground(SUCCESS);
        qrPanel.add(amountInfo, "center, gaptop 4");
        
        // Cash input panel (visible by default since Cash is selected)
        JPanel cashPanel = new JPanel(new MigLayout("wrap, insets 8, gapy 8", "[grow]", ""));
        cashPanel.setOpaque(false);
        cashPanel.setVisible(true); // Default: Cash selected
        
        cashBtn.addActionListener(e -> {
            selectedMethod[0] = "CASH";
            cashBtn.setBackground(SUCCESS); cashBtn.setForeground(Color.WHITE);
            transferBtn.setBackground(BACKGROUND); transferBtn.setForeground(TEXT_PRIMARY);
            cardBtn.setBackground(BACKGROUND); cardBtn.setForeground(TEXT_PRIMARY);
            cashPanel.setVisible(true); qrPanel.setVisible(false);
        });
        transferBtn.addActionListener(e -> {
            selectedMethod[0] = "TRANSFER";
            transferBtn.setBackground(SUCCESS); transferBtn.setForeground(Color.WHITE);
            cashBtn.setBackground(BACKGROUND); cashBtn.setForeground(TEXT_PRIMARY);
            cardBtn.setBackground(BACKGROUND); cardBtn.setForeground(TEXT_PRIMARY);
            cashPanel.setVisible(false); qrPanel.setVisible(true);
        });
        cardBtn.addActionListener(e -> {
            selectedMethod[0] = "CARD";
            cardBtn.setBackground(SUCCESS); cardBtn.setForeground(Color.WHITE);
            cashBtn.setBackground(BACKGROUND); cashBtn.setForeground(TEXT_PRIMARY);
            transferBtn.setBackground(BACKGROUND); transferBtn.setForeground(TEXT_PRIMARY);
            cashPanel.setVisible(false); qrPanel.setVisible(false);
        });
        
        methods.add(cashBtn, "grow");
        methods.add(transferBtn, "grow");
        methods.add(cardBtn, "grow");
        content.add(methods, "growx");
        
        // QR Panel (for transfer) - centered
        content.add(qrPanel, "center, growx, gaptop 12");
        
        // Cash input section
        cashPanel.add(new JLabel("Tiền khách đưa:"), "gaptop 8");
        
        JFormattedTextField cashInput = new JFormattedTextField();
        cashInput.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 20));
        cashInput.setHorizontalAlignment(SwingConstants.RIGHT);
        cashInput.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        cashPanel.add(cashInput, "growx, h 50!");
        
        // Quick amount buttons
        JPanel quickAmounts = new JPanel(new MigLayout("insets 0, gap 6", "[grow][grow][grow][grow]", ""));
        quickAmounts.setOpaque(false);
        
        long totalLong = total.longValue();
        long roundedUp = ((totalLong / 10000) + 1) * 10000; // Round up to nearest 10k
        
        String[] quickLabels = {"100k", "200k", "500k", "Đủ tiền"};
        long[] quickValues = {100000, 200000, 500000, roundedUp};
        
        final BigDecimal finalTotal = total;
        JLabel changeValue = new JLabel("0 ₫");
        
        for (int i = 0; i < quickLabels.length; i++) {
            int idx = i;
            JButton qBtn = new JButton(i == 3 ? currencyFormat.format(quickValues[i]) : quickLabels[i]);
            qBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
            qBtn.setBackground(BACKGROUND);
            qBtn.setForeground(TEXT_PRIMARY);
            qBtn.setBorderPainted(false);
            qBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
            qBtn.addActionListener(e -> {
                cashInput.setText(formatWithCommas(quickValues[idx]));
                updateChange(quickValues[idx], finalTotal, changeValue);
            });
            quickAmounts.add(qBtn, "grow");
        }
        cashPanel.add(quickAmounts, "growx, gaptop 6");
        
        // Change panel
        JPanel changePanel = new JPanel(new MigLayout("insets 12", "[]push[]", ""));
        changePanel.setBackground(new Color(SUCCESS.getRed(), SUCCESS.getGreen(), SUCCESS.getBlue(), 30));
        changePanel.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        changePanel.add(new JLabel("Tiền thối:"));
        changeValue.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        changeValue.setForeground(SUCCESS);
        changePanel.add(changeValue);
        cashPanel.add(changePanel, "growx, gaptop 8");
        
        content.add(cashPanel, "growx");
        
        // Format on type
        cashInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    String text = cashInput.getText().replaceAll("[^0-9]", "");
                    if (!text.isEmpty()) {
                        long value = Long.parseLong(text);
                        cashInput.setText(formatWithCommas(value));
                        cashInput.setCaretPosition(cashInput.getText().length());
                        updateChange(value, finalTotal, changeValue);
                    } else {
                        changeValue.setText("0 ₫");
                    }
                } catch (Exception ex) {
                    changeValue.setText("--");
                }
            }
        });
        
        // Buttons
        JPanel buttons = new JPanel(new MigLayout("insets 0, gap 12", "[grow][grow]", ""));
        buttons.setOpaque(false);
        
        JButton cancelBtn = createActionBtn("Hủy", BACKGROUND);
        cancelBtn.setForeground(TEXT_PRIMARY);
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttons.add(cancelBtn, "grow, h 48!");
        
        JButton confirmBtn = createActionBtn("✓ Xác nhận thanh toán", SUCCESS);
        confirmBtn.addActionListener(e -> {
            dialog.dispose();
            completePayment();
        });
        buttons.add(confirmBtn, "grow, h 48!");
        
        content.add(buttons, "growx, gaptop 16");
        
        dialog.setContentPane(content);
        dialog.setVisible(true);
    }
    
    private JButton createMethodBtn(String text, boolean selected) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        btn.setBackground(selected ? SUCCESS : BACKGROUND);
        btn.setForeground(selected ? Color.WHITE : TEXT_PRIMARY);
        btn.setBorderPainted(false);
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        return btn;
    }
    
    /**
     * Format number with comma separators
     */
    private String formatWithCommas(long value) {
        return String.format("%,d", value).replace(",", ".");
    }
    
    /**
     * Update change label based on received amount
     */
    private void updateChange(long received, BigDecimal total, JLabel changeLabel) {
        BigDecimal change = new BigDecimal(received).subtract(total);
        if (change.compareTo(BigDecimal.ZERO) >= 0) {
            changeLabel.setText(currencyFormat.format(change));
            changeLabel.setForeground(SUCCESS);
        } else {
            changeLabel.setText("Thiếu " + currencyFormat.format(change.abs()));
            changeLabel.setForeground(ERROR);
        }
    }
    
    private void completePayment() {
        selectedTable.setStatus(TableStatus.AVAILABLE);
        selectedTable.setGuestCount(0);
        selectedTable.setOccupiedSince(null);
        selectedTable.setCurrentOrderCode(null);
        
        orderItems.clear();
        
        refreshTableGrid();
        
        CardLayout cl = (CardLayout) orderSectionContent.getLayout();
        cl.show(orderSectionContent, "empty");
        
        updateButtonStates();
        
        // Switch back to table map
        tableMapBtn.doClick();
        
        ToastNotification.success(SwingUtilities.getWindowAncestor(this),
            "Thanh toán thành công! " + selectedTable.getName() + " đã trống.");
        
        selectedTable = null;
        selectedTableCard = null;
    }
    
    public void refresh() {
        loadData();
    }
    
    // Order item helper class
    private static class OrderItem {
        int productId;
        String name;
        int quantity;
        BigDecimal price;
        
        OrderItem(int productId, String name, int quantity, BigDecimal price) {
            this.productId = productId;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
    }
}
