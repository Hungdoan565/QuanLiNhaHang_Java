package com.restaurant.util;

import javax.swing.*;
import java.awt.*;

/**
 * Toast notification utility for success/error messages
 * Replaces JOptionPane for non-blocking feedback
 */
public class ToastNotification {
    
    public enum ToastType {
        SUCCESS("#00B894"),
        ERROR("#E74C3C"),
        WARNING("#FDCB6E"),
        INFO("#0984E3");
        
        private final String colorHex;
        
        ToastType(String colorHex) {
            this.colorHex = colorHex;
        }
        
        public Color getColor() {
            return Color.decode(colorHex);
        }
        
        public Color getTextColor() {
            return this == WARNING ? Color.BLACK : Color.WHITE;
        }
    }
    
    private static final int TOAST_WIDTH = 300;
    private static final int TOAST_HEIGHT = 50;
    private static final int DURATION_MS = 3000;
    private static final int MARGIN = 20;
    
    /**
     * Show a toast notification at the bottom-right corner
     */
    public static void show(Component parent, String message, ToastType type) {
        // Find the parent frame
        Window window = SwingUtilities.getWindowAncestor(parent);
        if (window == null && parent instanceof Window) {
            window = (Window) parent;
        }
        
        if (window == null) {
            // Fallback: just show a dialog
            JOptionPane.showMessageDialog(parent, message);
            return;
        }
        
        final Window parentWindow = window;
        
        SwingUtilities.invokeLater(() -> {
            // Create toast panel
            JPanel toastPanel = new JPanel(new BorderLayout());
            toastPanel.setBackground(type.getColor());
            toastPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
            
            // Icon based on type
            String icon = switch (type) {
                case SUCCESS -> "✓";
                case ERROR -> "✕";
                case WARNING -> "⚠";
                case INFO -> "ℹ";
            };
            
            JLabel iconLabel = new JLabel(icon + "  ");
            iconLabel.setForeground(type.getTextColor());
            iconLabel.setFont(iconLabel.getFont().deriveFont(Font.BOLD, 16f));
            toastPanel.add(iconLabel, BorderLayout.WEST);
            
            JLabel messageLabel = new JLabel(message);
            messageLabel.setForeground(type.getTextColor());
            messageLabel.setFont(messageLabel.getFont().deriveFont(14f));
            toastPanel.add(messageLabel, BorderLayout.CENTER);
            
            // Create window
            JWindow toast = new JWindow(parentWindow);
            toast.setContentPane(toastPanel);
            toast.setSize(TOAST_WIDTH, TOAST_HEIGHT);
            
            // Position at bottom-right of parent
            int x = parentWindow.getX() + parentWindow.getWidth() - TOAST_WIDTH - MARGIN;
            int y = parentWindow.getY() + parentWindow.getHeight() - TOAST_HEIGHT - MARGIN - 40;
            toast.setLocation(x, y);
            
            // Add rounded corners
            toast.setBackground(new Color(0, 0, 0, 0));
            toastPanel.setOpaque(true);
            
            // Show toast
            toast.setVisible(true);
            
            // Auto-hide after duration
            Timer timer = new Timer(DURATION_MS, e -> {
                toast.dispose();
            });
            timer.setRepeats(false);
            timer.start();
        });
    }
    
    /**
     * Convenience methods
     */
    public static void success(Component parent, String message) {
        show(parent, message, ToastType.SUCCESS);
    }
    
    public static void error(Component parent, String message) {
        show(parent, message, ToastType.ERROR);
    }
    
    public static void warning(Component parent, String message) {
        show(parent, message, ToastType.WARNING);
    }
    
    public static void info(Component parent, String message) {
        show(parent, message, ToastType.INFO);
    }
}
