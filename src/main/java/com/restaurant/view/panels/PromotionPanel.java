package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.Promotion;
import com.restaurant.model.Promotion.PromotionType;
import com.restaurant.model.Customer.CustomerTier;
import com.restaurant.model.User;
import com.restaurant.service.PromotionService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Promotion Panel - Quản lý khuyến mãi
 */
public class PromotionPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(PromotionPanel.class);
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color SUCCESS = Color.decode(AppConfig.Colors.SUCCESS);
    private static final Color WARNING = Color.decode(AppConfig.Colors.WARNING);
    private static final Color ERROR = Color.decode(AppConfig.Colors.ERROR);
    private static final Color DANGER = Color.decode(AppConfig.Colors.ERROR);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    
    private final User currentUser;
    private final PromotionService promotionService;
    private final NumberFormat currencyFormat;
    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    private JTable promotionTable;
    private DefaultTableModel tableModel;
    private JLabel totalLabel;
    
    public PromotionPanel(User user) {
        this.currentUser = user;
        this.promotionService = PromotionService.getInstance();
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        
        initializeUI();
        loadPromotions();
    }
    
    private void initializeUI() {
        setLayout(new MigLayout("fill, insets 16", "[grow]", "[][grow]"));
        setBackground(BACKGROUND);
        
        add(createHeader(), "growx, wrap");
        add(createContent(), "grow");
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0", "[]push[]", ""));
        header.setOpaque(false);
        
        JLabel title = new JLabel("🎁 Quản lý khuyến mãi");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        header.add(title);
        
        JButton addBtn = new JButton("➕ Tạo khuyến mãi");
        addBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        addBtn.setBackground(PRIMARY);
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> showPromotionDialog(null));
        header.add(addBtn);
        
        return header;
    }
    
    private JPanel createContent() {
        JPanel content = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[][grow][]"));
        content.setOpaque(false);
        
        // Stats
        content.add(createStatsPanel(), "growx, wrap");
        
        // Table
        String[] columns = {"ID", "Mã", "Tên", "Loại", "Giá trị", "Thời gian", "Trạng thái", "Thao tác"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column == 7; }
        };
        
        promotionTable = new JTable(tableModel);
        promotionTable.setRowHeight(45);
        promotionTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        promotionTable.getTableHeader().setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        
        // Status renderer
        promotionTable.getColumnModel().getColumn(6).setCellRenderer(new StatusRenderer());
        
        // Action buttons
        promotionTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer());
        promotionTable.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor());
        
        // Column widths
        promotionTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        promotionTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        promotionTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        promotionTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        promotionTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        promotionTable.getColumnModel().getColumn(5).setPreferredWidth(150);
        promotionTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        promotionTable.getColumnModel().getColumn(7).setPreferredWidth(200);
        promotionTable.getColumnModel().getColumn(7).setMinWidth(180);
        
        JScrollPane scroll = new JScrollPane(promotionTable);
        scroll.setBorder(BorderFactory.createLineBorder(Color.decode(AppConfig.Colors.BORDER)));
        content.add(scroll, "grow, wrap");
        
        totalLabel = new JLabel();
        totalLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        totalLabel.setForeground(TEXT_SECONDARY);
        content.add(totalLabel);
        
        return content;
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 16", "[grow][grow][grow]", ""));
        panel.setOpaque(false);
        
        List<Promotion> all = promotionService.getAllPromotions();
        long active = all.stream().filter(p -> p.isActive() && p.isValid()).count();
        long expired = all.stream().filter(p -> !p.isValid()).count();
        
        panel.add(createStatCard("🎁", "Tổng KM", String.valueOf(all.size()), PRIMARY), "grow");
        panel.add(createStatCard("✅", "Đang áp dụng", String.valueOf(active), SUCCESS), "grow");
        panel.add(createStatCard("⏰", "Hết hạn/Dừng", String.valueOf(expired), WARNING), "grow");
        
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
    
    private void loadPromotions() {
        tableModel.setRowCount(0);
        List<Promotion> promotions = promotionService.getAllPromotions();
        
        for (Promotion p : promotions) {
            String valueText;
            switch (p.getType()) {
                case PERCENT:
                    valueText = p.getValue().intValue() + "%";
                    break;
                case FIXED:
                    valueText = currencyFormat.format(p.getValue());
                    break;
                default:
                    valueText = p.getValue().toString();
                    break;
            }
            
            String timeText = p.getStartDate().format(dtFormatter) + " - " + p.getEndDate().format(dtFormatter);
            
            tableModel.addRow(new Object[]{
                p.getId(),
                p.getCode() != null ? p.getCode() : "(Auto)",
                p.getName(),
                p.getType().getDisplayName(),
                valueText,
                timeText,
                p,  // For status rendering
                p.getId()
            });
        }
        
        totalLabel.setText("Tổng: " + promotions.size() + " khuyến mãi");
    }
    
    private void showPromotionDialog(Promotion promo) {
        boolean isNew = promo == null;
        if (isNew) promo = new Promotion();
        
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
            isNew ? "Tạo khuyến mãi" : "Sửa khuyến mãi", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);
        
        JPanel content = new JPanel(new MigLayout("wrap 2, insets 20", "[][grow]", ""));
        content.setBackground(Color.WHITE);
        
        // Code
        content.add(new JLabel("Mã (trống = auto):"));
        JTextField codeField = new JTextField(promo.getCode(), 15);
        content.add(codeField, "growx");
        
        // Name
        content.add(new JLabel("Tên: *"));
        JTextField nameField = new JTextField(promo.getName(), 20);
        content.add(nameField, "growx");
        
        // Type
        content.add(new JLabel("Loại: *"));
        JComboBox<PromotionType> typeCombo = new JComboBox<>(PromotionType.values());
        typeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PromotionType type) {
                    setText(type.getDisplayName());
                }
                return this;
            }
        });
        typeCombo.setSelectedItem(promo.getType() != null ? promo.getType() : PromotionType.PERCENT);
        content.add(typeCombo, "growx");
        
        // Value
        content.add(new JLabel("Giá trị: *"));
        JTextField valueField = new JTextField(promo.getValue() != null ? promo.getValue().toString() : "", 10);
        content.add(valueField, "growx");
        
        // Min order
        content.add(new JLabel("Đơn tối thiểu:"));
        JTextField minOrderField = new JTextField(promo.getMinOrderValue().toString(), 10);
        content.add(minOrderField, "growx");
        
        // Max discount
        content.add(new JLabel("Giảm tối đa:"));
        JTextField maxDiscountField = new JTextField(
            promo.getMaxDiscount() != null ? promo.getMaxDiscount().toString() : "", 10);
        content.add(maxDiscountField, "growx");
        
        // Start date
        content.add(new JLabel("Bắt đầu: *"));
        JSpinner startSpinner = new JSpinner(new SpinnerDateModel());
        startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "dd/MM/yyyy HH:mm"));
        if (promo.getStartDate() != null) {
            startSpinner.setValue(java.sql.Timestamp.valueOf(promo.getStartDate()));
        }
        content.add(startSpinner, "growx");
        
        // End date
        content.add(new JLabel("Kết thúc: *"));
        JSpinner endSpinner = new JSpinner(new SpinnerDateModel());
        endSpinner.setEditor(new JSpinner.DateEditor(endSpinner, "dd/MM/yyyy HH:mm"));
        if (promo.getEndDate() != null) {
            endSpinner.setValue(java.sql.Timestamp.valueOf(promo.getEndDate()));
        }
        content.add(endSpinner, "growx");
        
        // Hours
        content.add(new JLabel("Giờ áp dụng:"));
        JTextField hoursField = new JTextField(promo.getApplicableHours(), 15);
        hoursField.setToolTipText("VD: 11:00-14:00 hoặc * = cả ngày");
        content.add(hoursField, "growx");
        
        // Tier
        content.add(new JLabel("Hạng tối thiểu:"));
        JComboBox<Object> tierCombo = new JComboBox<>(new Object[]{
            "-- Tất cả --",
            CustomerTier.REGULAR,
            CustomerTier.SILVER,
            CustomerTier.GOLD,
            CustomerTier.VIP
        });
        tierCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof CustomerTier tier) {
                    setText(tier.getDisplayName());
                }
                return this;
            }
        });
        if (promo.getMinCustomerTier() != null) {
            tierCombo.setSelectedItem(promo.getMinCustomerTier());
        }
        content.add(tierCombo, "growx");
        
        // Usage limit
        content.add(new JLabel("Giới hạn lượt:"));
        JTextField limitField = new JTextField(
            promo.getUsageLimit() != null ? promo.getUsageLimit().toString() : "", 10);
        content.add(limitField, "growx");
        
        // Buttons
        JPanel buttons = new JPanel(new MigLayout("insets 0", "push[][]", ""));
        buttons.setOpaque(false);
        
        Promotion finalPromo = promo;
        JButton saveBtn = new JButton("💾 Lưu");
        saveBtn.setBackground(PRIMARY);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                ToastNotification.error(dialog, "Vui lòng nhập tên!");
                return;
            }
            
            try {
                finalPromo.setCode(codeField.getText().trim().isEmpty() ? null : codeField.getText().trim().toUpperCase());
                finalPromo.setName(name);
                finalPromo.setType((PromotionType) typeCombo.getSelectedItem());
                finalPromo.setValue(new BigDecimal(valueField.getText().trim()));
                finalPromo.setMinOrderValue(new BigDecimal(minOrderField.getText().trim()));
                
                String maxStr = maxDiscountField.getText().trim();
                finalPromo.setMaxDiscount(maxStr.isEmpty() ? null : new BigDecimal(maxStr));
                
                java.util.Date startDate = (java.util.Date) startSpinner.getValue();
                java.util.Date endDate = (java.util.Date) endSpinner.getValue();
                finalPromo.setStartDate(LocalDateTime.ofInstant(startDate.toInstant(), java.time.ZoneId.systemDefault()));
                finalPromo.setEndDate(LocalDateTime.ofInstant(endDate.toInstant(), java.time.ZoneId.systemDefault()));
                
                finalPromo.setApplicableHours(hoursField.getText().trim());
                
                Object tierSelected = tierCombo.getSelectedItem();
                finalPromo.setMinCustomerTier(tierSelected instanceof CustomerTier tier ? tier : null);
                
                String limitStr = limitField.getText().trim();
                finalPromo.setUsageLimit(limitStr.isEmpty() ? null : Integer.parseInt(limitStr));
                
                finalPromo.setCreatedBy(currentUser.getId());
                
                boolean success;
                if (isNew) {
                    success = promotionService.createPromotion(finalPromo);
                } else {
                    success = promotionService.updatePromotion(finalPromo);
                }
                
                if (success) {
                    ToastNotification.success(dialog, isNew ? "Đã tạo khuyến mãi!" : "Đã cập nhật!");
                    dialog.dispose();
                    loadPromotions();
                } else {
                    ToastNotification.error(dialog, "Lỗi khi lưu!");
                }
            } catch (Exception ex) {
                ToastNotification.error(dialog, "Dữ liệu không hợp lệ!");
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
    
    private class StatusRenderer extends JLabel implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Promotion promo = (Promotion) value;
            String status = promo.getStatusText();
            setText(status);
            setHorizontalAlignment(CENTER);
            setOpaque(true);
            setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
            
            Color bgColor;
            switch (status) {
                case "Đang áp dụng":
                    bgColor = PromotionPanel.SUCCESS;
                    break;
                case "Chưa bắt đầu":
                    bgColor = PromotionPanel.WARNING;
                    break;
                default:
                    bgColor = PromotionPanel.ERROR;
                    break;
            }
            
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
        JButton editBtn = createActionButton("Sửa", PRIMARY, "Sửa khuyến mãi");
        JButton deleteBtn = createActionButton("Xóa", DANGER, "Xóa khuyến mãi");
        
        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 4, 4));
            setOpaque(true);
            add(editBtn);
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
        private JButton editBtn, deleteBtn;
        private int promoId;
        
        public ButtonEditor() {
            super(new JCheckBox());
            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            panel.setOpaque(true);
            panel.add(Box.createHorizontalGlue());
            
            editBtn = createActionButton("Sửa", PRIMARY, "Sửa khuyến mãi");
            editBtn.addActionListener(e -> {
                Promotion p = promotionService.getById(promoId);
                if (p != null) showPromotionDialog(p);
                fireEditingStopped();
            });
            
            deleteBtn = createActionButton("Xóa", DANGER, "Xóa khuyến mãi");
            deleteBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(PromotionPanel.this,
                    "Xác nhận xóa khuyến mãi?", "Xóa", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (promotionService.deletePromotion(promoId)) {
                        ToastNotification.success(SwingUtilities.getWindowAncestor(PromotionPanel.this),
                            "Đã xóa!");
                        loadPromotions();
                    }
                }
                fireEditingStopped();
            });
            
            panel.add(editBtn);
            panel.add(Box.createHorizontalStrut(4));
            panel.add(deleteBtn);
            panel.add(Box.createHorizontalGlue());
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            promoId = (int) value;
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return promoId;
        }
    }
}
