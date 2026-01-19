package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.User;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import com.restaurant.service.SettingsService;

/**
 * Settings Panel - Cài đặt hệ thống
 * 
 * Features:
 * - Cài đặt nhà hàng (tên, địa chỉ, logo)
 * - Cài đặt máy in
 * - Cài đặt hóa đơn
 * - Cài đặt hiển thị
 */
public class SettingsPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(SettingsPanel.class);
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color BORDER = Color.decode(AppConfig.Colors.BORDER);
    private static final Color SUCCESS = Color.decode(AppConfig.Colors.SUCCESS);
    
    private final User currentUser;
    private final SettingsService settingsService;
    private final Map<String, JComponent> settingsFields = new HashMap<>();
    
    public SettingsPanel(User user) {
        this.currentUser = user;
        this.settingsService = SettingsService.getInstance();
        initializeUI();
        loadSettingsFromDB();
    }
    
    private void loadSettingsFromDB() {
        // Load values from SettingsService into fields
        settingsService.reload();
        
        // Restaurant info
        setFieldValue("restaurant.name", settingsService.get(SettingsService.KEY_RESTAURANT_NAME));
        setFieldValue("restaurant.address", settingsService.get(SettingsService.KEY_RESTAURANT_ADDRESS));
        setFieldValue("restaurant.phone", settingsService.get(SettingsService.KEY_RESTAURANT_PHONE));
        
        // Receipt settings
        setCheckboxValue("receipt.enableTax", settingsService.getBoolean(SettingsService.KEY_VAT_ENABLED, true));
        setFieldValue("receipt.taxRate", settingsService.get(SettingsService.KEY_VAT_PERCENT));
        setCheckboxValue("receipt.enableService", settingsService.getBoolean(SettingsService.KEY_SERVICE_CHARGE_ENABLED, true));
        setFieldValue("receipt.serviceCharge", settingsService.get(SettingsService.KEY_SERVICE_CHARGE_PERCENT));
        setFieldValue("receipt.footer", settingsService.get(SettingsService.KEY_RECEIPT_FOOTER));
        
        // Printer settings
        setComboValue("printer.kitchen", settingsService.get(SettingsService.KEY_PRINTER_KITCHEN));
        setComboValue("printer.bar", settingsService.get(SettingsService.KEY_PRINTER_BAR));
        setComboValue("printer.cashier", settingsService.get(SettingsService.KEY_PRINTER_CASHIER));
        setCheckboxValue("printer.autoPrint", settingsService.getBoolean(SettingsService.KEY_KITCHEN_AUTO_PRINT, true));
        setCheckboxValue("printer.printReceipt", settingsService.getBoolean(SettingsService.KEY_PRINT_CUSTOMER_RECEIPT, true));
        
        // Display settings
        setComboValue("display.theme", settingsService.get(SettingsService.KEY_THEME));
        setSliderValue("display.fontSize", settingsService.getInt(SettingsService.KEY_FONT_SIZE, 14));
        setComboValue("display.kitchenCols", settingsService.get(SettingsService.KEY_KITCHEN_COLUMNS));
        
        // Load primary color
        String savedColor = settingsService.get(SettingsService.KEY_PRIMARY_COLOR);
        if (savedColor != null && !savedColor.isEmpty()) {
            setFieldValue("display.primaryColor", savedColor);
            selectColorButton(savedColor);
        }
        
        logger.info("Settings loaded from database");
    }
    
    private void selectColorButton(String colorHex) {
        JComponent colorPanel = settingsFields.get("display.colorPanel");
        if (colorPanel instanceof JPanel panel) {
            for (Component comp : panel.getComponents()) {
                if (comp instanceof JToggleButton btn) {
                    String btnColor = (String) btn.getClientProperty("colorValue");
                    if (colorHex.equalsIgnoreCase(btnColor)) {
                        btn.setSelected(true);
                        break;
                    }
                }
            }
        }
    }
    
    private void setComboValue(String key, String value) {
        JComponent comp = settingsFields.get(key);
        if (comp instanceof JComboBox<?> combo && value != null && !value.isEmpty()) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (combo.getItemAt(i).toString().contains(value) || value.contains(combo.getItemAt(i).toString())) {
                    combo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
    
    private void setSliderValue(String key, int value) {
        JComponent comp = settingsFields.get(key);
        if (comp instanceof JSlider slider) {
            slider.setValue(value);
        }
    }
    
    private void setFieldValue(String key, String value) {
        JComponent comp = settingsFields.get(key);
        if (comp instanceof JTextField tf && value != null) {
            tf.setText(value);
        } else if (comp instanceof JTextArea ta && value != null) {
            ta.setText(value);
        }
    }
    
    private void setCheckboxValue(String key, boolean value) {
        JComponent comp = settingsFields.get(key);
        if (comp instanceof JCheckBox cb) {
            cb.setSelected(value);
        }
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        
        // Action bar at top
        JPanel actionBar = createActionBar();
        add(actionBar, BorderLayout.NORTH);
        
        // Settings content
        JPanel content = new JPanel(new MigLayout("wrap 2, gap 24", "[grow][grow]", ""));
        content.setOpaque(false);
        
        // Left column
        JPanel leftCol = new JPanel(new MigLayout("wrap, insets 0, gap 16", "[grow]", ""));
        leftCol.setOpaque(false);
        leftCol.add(createRestaurantInfoSection(), "grow");
        leftCol.add(createReceiptSection(), "grow");
        
        // Right column
        JPanel rightCol = new JPanel(new MigLayout("wrap, insets 0, gap 16", "[grow]", ""));
        rightCol.setOpaque(false);
        rightCol.add(createPrinterSection(), "grow");
        rightCol.add(createDisplaySection(), "grow");
        
        content.add(leftCol, "grow");
        content.add(rightCol, "grow");
        
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createActionBar() {
        JPanel bar = new JPanel(new MigLayout("insets 0", "push[]8[]", ""));
        bar.setOpaque(false);
        
        JButton saveBtn = new JButton("💾 Lưu thay đổi");
        saveBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        saveBtn.setBackground(SUCCESS);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        saveBtn.addActionListener(e -> saveSettings());
        bar.add(saveBtn);
        
        JButton resetBtn = new JButton("↩️ Hoàn tác");
        resetBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        resetBtn.setBackground(SURFACE);
        resetBtn.setForeground(TEXT_PRIMARY);
        resetBtn.setBorderPainted(false);
        resetBtn.setFocusPainted(false);
        resetBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resetBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        resetBtn.addActionListener(e -> resetSettings());
        bar.add(resetBtn);
        
        return bar;
    }
    
    private JPanel createRestaurantInfoSection() {
        JPanel section = createSection("🏪 Thông tin nhà hàng");
        JPanel content = (JPanel) section.getComponent(1);
        
        // Name
        content.add(createLabel("Tên nhà hàng:"));
        JTextField nameField = createTextField("");
        settingsFields.put("restaurant.name", nameField);
        content.add(nameField, "growx");
        
        // Address
        content.add(createLabel("Địa chỉ:"));
        JTextField addressField = createTextField("");
        settingsFields.put("restaurant.address", addressField);
        content.add(addressField, "growx");
        
        // Phone
        content.add(createLabel("Số điện thoại:"));
        JTextField phoneField = createTextField("");
        settingsFields.put("restaurant.phone", phoneField);
        content.add(phoneField, "growx");
        
        // Logo
        content.add(createLabel("Logo:"));
        JPanel logoPanel = new JPanel(new MigLayout("insets 0, gap 8", "[][]", ""));
        logoPanel.setOpaque(false);
        
        JLabel logoPreview = new JLabel("🍽️");
        logoPreview.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        logoPreview.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        logoPreview.setPreferredSize(new Dimension(80, 80));
        logoPreview.setHorizontalAlignment(SwingConstants.CENTER);
        logoPanel.add(logoPreview);
        
        JButton uploadBtn = new JButton("📤 Tải lên");
        uploadBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        uploadBtn.addActionListener(e -> uploadLogo());
        logoPanel.add(uploadBtn);
        
        content.add(logoPanel, "growx");
        
        return section;
    }
    
    private JPanel createReceiptSection() {
        JPanel section = createSection("🧾 Cài đặt hóa đơn");
        JPanel content = (JPanel) section.getComponent(1);
        
        // Tax
        JCheckBox taxCheck = new JCheckBox("Tính thuế VAT", true);
        taxCheck.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        taxCheck.setOpaque(false);
        settingsFields.put("receipt.enableTax", taxCheck);
        content.add(taxCheck, "span 2");
        
        content.add(createLabel("Thuế suất (%):"));
        JTextField taxField = createTextField("10");
        settingsFields.put("receipt.taxRate", taxField);
        content.add(taxField, "w 100!");
        
        // Service charge
        JCheckBox serviceCheck = new JCheckBox("Phí dịch vụ", true);
        serviceCheck.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        serviceCheck.setOpaque(false);
        settingsFields.put("receipt.enableService", serviceCheck);
        content.add(serviceCheck, "span 2, gaptop 8");
        
        content.add(createLabel("Phí dịch vụ (%):"));
        JTextField serviceField = createTextField("5");
        settingsFields.put("receipt.serviceCharge", serviceField);
        content.add(serviceField, "w 100!");
        
        // Receipt header/footer
        content.add(createLabel("Ghi chú cuối hóa đơn:"), "gaptop 8");
        JTextArea footerArea = new JTextArea("Cảm ơn quý khách!\nHẹn gặp lại!", 3, 20);
        footerArea.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        footerArea.setLineWrap(true);
        JScrollPane footerScroll = new JScrollPane(footerArea);
        settingsFields.put("receipt.footer", footerArea);
        content.add(footerScroll, "growx");
        
        return section;
    }
    
    private JPanel createPrinterSection() {
        JPanel section = createSection("🖨️ Cài đặt máy in");
        JPanel content = (JPanel) section.getComponent(1);
        
        // Kitchen printer
        content.add(createLabel("Máy in bếp:"));
        JComboBox<String> kitchenPrinter = new JComboBox<>(new String[]{
            "Kitchen_Printer", "Printer_01", "Không in"
        });
        kitchenPrinter.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        settingsFields.put("printer.kitchen", kitchenPrinter);
        content.add(kitchenPrinter, "growx");
        
        // Bar printer
        content.add(createLabel("Máy in bar:"));
        JComboBox<String> barPrinter = new JComboBox<>(new String[]{
            "Bar_Printer", "Printer_02", "Không in"
        });
        barPrinter.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        settingsFields.put("printer.bar", barPrinter);
        content.add(barPrinter, "growx");
        
        // Cashier printer
        content.add(createLabel("Máy in thu ngân:"));
        JComboBox<String> cashierPrinter = new JComboBox<>(new String[]{
            "Cashier_Printer", "Printer_03", "Không in"
        });
        cashierPrinter.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        settingsFields.put("printer.cashier", cashierPrinter);
        content.add(cashierPrinter, "growx");
        
        // Options
        JCheckBox autoPrintCheck = new JCheckBox("Tự động in order xuống bếp", true);
        autoPrintCheck.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        autoPrintCheck.setOpaque(false);
        settingsFields.put("printer.autoPrint", autoPrintCheck);
        content.add(autoPrintCheck, "span 2, gaptop 8");
        
        JCheckBox printReceiptCheck = new JCheckBox("In hóa đơn cho khách", true);
        printReceiptCheck.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        printReceiptCheck.setOpaque(false);
        settingsFields.put("printer.printReceipt", printReceiptCheck);
        content.add(printReceiptCheck, "span 2");
        
        // Test print button
        JButton testBtn = new JButton("🖨️ In thử");
        testBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        testBtn.addActionListener(e -> testPrint());
        content.add(testBtn, "span 2, gaptop 8, align right");
        
        return section;
    }
    
    private JPanel createDisplaySection() {
        JPanel section = createSection("🎨 Giao diện hiển thị");
        JPanel content = (JPanel) section.getComponent(1);
        
        // Theme
        content.add(createLabel("Giao diện:"));
        JComboBox<String> themeCombo = new JComboBox<>(new String[]{
            "Sáng (Light)", "Tối (Dark)", "Tự động theo hệ thống"
        });
        themeCombo.setSelectedIndex(0);
        themeCombo.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        settingsFields.put("display.theme", themeCombo);
        content.add(themeCombo, "growx");
        
        // Primary color
        content.add(createLabel("Màu chủ đạo:"));
        JPanel colorPanel = new JPanel(new MigLayout("insets 0, gap 8", "[][][][][]", ""));
        colorPanel.setOpaque(false);
        
        String[] colors = {"#E85A4F", "#3498DB", "#2ECC71", "#9B59B6", "#F39C12"};
        ButtonGroup colorGroup = new ButtonGroup();
        
        // Hidden field to store selected color
        JTextField selectedColorField = new JTextField("#E85A4F");
        selectedColorField.setVisible(false);
        settingsFields.put("display.primaryColor", selectedColorField);
        
        for (String color : colors) {
            JToggleButton colorBtn = new JToggleButton();
            colorBtn.setPreferredSize(new Dimension(32, 32));
            colorBtn.setBackground(Color.decode(color));
            colorBtn.setBorderPainted(false);
            colorBtn.setFocusPainted(false);
            colorBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 999");
            colorBtn.setSelected(color.equals("#E85A4F"));
            colorBtn.putClientProperty("colorValue", color);
            
            // Store reference for loading
            colorBtn.addActionListener(e -> {
                selectedColorField.setText(color);
                logger.debug("Selected primary color: {}", color);
            });
            
            colorGroup.add(colorBtn);
            colorPanel.add(colorBtn);
        }
        
        // Store color panel reference for loading
        colorPanel.putClientProperty("colorButtons", colorGroup);
        settingsFields.put("display.colorPanel", colorPanel);
        content.add(colorPanel, "growx");
        
        // Font size
        content.add(createLabel("Cỡ chữ:"));
        JSlider fontSlider = new JSlider(12, 18, 14);
        fontSlider.setMajorTickSpacing(2);
        fontSlider.setPaintTicks(true);
        fontSlider.setPaintLabels(true);
        fontSlider.setOpaque(false);
        settingsFields.put("display.fontSize", fontSlider);
        content.add(fontSlider, "growx");
        
        // Kitchen display settings
        content.add(createLabel("Màn hình bếp:"), "gaptop 8");
        JComboBox<String> kitchenLayout = new JComboBox<>(new String[]{
            "4 cột", "3 cột", "2 cột"
        });
        kitchenLayout.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        settingsFields.put("display.kitchenCols", kitchenLayout);
        content.add(kitchenLayout, "growx");
        
        return section;
    }
    
    private JPanel createSection(String title) {
        JPanel section = new JPanel(new MigLayout("wrap, insets 0", "[grow]", "[]0[]"));
        section.setOpaque(false);
        
        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        section.add(titleLabel);
        
        // Content card
        JPanel content = new JPanel(new MigLayout("wrap 2, insets 16, gap 8 12", "[][grow]", ""));
        content.setBackground(SURFACE);
        content.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        content.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        section.add(content, "grow");
        
        return section;
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }
    
    private JTextField createTextField(String value) {
        JTextField field = new JTextField(value, 20);
        field.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        field.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        return field;
    }
    
    private void saveSettings() {
        logger.info("Saving settings...");
        
        Map<String, String> settings = new HashMap<>();
        
        // Restaurant info
        JComponent nameComp = settingsFields.get("restaurant.name");
        if (nameComp instanceof JTextField tf) {
            settings.put(SettingsService.KEY_RESTAURANT_NAME, tf.getText());
        }
        JComponent addrComp = settingsFields.get("restaurant.address");
        if (addrComp instanceof JTextField tf) {
            settings.put(SettingsService.KEY_RESTAURANT_ADDRESS, tf.getText());
        }
        JComponent phoneComp = settingsFields.get("restaurant.phone");
        if (phoneComp instanceof JTextField tf) {
            settings.put(SettingsService.KEY_RESTAURANT_PHONE, tf.getText());
        }
        
        // Receipt settings
        JComponent taxEnableComp = settingsFields.get("receipt.enableTax");
        if (taxEnableComp instanceof JCheckBox cb) {
            settings.put(SettingsService.KEY_VAT_ENABLED, cb.isSelected() ? "true" : "false");
        }
        JComponent taxComp = settingsFields.get("receipt.taxRate");
        if (taxComp instanceof JTextField tf) {
            settings.put(SettingsService.KEY_VAT_PERCENT, tf.getText());
        }
        JComponent serviceEnableComp = settingsFields.get("receipt.enableService");
        if (serviceEnableComp instanceof JCheckBox cb) {
            settings.put(SettingsService.KEY_SERVICE_CHARGE_ENABLED, cb.isSelected() ? "true" : "false");
        }
        JComponent serviceComp = settingsFields.get("receipt.serviceCharge");
        if (serviceComp instanceof JTextField tf) {
            settings.put(SettingsService.KEY_SERVICE_CHARGE_PERCENT, tf.getText());
        }
        JComponent footerComp = settingsFields.get("receipt.footer");
        if (footerComp instanceof JTextArea ta) {
            settings.put(SettingsService.KEY_RECEIPT_FOOTER, ta.getText());
        }
        
        // Printer settings
        JComponent kitchenPrinterComp = settingsFields.get("printer.kitchen");
        if (kitchenPrinterComp instanceof JComboBox<?> cb) {
            settings.put(SettingsService.KEY_PRINTER_KITCHEN, cb.getSelectedItem().toString());
        }
        JComponent barPrinterComp = settingsFields.get("printer.bar");
        if (barPrinterComp instanceof JComboBox<?> cb) {
            settings.put(SettingsService.KEY_PRINTER_BAR, cb.getSelectedItem().toString());
        }
        JComponent cashierPrinterComp = settingsFields.get("printer.cashier");
        if (cashierPrinterComp instanceof JComboBox<?> cb) {
            settings.put(SettingsService.KEY_PRINTER_CASHIER, cb.getSelectedItem().toString());
        }
        JComponent autoPrintComp = settingsFields.get("printer.autoPrint");
        if (autoPrintComp instanceof JCheckBox cb) {
            settings.put(SettingsService.KEY_KITCHEN_AUTO_PRINT, cb.isSelected() ? "true" : "false");
        }
        JComponent printReceiptComp = settingsFields.get("printer.printReceipt");
        if (printReceiptComp instanceof JCheckBox cb) {
            settings.put(SettingsService.KEY_PRINT_CUSTOMER_RECEIPT, cb.isSelected() ? "true" : "false");
        }
        
        // Display settings
        JComponent themeComp = settingsFields.get("display.theme");
        if (themeComp instanceof JComboBox<?> cb) {
            String theme = cb.getSelectedItem().toString();
            if (theme.contains("Tối") || theme.contains("Dark")) {
                settings.put(SettingsService.KEY_THEME, "dark");
            } else if (theme.contains("Sáng") || theme.contains("Light")) {
                settings.put(SettingsService.KEY_THEME, "light");
            } else {
                settings.put(SettingsService.KEY_THEME, "system");
            }
        }
        JComponent fontComp = settingsFields.get("display.fontSize");
        if (fontComp instanceof JSlider sl) {
            settings.put(SettingsService.KEY_FONT_SIZE, String.valueOf(sl.getValue()));
        }
        JComponent kitchenColsComp = settingsFields.get("display.kitchenCols");
        if (kitchenColsComp instanceof JComboBox<?> cb) {
            String cols = cb.getSelectedItem().toString();
            settings.put(SettingsService.KEY_KITCHEN_COLUMNS, cols.replaceAll("[^0-9]", ""));
        }
        
        // Primary color
        JComponent colorComp = settingsFields.get("display.primaryColor");
        if (colorComp instanceof JTextField tf) {
            settings.put(SettingsService.KEY_PRIMARY_COLOR, tf.getText());
        }
        
        // Save all to database
        Integer userId = currentUser != null ? currentUser.getId() : null;
        boolean success = settingsService.saveAll(settings, userId);
        
        if (success) {
            ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                "✅ Đã lưu " + settings.size() + " cài đặt!");
            
            // Check if theme changed - notify restart required
            String newTheme = settings.get(SettingsService.KEY_THEME);
            String currentTheme = settingsService.get(SettingsService.KEY_THEME);
            if (newTheme != null && !newTheme.equals(currentTheme)) {
                JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                    "Cần khởi động lại ứng dụng để áp dụng giao diện mới.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            ToastNotification.error(SwingUtilities.getWindowAncestor(this), 
                "❌ Lỗi khi lưu cài đặt!");
        }
    }
    
    private void resetSettings() {
        int confirm = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            "Hoàn tác tất cả thay đổi chưa lưu?",
            "Xác nhận",
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            loadSettingsFromDB();
            ToastNotification.info(SwingUtilities.getWindowAncestor(this), "Đã hoàn tác từ database");
        }
    }
    
    private void uploadLogo() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn logo");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Hình ảnh (PNG, JPG)", "png", "jpg", "jpeg"
        ));
        
        int result = fileChooser.showOpenDialog(SwingUtilities.getWindowAncestor(this));
        if (result == JFileChooser.APPROVE_OPTION) {
            // TODO: Handle logo upload
            ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                "Đã chọn: " + fileChooser.getSelectedFile().getName());
        }
    }
    
    private void testPrint() {
        ToastNotification.info(SwingUtilities.getWindowAncestor(this), 
            "Đang gửi lệnh in thử... (Demo)");
    }
}
