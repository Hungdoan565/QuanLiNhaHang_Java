package com.restaurant.view.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.SplitBillPart;
import com.restaurant.util.QRCodeGenerator;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Dialog showing QR code for a single split bill part
 */
public class TransferQRDialog extends JDialog {
    
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color SUCCESS = Color.decode(AppConfig.Colors.SUCCESS);
    
    private final SplitBillPart part;
    private final NumberFormat currencyFormat;
    private boolean confirmed = false;
    
    public TransferQRDialog(Window owner, SplitBillPart part) {
        super(owner, "Chuyển khoản - " + part.getDisplayLabel(), ModalityType.APPLICATION_MODAL);
        this.part = part;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        
        initializeUI();
        pack();
        setLocationRelativeTo(owner);
    }
    
    private void initializeUI() {
        JPanel content = new JPanel(new MigLayout("wrap, insets 24, gap 12", "[center, grow]", ""));
        content.setBackground(BACKGROUND);
        
        // Header
        JLabel header = new JLabel("📱 Thanh toán " + part.getDisplayLabel());
        header.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        header.setForeground(TEXT_PRIMARY);
        content.add(header, "center");
        
        // Amount
        JLabel amountLabel = new JLabel(currencyFormat.format(part.getAmount()));
        amountLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 28));
        amountLabel.setForeground(SUCCESS);
        content.add(amountLabel, "center, gaptop 8");
        
        // QR Code
        JPanel qrPanel = new JPanel(new MigLayout("insets 16", "[center]", "[center]"));
        qrPanel.setBackground(Color.WHITE);
        qrPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        
        String description = "SPLIT" + part.getPartNumber() + "_" + System.currentTimeMillis() % 10000;
        ImageIcon qrIcon = QRCodeGenerator.generateVietQR(part.getAmount(), description, 240);
        
        if (qrIcon != null) {
            JLabel qrImage = new JLabel(qrIcon);
            qrPanel.add(qrImage);
        } else {
            JLabel errorLabel = new JLabel("⚠️ Không thể tải QR");
            errorLabel.setPreferredSize(new Dimension(240, 240));
            errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            errorLabel.setForeground(Color.RED);
            qrPanel.add(errorLabel);
        }
        
        content.add(qrPanel, "center, gaptop 16");
        
        // Bank info with copy button
        JPanel bankPanel = new JPanel(new MigLayout("insets 0", "[]8[]", ""));
        bankPanel.setOpaque(false);
        
        JLabel bankLabel = new JLabel(QRCodeGenerator.getBankInfo());
        bankLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        bankLabel.setForeground(PRIMARY);
        bankPanel.add(bankLabel);
        
        JButton copyBtn = new JButton("📋 Copy");
        copyBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 11));
        copyBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        copyBtn.addActionListener(e -> {
            StringSelection selection = new StringSelection(QRCodeGenerator.getAccountNo());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            copyBtn.setText("✓ Đã copy");
            Timer timer = new Timer(2000, evt -> copyBtn.setText("📋 Copy"));
            timer.setRepeats(false);
            timer.start();
        });
        bankPanel.add(copyBtn);
        
        content.add(bankPanel, "center, gaptop 12");
        
        // Account name
        JLabel nameLabel = new JLabel("Chủ TK: " + QRCodeGenerator.getAccountName());
        nameLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        nameLabel.setForeground(TEXT_SECONDARY);
        content.add(nameLabel, "center");
        
        // Separator
        JSeparator sep = new JSeparator();
        content.add(sep, "growx, gaptop 16");
        
        // Actions
        JPanel actions = new JPanel(new MigLayout("insets 0", "[grow][][]", ""));
        actions.setOpaque(false);
        
        JLabel hint = new JLabel("Xác nhận khi đã nhận được tiền");
        hint.setFont(new Font(AppConfig.FONT_FAMILY, Font.ITALIC, 11));
        hint.setForeground(TEXT_SECONDARY);
        actions.add(hint);
        
        JButton cancelBtn = new JButton("Hủy");
        cancelBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        cancelBtn.setBackground(SURFACE);
        cancelBtn.setForeground(TEXT_PRIMARY);
        cancelBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        cancelBtn.addActionListener(e -> dispose());
        actions.add(cancelBtn);
        
        JButton confirmBtn = new JButton("✓ Đã nhận tiền");
        confirmBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        confirmBtn.setBackground(SUCCESS);
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        confirmBtn.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        actions.add(confirmBtn);
        
        content.add(actions, "growx, gaptop 8");
        
        setContentPane(content);
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
}
