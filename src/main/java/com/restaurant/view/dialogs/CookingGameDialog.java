package com.restaurant.view.dialogs;

import com.restaurant.util.KitchenOrderManager.KitchenOrder;
import com.restaurant.util.KitchenOrderManager.OrderItem;
import com.restaurant.view.components.CookingCardPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * CookingGameDialog - Modal dialog for cooking game interface
 * Shows detailed cooking steps and ingredient checklist for a single item
 */
public class CookingGameDialog extends JDialog {
    
    private static final Color BACKGROUND = Color.decode("#0D1117");
    
    public CookingGameDialog(Window parent, KitchenOrder order, OrderItem item, 
                              boolean isViewOnly, Runnable onComplete) {
        super(parent, "🎮 Nấu: " + item.getName(), ModalityType.APPLICATION_MODAL);
        
        setSize(450, 600);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        JPanel contentPanel = new JPanel(new MigLayout("fill, insets 16", "[grow]", "[grow][]"));
        contentPanel.setBackground(BACKGROUND);
        
        // Cooking card panel
        CookingCardPanel cookingCard = new CookingCardPanel(order, item, isViewOnly, () -> {
            if (onComplete != null) {
                onComplete.run();
            }
            dispose();
        });
        
        JScrollPane scrollPane = new JScrollPane(cookingCard);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        contentPanel.add(scrollPane, "grow, wrap");
        
        // Close button
        JButton closeBtn = new JButton("✕ Đóng");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        closeBtn.setBackground(Color.decode("#374151"));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        contentPanel.add(closeBtn, "center, h 36!, w 120!");
        
        setContentPane(contentPanel);
    }
    
    /**
     * Show the cooking game dialog
     */
    public static void showDialog(Component parent, KitchenOrder order, OrderItem item,
                                   boolean isViewOnly, Runnable onComplete) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        CookingGameDialog dialog = new CookingGameDialog(window, order, item, isViewOnly, onComplete);
        dialog.setVisible(true);
    }
}
