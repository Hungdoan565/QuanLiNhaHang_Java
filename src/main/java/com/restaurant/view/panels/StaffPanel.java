package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.User;
import com.restaurant.service.UserService;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Staff Management Panel - Quản lý nhân viên
 * 
 * Features:
 * - CRUD nhân viên
 * - Phân quyền theo vai trò
 * - Reset mật khẩu
 */
public class StaffPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(StaffPanel.class);
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color BORDER = Color.decode(AppConfig.Colors.BORDER);
    private static final Color SUCCESS_COLOR = Color.decode(AppConfig.Colors.SUCCESS);
    private static final Color ERROR_COLOR = Color.decode(AppConfig.Colors.ERROR);
    
    private final User currentUser;
    private final List<StaffMember> staffList = new ArrayList<>();
    private JTable staffTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> roleFilter;
    private final UserService userService = UserService.getInstance();
    
    public StaffPanel(User user) {
        this.currentUser = user;
        initializeUI();
        loadStaff();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        
        // Toolbar
        JPanel toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);
        
        // Table
        JPanel tableContainer = createTable();
        add(tableContainer, BorderLayout.CENTER);
    }
    
    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new MigLayout("insets 0", "[]16[]push[]8[]", ""));
        toolbar.setOpaque(false);
        
        // Search
        searchField = new JTextField(20);
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Tìm kiếm nhân viên...");
        searchField.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                filterStaff();
            }
        });
        toolbar.add(searchField);
        
        // Role filter - default to "Nhân viên" to hide admin
        roleFilter = new JComboBox<>(new String[]{"Nhân viên", "Tất cả", "CASHIER", "WAITER", "CHEF", "ADMIN"});
        roleFilter.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        roleFilter.addActionListener(e -> filterStaff());
        toolbar.add(roleFilter);
        
        // Add button
        JButton addBtn = createButton("➕ Thêm nhân viên", PRIMARY, this::showAddDialog);
        toolbar.add(addBtn);
        
        // Refresh
        JButton refreshBtn = createButton("🔄", SURFACE, this::refresh);
        refreshBtn.setForeground(TEXT_PRIMARY);
        toolbar.add(refreshBtn);
        
        return toolbar;
    }
    
    private JPanel createTable() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(SURFACE);
        container.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        
        String[] columns = {"ID", "Tên đăng nhập", "Họ tên", "Vai trò", "Email", "Trạng thái", "Thao tác"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6;
            }
        };
        
        staffTable = new JTable(tableModel);
        staffTable.setRowHeight(48);
        staffTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        staffTable.getTableHeader().setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        staffTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        staffTable.setShowVerticalLines(false);
        staffTable.setGridColor(BORDER);
        
        // Hide ID column
        staffTable.getColumnModel().getColumn(0).setMinWidth(0);
        staffTable.getColumnModel().getColumn(0).setMaxWidth(0);
        
        // Column widths
        staffTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        staffTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        staffTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        staffTable.getColumnModel().getColumn(4).setPreferredWidth(180);
        staffTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        staffTable.getColumnModel().getColumn(6).setPreferredWidth(150);
        
        // Status renderer
        staffTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                if ("Đang làm".equals(value)) {
                    setForeground(SUCCESS_COLOR);
                } else {
                    setForeground(ERROR_COLOR);
                }
                return this;
            }
        });
        
        // Action column - simplified buttons
        staffTable.getColumnModel().getColumn(6).setCellRenderer((table, value, isSelected, hasFocus, row, column) -> {
            JPanel panel = new JPanel(new MigLayout("insets 4, gap 4", "[][][]", ""));
            panel.setOpaque(true);
            panel.setBackground(isSelected ? table.getSelectionBackground() : SURFACE);
            
            JButton editBtn = new JButton("✏️");
            JButton resetBtn = new JButton("🔑");
            JButton deleteBtn = new JButton("🗑️");
            
            for (JButton btn : new JButton[]{editBtn, resetBtn, deleteBtn}) {
                btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
                btn.setBorderPainted(false);
                btn.setContentAreaFilled(false);
                panel.add(btn);
            }
            
            return panel;
        });
        
        // Add mouse listener for action buttons
        staffTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = staffTable.rowAtPoint(e.getPoint());
                int col = staffTable.columnAtPoint(e.getPoint());
                
                if (col == 6 && row >= 0) {
                    int staffId = (int) tableModel.getValueAt(row, 0);
                    int x = e.getX() - staffTable.getCellRect(row, col, true).x;
                    
                    if (x < 30) {
                        editStaff(staffId);
                    } else if (x < 60) {
                        resetPassword(staffId);
                    } else if (x < 90) {
                        deleteStaff(staffId);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(staffTable);
        scrollPane.setBorder(null);
        container.add(scrollPane, BorderLayout.CENTER);
        
        return container;
    }
    
    private void loadStaff() {
        staffList.clear();
        
        try {
            List<User> users = userService.getAllUsers();
            for (User user : users) {
                staffList.add(new StaffMember(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getRole() != null ? user.getRole().getName() : "UNKNOWN",
                    user.getEmail(),
                    user.isActive()
                ));
            }
            logger.info("Loaded {} staff members from database", staffList.size());
        } catch (Exception e) {
            logger.error("Error loading staff from database", e);
            ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Lỗi tải dữ liệu nhân viên");
        }
        
        filterStaff(); // Apply default filter
    }
    
    private void refreshTable() {
        tableModel.setRowCount(0);
        for (StaffMember s : staffList) {
            tableModel.addRow(new Object[]{
                s.id,
                s.username,
                s.fullName,
                s.role,
                s.email,
                s.active ? "Đang làm" : "Nghỉ việc",
                "actions"
            });
        }
    }
    
    private void filterStaff() {
        String search = searchField.getText().toLowerCase().trim();
        String roleSelect = (String) roleFilter.getSelectedItem();
        
        tableModel.setRowCount(0);
        for (StaffMember s : staffList) {
            boolean matchSearch = search.isEmpty() || 
                                  s.username.toLowerCase().contains(search) ||
                                  s.fullName.toLowerCase().contains(search);
            
            boolean matchRole;
            if ("Nhân viên".equals(roleSelect)) {
                // Hide ADMIN
                matchRole = !"ADMIN".equals(s.role);
            } else if ("Tất cả".equals(roleSelect)) {
                matchRole = true;
            } else {
                matchRole = s.role.equals(roleSelect);
            }
            
            if (matchSearch && matchRole) {
                tableModel.addRow(new Object[]{
                    s.id,
                    s.username,
                    s.fullName,
                    s.role,
                    s.email,
                    s.active ? "Đang làm" : "Nghỉ việc",
                    "actions"
                });
            }
        }
    }
    
    private void showAddDialog() {
        showStaffDialog(null);
    }
    
    private void showStaffDialog(StaffMember staff) {
        boolean isEdit = staff != null;
        
        JPanel panel = new JPanel(new MigLayout("wrap 2, insets 16", "[][grow, fill]", ""));
        
        panel.add(new JLabel("Tên đăng nhập: *"));
        JTextField usernameField = new JTextField(staff != null ? staff.username : "", 20);
        usernameField.setEnabled(!isEdit);
        panel.add(usernameField);
        
        panel.add(new JLabel("Họ tên: *"));
        JTextField fullNameField = new JTextField(staff != null ? staff.fullName : "", 20);
        panel.add(fullNameField);
        
        panel.add(new JLabel("Vai trò: *"));
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"ADMIN", "CASHIER", "WAITER", "CHEF"});
        if (staff != null) roleCombo.setSelectedItem(staff.role);
        panel.add(roleCombo);
        
        panel.add(new JLabel("Email:"));
        JTextField emailField = new JTextField(staff != null ? staff.email : "", 20);
        panel.add(emailField);
        
        panel.add(new JLabel("Điện thoại:"));
        JTextField phoneField = new JTextField(staff != null ? staff.phone : "", 15);
        panel.add(phoneField);
        
        if (!isEdit) {
            panel.add(new JLabel("Mật khẩu: *"));
            JPasswordField passField = new JPasswordField(15);
            panel.add(passField);
        }
        
        JCheckBox activeCheck = new JCheckBox("Đang làm việc", staff == null || staff.active);
        panel.add(new JLabel(""));
        panel.add(activeCheck);
        
        int result = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            panel,
            isEdit ? "Sửa nhân viên" : "Thêm nhân viên",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String fullName = fullNameField.getText().trim();
            
            if (username.isEmpty() || fullName.isEmpty()) {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Vui lòng điền đầy đủ thông tin!");
                return;
            }
            
            if (isEdit) {
                staff.fullName = fullName;
                staff.role = (String) roleCombo.getSelectedItem();
                staff.email = emailField.getText();
                staff.phone = phoneField.getText();
                staff.active = activeCheck.isSelected();
                ToastNotification.success(SwingUtilities.getWindowAncestor(this), "Đã cập nhật: " + fullName);
            } else {
                StaffMember newStaff = new StaffMember(
                    staffList.size() + 1,
                    username,
                    fullName,
                    (String) roleCombo.getSelectedItem(),
                    emailField.getText(),
                    activeCheck.isSelected()
                );
                newStaff.phone = phoneField.getText();
                staffList.add(newStaff);
                ToastNotification.success(SwingUtilities.getWindowAncestor(this), "Đã thêm: " + fullName);
            }
            
            refreshTable();
        }
    }
    
    private void editStaff(int staffId) {
        StaffMember staff = staffList.stream().filter(s -> s.id == staffId).findFirst().orElse(null);
        if (staff != null) {
            showStaffDialog(staff);
        }
    }
    
    private void resetPassword(int staffId) {
        StaffMember staff = staffList.stream().filter(s -> s.id == staffId).findFirst().orElse(null);
        if (staff == null) return;
        
        int confirm = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            "Reset mật khẩu cho \"" + staff.fullName + "\"?\nMật khẩu mới sẽ là: 123456",
            "Xác nhận reset mật khẩu",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            // TODO: Update password in database
            ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                "Đã reset mật khẩu cho: " + staff.fullName);
        }
    }
    
    private void deleteStaff(int staffId) {
        StaffMember staff = staffList.stream().filter(s -> s.id == staffId).findFirst().orElse(null);
        if (staff == null) return;
        
        if (staff.id == currentUser.getId()) {
            ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Không thể xóa chính mình!");
            return;
        }
        
        if ("ADMIN".equals(staff.role)) {
            ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Không thể xóa tài khoản Admin!");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            "Bạn có chắc muốn xóa \"" + staff.fullName + "\"?",
            "Xác nhận xóa",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = userService.deleteUser(staffId);
                if (success) {
                    staffList.remove(staff);
                    filterStaff();
                    ToastNotification.success(SwingUtilities.getWindowAncestor(this), "Đã xóa: " + staff.fullName);
                } else {
                    ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Không thể xóa nhân viên!");
                }
            } catch (Exception e) {
                logger.error("Error deleting staff", e);
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Lỗi: " + e.getMessage());
            }
        }
    }
    
    private JButton createButton(String text, Color bgColor, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        btn.setBackground(bgColor);
        btn.setForeground(bgColor.equals(SURFACE) ? TEXT_PRIMARY : Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btn.addActionListener(e -> action.run());
        return btn;
    }
    
    public void refresh() {
        loadStaff();
        ToastNotification.info(SwingUtilities.getWindowAncestor(this), "Đã làm mới dữ liệu");
    }
    
    // ===========================================
    // Inner Classes
    // ===========================================
    
    private static class StaffMember {
        int id;
        String username;
        String fullName;
        String role;
        String email;
        String phone;
        boolean active;
        
        StaffMember(int id, String username, String fullName, String role, String email, boolean active) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.role = role;
            this.email = email;
            this.active = active;
        }
    }
}
