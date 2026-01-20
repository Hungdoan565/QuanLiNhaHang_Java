package com.restaurant.view.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.Order;
import com.restaurant.model.SplitBill;
import com.restaurant.model.SplitBillPart;
import com.restaurant.service.ServiceResult;
import com.restaurant.service.SplitBillService;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Split Bill Dialog
 * Allows cashier to split order payment among multiple people
 */
public class SplitBillDialog extends JDialog {
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color SUCCESS = Color.decode(AppConfig.Colors.SUCCESS);
    private static final Color WARNING = Color.decode(AppConfig.Colors.WARNING);
    private static final Color ERROR = Color.decode(AppConfig.Colors.ERROR);
    
    private final Order order;
    private final NumberFormat currencyFormat;
    private final SplitBillService splitBillService;
    private final Consumer<SplitBill> onComplete;
    
    private SplitBill currentSplit;
    private JPanel partsPanel;
    private JLabel summaryLabel;
    private JSpinner splitCountSpinner;
    
    public SplitBillDialog(Window owner, Order order, Consumer<SplitBill> onComplete) {
        super(owner, "Chia bill - " + order.getOrderCode(), ModalityType.APPLICATION_MODAL);
        this.order = order;
        this.onComplete = onComplete;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        this.splitBillService = SplitBillService.getInstance();
        
        initializeUI();
        pack();
        setLocationRelativeTo(owner);
    }
    
    private void initializeUI() {
        setLayout(new MigLayout("fill, wrap, insets 20", "[500!]", "[][][grow][]"));
        getContentPane().setBackground(BACKGROUND);
        
        // Header
        add(createHeader(), "growx");
        
        // Split configuration
        add(createSplitConfig(), "growx");
        
        // Parts list
        JScrollPane scrollPane = new JScrollPane(createPartsPanel());
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, "grow, h 300!");
        
        // Summary & Actions
        add(createFooter(), "growx");
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0", "[]push[]", ""));
        header.setOpaque(false);
        
        JLabel title = new JLabel("💳 Chia bill đơn hàng");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 20));
        title.setForeground(TEXT_PRIMARY);
        header.add(title);
        
        JLabel total = new JLabel("Tổng: " + currencyFormat.format(order.getTotalAmount()));
        total.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        total.setForeground(SUCCESS);
        header.add(total);
        
        return header;
    }
    
    private JPanel createSplitConfig() {
        JPanel panel = new JPanel(new MigLayout("insets 12, gap 16", "[][][grow][]", ""));
        panel.setBackground(SURFACE);
        panel.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        
        // Split type tabs (simplified to Quick Split for MVP)
        JLabel modeLabel = new JLabel("Chế độ:");
        modeLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        modeLabel.setForeground(TEXT_PRIMARY);
        panel.add(modeLabel);
        
        JToggleButton equalBtn = new JToggleButton("⚖️ Chia đều");
        equalBtn.setSelected(true);
        equalBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        equalBtn.setBackground(PRIMARY);
        equalBtn.setForeground(Color.WHITE);
        equalBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        panel.add(equalBtn);
        
        panel.add(new JLabel()); // Spacer
        
        // Split count
        JLabel countLabel = new JLabel("Số người:");
        countLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        countLabel.setForeground(TEXT_PRIMARY);
        panel.add(countLabel, "split 2");
        
        splitCountSpinner = new JSpinner(new SpinnerNumberModel(2, 2, 20, 1));
        splitCountSpinner.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        splitCountSpinner.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        ((JSpinner.DefaultEditor) splitCountSpinner.getEditor()).getTextField().setColumns(3);
        splitCountSpinner.addChangeListener(e -> updateSplit());
        panel.add(splitCountSpinner);
        
        return panel;
    }
    
    private JPanel createPartsPanel() {
        partsPanel = new JPanel(new MigLayout("wrap, insets 8, gapy 8", "[grow]", ""));
        partsPanel.setOpaque(false);
        
        // Initialize with default split
        updateSplit();
        
        return partsPanel;
    }
    
    private void updateSplit() {
        int splitCount = (Integer) splitCountSpinner.getValue();
        
        ServiceResult<SplitBill> result = splitBillService.createEqualSplit(order, splitCount);
        if (!result.isSuccess()) {
            ToastNotification.error(this, result.getMessage());
            return;
        }
        
        currentSplit = result.getData();
        refreshPartsDisplay();
    }
    
    private void refreshPartsDisplay() {
        partsPanel.removeAll();
        
        for (SplitBillPart part : currentSplit.getParts()) {
            partsPanel.add(createPartCard(part), "growx");
        }
        
        updateSummary();
        partsPanel.revalidate();
        partsPanel.repaint();
    }
    
    private JPanel createPartCard(SplitBillPart part) {
        JPanel card = new JPanel(new MigLayout("insets 12", "[][grow][][]", ""));
        card.setBackground(part.isPaid() ? 
            new Color(SUCCESS.getRed(), SUCCESS.getGreen(), SUCCESS.getBlue(), 30) : SURFACE);
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        
        // Part number/name
        JLabel numberLabel = new JLabel("👤 " + part.getDisplayLabel());
        numberLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        numberLabel.setForeground(TEXT_PRIMARY);
        card.add(numberLabel);
        
        // Name input (optional)
        JTextField nameField = new JTextField(part.getPayerName() != null ? part.getPayerName() : "");
        nameField.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        nameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tên người trả...");
        nameField.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        nameField.setEnabled(!part.isPaid());
        nameField.addActionListener(e -> part.setPayerName(nameField.getText().trim()));
        card.add(nameField, "w 120!");
        
        // Amount
        JLabel amountLabel = new JLabel(currencyFormat.format(part.getAmount()));
        amountLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        amountLabel.setForeground(part.isPaid() ? SUCCESS : PRIMARY);
        card.add(amountLabel);
        
        // Pay button or status
        if (part.isPaid()) {
            JLabel statusLabel = new JLabel("✅ " + part.getPaymentMethod());
            statusLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
            statusLabel.setForeground(SUCCESS);
            card.add(statusLabel);
        } else {
            JButton payBtn = new JButton("💵 Thanh toán");
            payBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
            payBtn.setBackground(SUCCESS);
            payBtn.setForeground(Color.WHITE);
            payBtn.setBorderPainted(false);
            payBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
            payBtn.addActionListener(e -> payPart(part));
            card.add(payBtn);
        }
        
        return card;
    }
    
    private void payPart(SplitBillPart part) {
        // Show payment method selector
        String[] methods = {"CASH", "CARD", "TRANSFER"};
        String[] displayMethods = {"💵 Tiền mặt", "💳 Thẻ", "📱 Chuyển khoản"};
        
        int choice = JOptionPane.showOptionDialog(this,
            "Chọn phương thức thanh toán cho " + part.getDisplayLabel() + "\n" +
            "Số tiền: " + currencyFormat.format(part.getAmount()),
            "Thanh toán phần " + part.getPartNumber(),
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            displayMethods,
            displayMethods[0]);
        
        if (choice >= 0) {
            ServiceResult<SplitBillPart> result = splitBillService.payPart(
                currentSplit, part.getPartNumber(), methods[choice]);
            
            if (result.isSuccess()) {
                ToastNotification.success(this, result.getMessage());
                refreshPartsDisplay();
                
                // Check if all paid
                if (currentSplit.isFullyPaid()) {
                    handleComplete();
                }
            } else {
                ToastNotification.error(this, result.getMessage());
            }
        }
    }
    
    private JPanel createFooter() {
        JPanel footer = new JPanel(new MigLayout("insets 12 0 0 0", "[]push[][]", ""));
        footer.setOpaque(false);
        
        // Summary
        summaryLabel = new JLabel("Chờ thanh toán...");
        summaryLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        summaryLabel.setForeground(TEXT_SECONDARY);
        footer.add(summaryLabel);
        
        // Cancel button
        JButton cancelBtn = new JButton("Hủy");
        cancelBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        cancelBtn.setBackground(SURFACE);
        cancelBtn.setForeground(TEXT_PRIMARY);
        cancelBtn.setBorderPainted(false);
        cancelBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        cancelBtn.addActionListener(e -> dispose());
        footer.add(cancelBtn);
        
        // Complete button
        JButton completeBtn = new JButton("✓ Hoàn tất");
        completeBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        completeBtn.setBackground(SUCCESS);
        completeBtn.setForeground(Color.WHITE);
        completeBtn.setBorderPainted(false);
        completeBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        completeBtn.addActionListener(e -> {
            if (!currentSplit.isFullyPaid()) {
                ToastNotification.warning(this, "Vẫn còn phần chưa thanh toán!");
                return;
            }
            handleComplete();
        });
        footer.add(completeBtn);
        
        return footer;
    }
    
    private void updateSummary() {
        if (summaryLabel == null) return; // Not yet initialized
        
        if (currentSplit != null) {
            summaryLabel.setText(splitBillService.getSummaryText(currentSplit));
            summaryLabel.setForeground(currentSplit.isFullyPaid() ? SUCCESS : WARNING);
        }
    }
    
    private void handleComplete() {
        if (onComplete != null) {
            onComplete.accept(currentSplit);
        }
        ToastNotification.success(this, "🎉 Đã thanh toán đủ tất cả các phần!");
        dispose();
    }
}
