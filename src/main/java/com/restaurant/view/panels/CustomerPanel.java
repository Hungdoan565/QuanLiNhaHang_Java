package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.Customer;
import com.restaurant.model.Customer.CustomerTier;
import com.restaurant.model.User;
import com.restaurant.service.CustomerService;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Customer Panel - Quản lý khách hàng và loyalty
 * For Admin and Manager
 */
public class CustomerPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(CustomerPanel.class);
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color SUCCESS = Color.decode(AppConfig.Colors.SUCCESS);
    private static final Color WARNING = Color.decode(AppConfig.Colors.WARNING);
    private static final Color DANGER = Color.decode(AppConfig.Colors.ERROR);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    
    private final User currentUser;
    private final CustomerService customerService;
    private final NumberFormat currencyFormat;
    
    // UI Components
    private JTextField searchField;
    private JTable customerTable;
    private DefaultTableModel tableModel;
    private JLabel totalLabel;
    
    public CustomerPanel(User user) {
        this.currentUser = user;
        this.customerService = CustomerService.getInstance();
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        
        initializeUI();
        loadCustomers();
    }
    
    private void initializeUI() {
        setLayout(new MigLayout("fill, insets 16", "[grow]", "[][grow]"));
        setBackground(BACKGROUND);
        
        add(createHeader(), "growx, wrap");
        add(createContent(), "grow");
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0", "[]push[][]", ""));
        header.setOpaque(false);
        
        // Title
        JLabel title = new JLabel("👥 Quản lý khách hàng");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        header.add(title);
        
        // Search
        searchField = new JTextField(20);
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Tìm theo tên, SĐT...");
        searchField.addActionListener(e -> searchCustomers());
        header.add(searchField);
        
        // Add button
        JButton addBtn = new JButton("➕ Thêm khách hàng");
        addBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        addBtn.setBackground(PRIMARY);
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> showCustomerDialog(null));
        header.add(addBtn);
        
        return header;
    }
    
    private JPanel createContent() {
        JPanel content = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[][grow][]"));
        content.setOpaque(false);
        
        // Stats cards
        content.add(createStatsPanel(), "growx, wrap");
        
        // Table
        String[] columns = {"ID", "Tên khách hàng", "SĐT", "Hạng", "Điểm", "Tổng chi tiêu", "Số lần", "Thao tác"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column == 7; }
        };
        
        customerTable = new JTable(tableModel);
        customerTable.setRowHeight(45);
        customerTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        customerTable.getTableHeader().setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        
        // Custom renderer for tier column
        customerTable.getColumnModel().getColumn(3).setCellRenderer(new TierRenderer());
        
        // Action buttons
        customerTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer());
        customerTable.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor());
        
        // Column widths
        customerTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        customerTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        customerTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        customerTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        customerTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        customerTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        customerTable.getColumnModel().getColumn(6).setPreferredWidth(70);
        customerTable.getColumnModel().getColumn(7).setPreferredWidth(280);
        customerTable.getColumnModel().getColumn(7).setMinWidth(260);
        
        JScrollPane scroll = new JScrollPane(customerTable);
        scroll.setBorder(BorderFactory.createLineBorder(Color.decode(AppConfig.Colors.BORDER)));
        content.add(scroll, "grow, wrap");
        
        // Footer
        totalLabel = new JLabel();
        totalLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        totalLabel.setForeground(TEXT_SECONDARY);
        content.add(totalLabel);
        
        return content;
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 16", "[grow][grow][grow][grow]", ""));
        panel.setOpaque(false);
        
        // Total customers
        panel.add(createStatCard("👥", "Tổng khách", String.valueOf(customerService.getTotalCustomers()), PRIMARY), "grow");
        
        // Birthday this week
        int birthdayCount = customerService.getBirthdayCustomers().size();
        panel.add(createStatCard("🎂", "Sinh nhật tuần này", String.valueOf(birthdayCount), WARNING), "grow");
        
        // VIP count
        long vipCount = customerService.getAllCustomers().stream()
            .filter(c -> c.getTier() == CustomerTier.VIP || c.getTier() == CustomerTier.GOLD)
            .count();
        panel.add(createStatCard("⭐", "Khách VIP/Gold", String.valueOf(vipCount), SUCCESS), "grow");
        
        // Today's visits (placeholder)
        panel.add(createStatCard("📊", "Lượt ghé thăm", "Coming soon", TEXT_SECONDARY), "grow");
        
        return panel;
    }
    
    private JPanel createStatCard(String icon, String label, String value, Color color) {
        JPanel card = new JPanel(new MigLayout("wrap, insets 16", "[grow]", ""));
        card.setBackground(Color.WHITE);
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        card.setBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, color));
        
        JLabel iconLabel = new JLabel(icon + " " + label);
        iconLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        iconLabel.setForeground(TEXT_SECONDARY);
        card.add(iconLabel);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 24));
        valueLabel.setForeground(color);
        card.add(valueLabel);
        
        return card;
    }
    
    private void loadCustomers() {
        tableModel.setRowCount(0);
        List<Customer> customers = customerService.getAllCustomers();
        
        for (Customer c : customers) {
            tableModel.addRow(new Object[]{
                c.getId(),
                c.getFullName(),
                c.getPhone(),
                c.getTier(),
                c.getLoyaltyPoints(),
                currencyFormat.format(c.getTotalSpent()),
                c.getVisitCount(),
                c.getId() // For actions
            });
        }
        
        totalLabel.setText("Tổng: " + customers.size() + " khách hàng");
    }
    
    private void searchCustomers() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadCustomers();
            return;
        }
        
        tableModel.setRowCount(0);
        List<Customer> customers = customerService.searchCustomers(keyword);
        
        for (Customer c : customers) {
            tableModel.addRow(new Object[]{
                c.getId(),
                c.getFullName(),
                c.getPhone(),
                c.getTier(),
                c.getLoyaltyPoints(),
                currencyFormat.format(c.getTotalSpent()),
                c.getVisitCount(),
                c.getId()
            });
        }
        
        totalLabel.setText("Tìm thấy: " + customers.size() + " khách hàng");
    }
    
    private void showCustomerDialog(Customer customer) {
        boolean isNew = customer == null;
        if (isNew) customer = new Customer();
        
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), 
            isNew ? "Thêm khách hàng" : "Sửa khách hàng", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(450, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel content = new JPanel(new MigLayout("wrap 2, insets 20", "[][grow]", ""));
        content.setBackground(Color.WHITE);
        
        // Name
        content.add(new JLabel("Họ tên: *"));
        JTextField nameField = new JTextField(customer.getFullName(), 20);
        content.add(nameField, "growx");
        
        // Phone
        content.add(new JLabel("SĐT: *"));
        JTextField phoneField = new JTextField(customer.getPhone(), 15);
        content.add(phoneField, "growx");
        
        // Email
        content.add(new JLabel("Email:"));
        JTextField emailField = new JTextField(customer.getEmail(), 20);
        content.add(emailField, "growx");
        
        // Birthday
        content.add(new JLabel("Ngày sinh:"));
        JSpinner birthdaySpinner = new JSpinner(new SpinnerDateModel());
        birthdaySpinner.setEditor(new JSpinner.DateEditor(birthdaySpinner, "dd/MM/yyyy"));
        if (customer.getBirthday() != null) {
            birthdaySpinner.setValue(java.sql.Date.valueOf(customer.getBirthday()));
        }
        content.add(birthdaySpinner, "growx");
        
        // Gender
        content.add(new JLabel("Giới tính:"));
        JComboBox<String> genderCombo = new JComboBox<>(new String[]{"-- Chọn --", "Nam", "Nữ", "Khác"});
        if (customer.getGender() != null) {
            genderCombo.setSelectedItem(customer.getGender().getDisplayName());
        }
        content.add(genderCombo, "growx");
        
        // Address
        content.add(new JLabel("Địa chỉ:"));
        JTextField addressField = new JTextField(customer.getAddress(), 30);
        content.add(addressField, "growx");
        
        // Notes
        content.add(new JLabel("Ghi chú:"));
        JTextArea notesArea = new JTextArea(customer.getNotes(), 3, 20);
        notesArea.setLineWrap(true);
        content.add(new JScrollPane(notesArea), "growx");
        
        // If editing, show stats
        if (!isNew) {
            content.add(new JSeparator(), "span 2, growx, gaptop 16");
            
            JLabel statsLabel = new JLabel(String.format(
                "📊 Hạng: %s | Điểm: %d | Tổng chi: %s | Lượt ghé: %d",
                customer.getTier().getDisplayName(),
                customer.getLoyaltyPoints(),
                currencyFormat.format(customer.getTotalSpent()),
                customer.getVisitCount()
            ));
            statsLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.ITALIC, 11));
            statsLabel.setForeground(TEXT_SECONDARY);
            content.add(statsLabel, "span 2");
        }
        
        // Buttons
        JPanel buttons = new JPanel(new MigLayout("insets 0", "push[][]", ""));
        buttons.setOpaque(false);
        
        Customer finalCustomer = customer;
        JButton saveBtn = new JButton("💾 Lưu");
        saveBtn.setBackground(PRIMARY);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            
            if (name.isEmpty() || phone.isEmpty()) {
                ToastNotification.error(dialog, "Vui lòng nhập họ tên và SĐT!");
                return;
            }
            
            finalCustomer.setFullName(name);
            finalCustomer.setPhone(phone);
            finalCustomer.setEmail(emailField.getText().trim());
            finalCustomer.setAddress(addressField.getText().trim());
            finalCustomer.setNotes(notesArea.getText().trim());
            
            java.util.Date birthday = (java.util.Date) birthdaySpinner.getValue();
            finalCustomer.setBirthday(birthday.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            
            int genderIndex = genderCombo.getSelectedIndex();
            if (genderIndex > 0) {
                finalCustomer.setGender(Customer.Gender.values()[genderIndex - 1]);
            }
            
            boolean success;
            if (isNew) {
                success = customerService.createCustomer(finalCustomer);
            } else {
                success = customerService.updateCustomer(finalCustomer);
            }
            
            if (success) {
                ToastNotification.success(dialog, isNew ? "Đã thêm khách hàng!" : "Đã cập nhật!");
                dialog.dispose();
                loadCustomers();
            } else {
                ToastNotification.error(dialog, "Lỗi! SĐT có thể đã tồn tại.");
            }
        });
        buttons.add(saveBtn);
        
        JButton cancelBtn = new JButton("Hủy");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttons.add(cancelBtn);
        
        content.add(buttons, "span 2, growx, gaptop 16");
        
        dialog.add(content);
        dialog.setVisible(true);
    }
    
    // ==================== RENDERERS ====================
    
    private class TierRenderer extends JLabel implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            CustomerTier tier = (CustomerTier) value;
            setText(tier.getDisplayName());
            setHorizontalAlignment(CENTER);
            setOpaque(true);
            setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
            
            Color bgColor = switch (tier) {
                case VIP -> Color.decode("#9B59B6");
                case GOLD -> Color.decode("#F39C12");
                case SILVER -> Color.decode("#95A5A6");
                default -> Color.decode("#BDC3C7");
            };
            
            setBackground(isSelected ? table.getSelectionBackground() : bgColor);
            setForeground(Color.WHITE);
            
            return this;
        }
    }
    
    private JButton createActionButton(String text, Color bg, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.STYLE, "arc: 6; margin: 2,6,2,6");
        button.setToolTipText(tooltip);
        return button;
    }
    
    private class ButtonRenderer extends JPanel implements TableCellRenderer {
        JButton editBtn = createActionButton("Sửa", PRIMARY, "Sửa khách hàng");
        JButton bonusBtn = createActionButton("Tặng điểm", SUCCESS, "Tặng điểm thưởng");
        JButton deleteBtn = createActionButton("Xóa", DANGER, "Xóa khách hàng");
        
        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 4, 4));
            setOpaque(true);
            add(editBtn);
            add(bonusBtn);
            add(deleteBtn);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }
    
    private class ButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton editBtn, bonusBtn, deleteBtn;
        private int customerId;
        
        public ButtonEditor() {
            super(new JCheckBox());
            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            panel.setOpaque(true);
            panel.add(Box.createHorizontalGlue());
            
            editBtn = createActionButton("Sửa", PRIMARY, "Sửa khách hàng");
            editBtn.addActionListener(e -> {
                Customer c = customerService.getById(customerId);
                if (c != null) showCustomerDialog(c);
                fireEditingStopped();
            });
            
            bonusBtn = createActionButton("Tặng điểm", SUCCESS, "Tặng điểm thưởng");
            bonusBtn.addActionListener(e -> {
                String input = JOptionPane.showInputDialog(CustomerPanel.this, 
                    "Nhập số điểm thưởng:", "Tặng điểm", JOptionPane.PLAIN_MESSAGE);
                if (input != null && !input.isEmpty()) {
                    try {
                        int points = Integer.parseInt(input);
                        if (customerService.addBonusPoints(customerId, points, "Tặng điểm từ Manager")) {
                            ToastNotification.success(SwingUtilities.getWindowAncestor(CustomerPanel.this), 
                                "Đã tặng " + points + " điểm!");
                            loadCustomers();
                        }
                    } catch (NumberFormatException ex) {
                        ToastNotification.error(SwingUtilities.getWindowAncestor(CustomerPanel.this), 
                            "Số điểm không hợp lệ!");
                    }
                }
                fireEditingStopped();
            });
            
            deleteBtn = createActionButton("Xóa", DANGER, "Xóa khách hàng");
            deleteBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(CustomerPanel.this,
                    "Xác nhận xóa khách hàng?", "Xóa", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (customerService.deleteCustomer(customerId)) {
                        ToastNotification.success(SwingUtilities.getWindowAncestor(CustomerPanel.this), 
                            "Đã xóa khách hàng!");
                        loadCustomers();
                    }
                }
                fireEditingStopped();
            });
            
            panel.add(editBtn);
            panel.add(Box.createHorizontalStrut(4));
            panel.add(bonusBtn);
            panel.add(Box.createHorizontalStrut(4));
            panel.add(deleteBtn);
            panel.add(Box.createHorizontalGlue());
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, 
                boolean isSelected, int row, int column) {
            customerId = (int) value;
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return customerId;
        }
    }
}
