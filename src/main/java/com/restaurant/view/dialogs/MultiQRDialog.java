package com.restaurant.view.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.SplitBill;
import com.restaurant.model.SplitBillPart;
import com.restaurant.service.ServiceResult;
import com.restaurant.service.SplitBillService;
import com.restaurant.util.QRCodeGenerator;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dialog showing grid of QR codes for all unpaid split bill parts
 */
public class MultiQRDialog extends JDialog {
    
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color SUCCESS = Color.decode(AppConfig.Colors.SUCCESS);
    
    private final SplitBill splitBill;
    private final SplitBillService splitBillService;
    private final NumberFormat currencyFormat;
    private final Runnable onUpdate;
    
    private JPanel qrGridPanel;
    private JLabel headerLabel;
    private Map<Integer, JPanel> partCards = new HashMap<>();
    
    public MultiQRDialog(Window owner, SplitBill splitBill, Runnable onUpdate) {
        super(owner, "Tất cả QR chuyển khoản", ModalityType.APPLICATION_MODAL);
        this.splitBill = splitBill;
        this.splitBillService = SplitBillService.getInstance();
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        this.onUpdate = onUpdate;
        
        initializeUI();
        pack();
        setLocationRelativeTo(owner);
    }
    
    private void initializeUI() {
        JPanel content = new JPanel(new MigLayout("wrap, insets 20", "[grow]", "[][grow][]"));
        content.setBackground(BACKGROUND);
        
        // Header
        JPanel header = new JPanel(new MigLayout("insets 0", "[]push[]", ""));
        header.setOpaque(false);
        
        headerLabel = new JLabel();
        updateHeaderText();
        headerLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        headerLabel.setForeground(TEXT_PRIMARY);
        header.add(headerLabel);
        
        JLabel bankInfo = new JLabel("📌 " + QRCodeGenerator.getBankInfo());
        bankInfo.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        bankInfo.setForeground(PRIMARY);
        header.add(bankInfo);
        
        content.add(header, "growx");
        
        // QR Grid
        qrGridPanel = new JPanel(new MigLayout("wrap 2, gap 16", "[grow][grow]", ""));
        qrGridPanel.setOpaque(false);
        
        refreshQRGrid();
        
        JScrollPane scrollPane = new JScrollPane(qrGridPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setPreferredSize(new Dimension(580, 450));
        content.add(scrollPane, "grow");
        
        // Footer
        JPanel footer = new JPanel(new MigLayout("insets 12 0 0 0", "[]push[]", ""));
        footer.setOpaque(false);
        
        JLabel hint = new JLabel("💡 Nhấn \"Đã nhận\" khi xác nhận từng người chuyển");
        hint.setFont(new Font(AppConfig.FONT_FAMILY, Font.ITALIC, 11));
        hint.setForeground(TEXT_SECONDARY);
        footer.add(hint);
        
        JButton closeBtn = new JButton("Đóng");
        closeBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        closeBtn.setBackground(SURFACE);
        closeBtn.setForeground(TEXT_PRIMARY);
        closeBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        
        content.add(footer, "growx");
        
        setContentPane(content);
    }
    
    private void updateHeaderText() {
        List<SplitBillPart> unpaid = splitBill.getParts().stream()
            .filter(p -> !p.isPaid())
            .collect(Collectors.toList());
        headerLabel.setText("📱 Tất cả QR chuyển khoản (" + unpaid.size() + " phần chưa thanh toán)");
    }
    
    private void refreshQRGrid() {
        qrGridPanel.removeAll();
        partCards.clear();
        
        List<SplitBillPart> unpaidParts = splitBill.getParts().stream()
            .filter(p -> !p.isPaid())
            .collect(Collectors.toList());
        
        if (unpaidParts.isEmpty()) {
            JLabel doneLabel = new JLabel("✅ Tất cả đã thanh toán!");
            doneLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
            doneLabel.setForeground(SUCCESS);
            qrGridPanel.add(doneLabel, "span 2, center");
        } else {
            for (SplitBillPart part : unpaidParts) {
                JPanel card = createQRCard(part);
                partCards.put(part.getPartNumber(), card);
                qrGridPanel.add(card, "grow");
            }
        }
        
        updateHeaderText();
        qrGridPanel.revalidate();
        qrGridPanel.repaint();
    }
    
    private JPanel createQRCard(SplitBillPart part) {
        JPanel card = new JPanel(new MigLayout("wrap, insets 12", "[center, grow]", ""));
        card.setBackground(SURFACE);
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        
        // Part label
        JLabel partLabel = new JLabel("👤 " + part.getDisplayLabel());
        partLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        partLabel.setForeground(TEXT_PRIMARY);
        card.add(partLabel, "center");
        
        // Amount
        JLabel amountLabel = new JLabel(currencyFormat.format(part.getAmount()));
        amountLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        amountLabel.setForeground(PRIMARY);
        card.add(amountLabel, "center, gaptop 4");
        
        // QR Code (smaller for grid)
        String description = "SPLIT" + part.getPartNumber() + "_" + System.currentTimeMillis() % 10000;
        ImageIcon qrIcon = QRCodeGenerator.generateVietQR(part.getAmount(), description, 180);
        
        JPanel qrWrapper = new JPanel(new MigLayout("insets 8", "[center]", "[center]"));
        qrWrapper.setBackground(Color.WHITE);
        qrWrapper.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        
        if (qrIcon != null) {
            qrWrapper.add(new JLabel(qrIcon));
        } else {
            JLabel errorLabel = new JLabel("⚠️");
            errorLabel.setPreferredSize(new Dimension(180, 180));
            errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            qrWrapper.add(errorLabel);
        }
        
        card.add(qrWrapper, "center, gaptop 8");
        
        // Confirm button
        JButton confirmBtn = new JButton("✓ Đã nhận tiền");
        confirmBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        confirmBtn.setBackground(SUCCESS);
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        confirmBtn.addActionListener(e -> confirmPayment(part));
        card.add(confirmBtn, "center, gaptop 8, growx");
        
        return card;
    }
    
    private void confirmPayment(SplitBillPart part) {
        ServiceResult<SplitBillPart> result = splitBillService.payPart(
            splitBill, part.getPartNumber(), "TRANSFER");
        
        if (result.isSuccess()) {
            ToastNotification.success(this, result.getMessage());
            
            // Update parent if callback provided
            if (onUpdate != null) {
                onUpdate.run();
            }
            
            // Refresh grid
            refreshQRGrid();
            
            // Check if all paid
            if (splitBill.isFullyPaid()) {
                ToastNotification.success(this, "🎉 Tất cả đã thanh toán!");
                dispose();
            }
        } else {
            ToastNotification.error(this, result.getMessage());
        }
    }
}
