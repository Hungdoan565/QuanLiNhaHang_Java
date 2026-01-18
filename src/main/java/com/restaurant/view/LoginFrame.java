package com.restaurant.view;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.User;
import com.restaurant.service.AuthService;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern Login Frame with split layout design
 * Left: Brand panel with gradient background
 * Right: Clean login form
 */
public class LoginFrame extends JFrame {
    
    private static final Logger logger = LogManager.getLogger(LoginFrame.class);
    
    // Colors
    private static final Color PRIMARY = Color.decode("#E85A4F");
    private static final Color PRIMARY_DARK = Color.decode("#C44536");
    private static final Color BACKGROUND = Color.decode("#F7F7F7");
    private static final Color SURFACE = Color.WHITE;
    private static final Color TEXT_PRIMARY = Color.decode("#2D3436");
    private static final Color TEXT_SECONDARY = Color.decode("#636E72");
    private static final Color BORDER = Color.decode("#DFE6E9");
    
    // Components
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    
    // Animation
    private Timer fadeInTimer;
    private float opacity = 0f;
    private String originalButtonText = "Đăng nhập";
    
    public LoginFrame() {
        initializeFrame();
        initializeComponents();
        setupKeyBindings();
        checkDatabaseConnection();
        startFadeInAnimation();
    }
    
    private void initializeFrame() {
        setTitle(AppConfig.APP_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true); // Must be undecorated to use setOpacity
        setSize(900, 600);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
        setResizable(true);
    }
    
    private void initializeComponents() {
        // Main container with split layout
        JPanel mainPanel = new JPanel(new GridLayout(1, 2));
        mainPanel.setBackground(BACKGROUND);
        
        // Add dragging support
        Point dragStart = new Point();
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart.x = e.getX();
                dragStart.y = e.getY();
            }
        });
        mainPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int x = e.getXOnScreen();
                int y = e.getYOnScreen();
                setLocation(x - dragStart.x, y - dragStart.y);
            }
        });
        
        // Left: Brand Panel
        JPanel brandPanel = createBrandPanel();
        mainPanel.add(brandPanel);
        
        // Right: Form Panel
        JPanel formPanel = createFormPanel();
        mainPanel.add(formPanel);
        
        // Add a subtle border for the undecorated window
        mainPanel.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        
        setContentPane(mainPanel);
    }
    
    /**
     * Create the left brand panel with gradient and illustrations
     */
    private JPanel createBrandPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 40", "[center]", "[center]")) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, PRIMARY,
                    getWidth(), getHeight(), PRIMARY_DARK
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Draw decorative circles
                g2d.setColor(new Color(255, 255, 255, 20));
                g2d.fillOval(-50, -50, 200, 200);
                g2d.fillOval(getWidth() - 100, getHeight() - 150, 250, 250);
                g2d.fillOval(getWidth() - 200, 100, 150, 150);
                
                // Draw food icons pattern (simplified)
                drawFoodPattern(g2d);
                
                g2d.dispose();
            }
            
            private void drawFoodPattern(Graphics2D g2d) {
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.setStroke(new BasicStroke(2));
                
                // Simple decorative elements
                int[][] positions = {{50, 150}, {80, 350}, {150, 480}, {300, 100}, {350, 400}};
                int[] sizes = {40, 30, 35, 45, 25};
                
                for (int i = 0; i < positions.length; i++) {
                    g2d.drawOval(positions[i][0], positions[i][1], sizes[i], sizes[i]);
                }
            }
        };
        panel.setOpaque(false);
        
        // Content container
        JPanel content = new JPanel(new MigLayout("wrap, align center", "[center]", "[]20[]10[]40[]"));
        content.setOpaque(false);
        
        // Logo icon (emoji as placeholder)
        JLabel logoIcon = new JLabel("🍽️");
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 72));
        content.add(logoIcon, "center");
        
        // App name
        JLabel appName = new JLabel(AppConfig.APP_NAME);
        appName.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 36));
        appName.setForeground(Color.WHITE);
        content.add(appName, "center");
        
        // Tagline
        JLabel tagline = new JLabel("Hệ thống Quản lý Nhà hàng");
        tagline.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 16));
        tagline.setForeground(new Color(255, 255, 255, 200));
        content.add(tagline, "center");
        
        // Features list
        JPanel features = createFeaturesList();
        content.add(features, "center, gaptop 20");
        
        panel.add(content);
        return panel;
    }
    
    /**
     * Create features list for brand panel
     */
    private JPanel createFeaturesList() {
        JPanel panel = new JPanel(new MigLayout("wrap, insets 0", "[left]", "[]8[]8[]"));
        panel.setOpaque(false);
        
        String[] items = {"✓ Quản lý bàn & order", "✓ Thanh toán nhanh chóng", "✓ Báo cáo doanh thu"};
        
        for (String item : items) {
            JLabel label = new JLabel(item);
            label.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
            label.setForeground(new Color(255, 255, 255, 180));
            panel.add(label);
        }
        
        return panel;
    }
    
    /**
     * Create the right form panel
     */
    private JPanel createFormPanel() {
        // Use "fill" to take up space, but align content "center" both horizontally and vertically
        JPanel panel = new JPanel(new MigLayout(
            "fill, insets 10 10 20 10", // Small top padding for close button
            "[grow, center]",
            "[]20[center]" // Close button at top, then content centered
        ));
        panel.setBackground(SURFACE);
        
        // Custom Close Button
        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        closeBtn.setForeground(TEXT_SECONDARY);
        closeBtn.setBorder(null);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(PRIMARY); }
            @Override
            public void mouseExited(MouseEvent e) { closeBtn.setForeground(TEXT_SECONDARY); }
        });
        panel.add(closeBtn, "dock north, align right, gapright 10, gaptop 10");

        // Create a wrapper for vertical grouping - REMOVED fillx to prevent stretching
        JPanel contentWrapper = new JPanel(new MigLayout("wrap, insets 0, align center", "[center]", "[]"));
        contentWrapper.setOpaque(false);
        
        // Welcome text
        JLabel welcomeLabel = new JLabel("Đăng nhập");
        welcomeLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 26)); // Slightly smaller font
        welcomeLabel.setForeground(TEXT_PRIMARY);
        contentWrapper.add(welcomeLabel, "gapbottom 5");
        
        JLabel subtitleLabel = new JLabel("Chào mừng bạn quay trở lại!");
        subtitleLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        contentWrapper.add(subtitleLabel, "gapbottom 25");
        
        // Form container with FIXED WIDTH (260px - narrower)
        JPanel formContainer = new JPanel(new MigLayout("wrap, insets 0, align center", "[260!]", ""));
        formContainer.setOpaque(false);
        
        // Username field
        JLabel usernameLabel = new JLabel("Tên đăng nhập");
        usernameLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        usernameLabel.setForeground(TEXT_PRIMARY);
        formContainer.add(usernameLabel, "align left, gapbottom 4");
        
        usernameField = new JTextField(20);
        styleTextField(usernameField, "Nhập tên đăng nhập");
        usernameField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, 
            createIcon("user"));
        formContainer.add(usernameField, "growx, h 40!, gapbottom 14"); // Height 40px
        
        // Password field
        JLabel passwordLabel = new JLabel("Mật khẩu");
        passwordLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        passwordLabel.setForeground(TEXT_PRIMARY);
        formContainer.add(passwordLabel, "align left, gapbottom 4");
        
        passwordField = new JPasswordField(20);
        styleTextField(passwordField, "Nhập mật khẩu");
        passwordField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, 
            createIcon("lock"));
        passwordField.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 8; " +
            "borderWidth: 1; " +
            "focusWidth: 1; " +
            "innerFocusWidth: 0; " +
            "padding: 8, 10, 8, 10; " + // Adjusted padding
            "focusColor: " + colorToHex(PRIMARY) + "; " +
            "borderColor: #bdc3c7; " +
            "showRevealButton: true"
        );
        formContainer.add(passwordField, "growx, h 40!, gapbottom 18"); // Height 40px
        
        // Login button
        loginButton = new JButton("Đăng nhập");
        styleLoginButton(loginButton);
        loginButton.addActionListener(this::handleLogin);
        formContainer.add(loginButton, "growx, h 40!, gapbottom 10"); // Height 40px
        
        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        formContainer.add(statusLabel, "growx, center");
        
        contentWrapper.add(formContainer, "center");
        panel.add(contentWrapper, "center");
        
        // Footer (dock south in main panel)
        JPanel footer = createFooter();
        panel.add(footer, "dock south, center");
        
        return panel;
    }
    
    /**
     * Create custom vector icon for better quality
     */
    private Icon createIcon(String type) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.decode("#95a5a6")); // Gray color
                
                int w = getIconWidth();
                int h = getIconHeight();
                int cx = x + w/2;
                int cy = y + h/2;
                
                if ("user".equals(type)) {
                    // Draw user icon
                    g2d.drawOval(cx - 5, cy - 7, 10, 10); // Head
                    g2d.drawArc(cx - 8, cy + 1, 16, 12, 0, 180); // Shoulders (inverted arc)
                } else if ("lock".equals(type)) {
                    // Draw lock icon
                    g2d.drawRoundRect(cx - 6, cy - 2, 12, 10, 2, 2); // Body
                    g2d.drawArc(cx - 4, cy - 7, 8, 10, 0, 180); // Shackle
                    g2d.fillOval(cx - 1, cy + 2, 2, 3); // Keyhole
                }
                
                g2d.dispose();
            }
            
            @Override
            public int getIconWidth() {
                return 24;
            }
            
            @Override
            public int getIconHeight() {
                return 24;
            }
        };
    }
    
    /**
     * Style text field with modern look - Thinner borders, subtle focus
     */
    private void styleTextField(JTextField field, String placeholder) {
        field.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 14));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 8; " + // Smaller corner radius
            "borderWidth: 1; " +
            "focusWidth: 1; " + // Thinner focus ring
            "innerFocusWidth: 0; " +
            "padding: 10, 12, 10, 12; " +
            "focusColor: " + colorToHex(PRIMARY) + "; " +
            "borderColor: #bdc3c7"
        );
    }
    
    /**
     * Style login button - polished look
     */
    private void styleLoginButton(JButton button) {
        button.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 15));
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, "roundRect");
        button.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 8; " +
            "background: " + colorToHex(PRIMARY) + "; " +
            "hoverBackground: " + colorToHex(PRIMARY_DARK) + "; " +
            "pressedBackground: " + colorToHex(PRIMARY_DARK) + "; " +
            "shadowWidth: 0" // Flat look
        );
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(PRIMARY_DARK);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(PRIMARY);
            }
        });
    }
    
    /**
     * Create footer with version info
     */
    private JPanel createFooter() {
        JPanel footer = new JPanel(new MigLayout("insets 15", "[center]", ""));
        footer.setOpaque(false);
        
        JLabel versionLabel = new JLabel("v" + AppConfig.APP_VERSION + " | © 2026 RestaurantPOS");
        versionLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 11));
        versionLabel.setForeground(TEXT_SECONDARY);
        footer.add(versionLabel);
        
        return footer;
    }
    
    private void setupKeyBindings() {
        getRootPane().setDefaultButton(loginButton);
        
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear");
        getRootPane().getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearFields();
            }
        });
    }
    
    private void checkDatabaseConnection() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return DatabaseConnection.testConnection();
            }
            
            @Override
            protected void done() {
                try {
                    boolean connected = get();
                    if (!connected) {
                        showStatus("❌ Không thể kết nối Database", true);
                        loginButton.setEnabled(false);
                    } else {
                        showStatus("✅ Sẵn sàng", false);
                        Timer timer = new Timer(2000, e -> statusLabel.setText(" "));
                        timer.setRepeats(false);
                        timer.start();
                    }
                } catch (Exception e) {
                    showStatus("❌ Lỗi: " + e.getMessage(), true);
                    loginButton.setEnabled(false);
                }
            }
        };
        worker.execute();
    }
    
    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        // Validate input
        if (username.isEmpty()) {
            showStatus("⚠️ Vui lòng nhập tên đăng nhập", true);
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showStatus("⚠️ Vui lòng nhập mật khẩu", true);
            passwordField.requestFocus();
            return;
        }
        
        // Show loading state
        showButtonLoading(true);
        statusLabel.setText(" ");
        
        SwingWorker<AuthService.LoginResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthService.LoginResult doInBackground() {
                return AuthService.getInstance().login(username, password);
            }
            
            @Override
            protected void done() {
                try {
                    AuthService.LoginResult result = get();
                    
                    if (result.isSuccess()) {
                        showStatus("✅ Đăng nhập thành công!", false);
                        ToastNotification.success(LoginFrame.this, "Đăng nhập thành công!");
                        logger.info("Login successful, opening main window");
                        
                        // Delay to show success state before transition
                        Timer timer = new Timer(500, evt -> openMainFrame(result.getUser()));
                        timer.setRepeats(false);
                        timer.start();
                    } else {
                        showStatus("❌ " + result.getMessage(), true);
                        ToastNotification.error(LoginFrame.this, result.getMessage());
                        passwordField.setText("");
                        passwordField.requestFocus();
                        showButtonLoading(false);
                    }
                    
                } catch (Exception ex) {
                    logger.error("Login error", ex);
                    showStatus("❌ Lỗi hệ thống", true);
                    ToastNotification.error(LoginFrame.this, "Lỗi: " + ex.getMessage());
                    showButtonLoading(false);
                }
            }
        };
        worker.execute();
    }
    
    private void openMainFrame(User user) {
        // Close login frame
        dispose();
        
        // Open main frame
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(user);
            mainFrame.setVisible(true);
        });
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? Color.decode("#E74C3C") : Color.decode("#00B894"));
    }
    
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        statusLabel.setText(" ");
        usernameField.requestFocus();
    }
    
    /**
     * Start fade-in animation when frame becomes visible
     */
    private void startFadeInAnimation() {
        // Set initial opacity
        setOpacity(0f);
        
        fadeInTimer = new Timer(16, e -> { // ~60fps
            opacity += 0.05f;
            if (opacity >= 1f) {
                opacity = 1f;
                setOpacity(1f);
                fadeInTimer.stop();
            } else {
                setOpacity(opacity);
            }
        });
        
        // Start animation after frame is shown
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                fadeInTimer.start();
            }
        });
    }
    
    /**
     * Show loading animation on button
     */
    private void showButtonLoading(boolean loading) {
        if (loading) {
            loginButton.setEnabled(false);
            loginButton.setText("⏳ Đang xử lý...");
            loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            loginButton.setEnabled(true);
            loginButton.setText(originalButtonText);
            loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    /**
     * Convert Color to hex string
     */
    private String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
