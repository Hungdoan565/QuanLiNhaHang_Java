package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.LeaveRequest;
import com.restaurant.model.LeaveRequest.LeaveStatus;
import com.restaurant.model.ShiftTemplate;
import com.restaurant.model.User;
import com.restaurant.model.WorkSchedule;
import com.restaurant.service.ScheduleService;
import com.restaurant.service.UserService;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Schedule Management Panel - Quản lý lịch làm việc
 * For Admin and Manager only
 */
public class ScheduleManagementPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(ScheduleManagementPanel.class);
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color SUCCESS = Color.decode(AppConfig.Colors.SUCCESS);
    private static final Color WARNING = Color.decode(AppConfig.Colors.WARNING);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    
    private final User currentUser;
    private final ScheduleService scheduleService;
    private final UserService userService;
    
    // State
    private LocalDate currentWeekStart;
    private List<User> staffList;
    private List<ShiftTemplate> shiftTemplates;
    
    // UI Components
    private JLabel weekLabel;
    private JPanel calendarGrid;
    private JTable leaveRequestsTable;
    private DefaultTableModel leaveTableModel;
    
    public ScheduleManagementPanel(User user) {
        this.currentUser = user;
        this.scheduleService = ScheduleService.getInstance();
        this.userService = UserService.getInstance();
        this.currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        initializeUI();
        loadData();
        
        // Auto-refresh leave requests every 5 seconds for near-real-time updates
        javax.swing.Timer refreshTimer = new javax.swing.Timer(5000, e -> {
            loadLeaveRequests();
        });
        refreshTimer.start();
    }
    
    private void initializeUI() {
        setLayout(new MigLayout("fill, insets 16", "[grow]", "[][grow]"));
        setBackground(BACKGROUND);
        
        // Header
        add(createHeader(), "growx, wrap");
        
        // Main content with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        tabbedPane.putClientProperty(FlatClientProperties.STYLE, "tabType: card");
        
        tabbedPane.addTab("📅 Lịch làm việc", createScheduleTab());
        tabbedPane.addTab("📋 Yêu cầu nghỉ phép", createLeaveRequestsTab());
        
        add(tabbedPane, "grow");
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0", "[]push[]", ""));
        header.setOpaque(false);
        
        JLabel title = new JLabel("📅 Quản lý lịch làm việc");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        header.add(title);
        
        return header;
    }
    
    private JPanel createScheduleTab() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 16", "[grow]", "[][grow]"));
        panel.setBackground(SURFACE);
        
        // Week navigation
        JPanel navPanel = new JPanel(new MigLayout("insets 0", "[][grow][]", ""));
        navPanel.setOpaque(false);
        
        JButton prevBtn = new JButton("◀ Tuần trước");
        prevBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        prevBtn.addActionListener(e -> navigateWeek(-1));
        navPanel.add(prevBtn);
        
        weekLabel = new JLabel();
        weekLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        weekLabel.setForeground(TEXT_PRIMARY);
        weekLabel.setHorizontalAlignment(SwingConstants.CENTER);
        navPanel.add(weekLabel, "grow, center");
        
        JButton nextBtn = new JButton("Tuần sau ▶");
        nextBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        nextBtn.addActionListener(e -> navigateWeek(1));
        navPanel.add(nextBtn);
        
        JButton copyBtn = new JButton("📋 Copy từ tuần trước");
        copyBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        copyBtn.addActionListener(e -> copyPreviousWeek());
        navPanel.add(copyBtn, "gapleft 16");
        
        JButton autoAssignBtn = new JButton("🤖 Phân công tự động");
        autoAssignBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        autoAssignBtn.setBackground(PRIMARY);
        autoAssignBtn.setForeground(Color.WHITE);
        autoAssignBtn.addActionListener(e -> autoAssignShifts());
        navPanel.add(autoAssignBtn, "gapleft 8");
        
        panel.add(navPanel, "growx, wrap");
        
        // Calendar grid
        calendarGrid = new JPanel(new MigLayout("fill, wrap 8, insets 8, gap 4", "[100!][grow][grow][grow][grow][grow][grow][grow]", ""));
        calendarGrid.setBackground(Color.WHITE);
        calendarGrid.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        
        JScrollPane scroll = new JScrollPane(calendarGrid);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scroll, "grow");
        
        return panel;
    }
    
    private JPanel createLeaveRequestsTab() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 16", "[grow]", "[][grow]"));
        panel.setBackground(SURFACE);
        
        // Header
        JPanel header = new JPanel(new MigLayout("insets 0", "[]push[]", ""));
        header.setOpaque(false);
        
        JLabel title = new JLabel("Yêu cầu nghỉ phép chờ duyệt");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        title.setForeground(TEXT_PRIMARY);
        header.add(title);
        
        JButton refreshBtn = new JButton("🔄 Làm mới");
        refreshBtn.addActionListener(e -> loadLeaveRequests());
        header.add(refreshBtn);
        
        panel.add(header, "growx, wrap");
        
        // Table
        String[] columns = {"ID", "Nhân viên", "Loại nghỉ", "Từ ngày", "Đến ngày", "Lý do", "Trạng thái", "Thao tác"};
        leaveTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7;
            }
        };
        
        leaveRequestsTable = new JTable(leaveTableModel);
        leaveRequestsTable.setRowHeight(45);
        leaveRequestsTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        leaveRequestsTable.getTableHeader().setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        
        // Hide ID column
        leaveRequestsTable.getColumnModel().getColumn(0).setMinWidth(0);
        leaveRequestsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        
        // Action buttons renderer
        leaveRequestsTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer());
        leaveRequestsTable.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor());
        
        JScrollPane scroll = new JScrollPane(leaveRequestsTable);
        scroll.setBorder(BorderFactory.createLineBorder(Color.decode(AppConfig.Colors.BORDER)));
        panel.add(scroll, "grow");
        
        return panel;
    }
    
    private void loadData() {
        // Load staff
        staffList = userService.getAllUsers().stream()
            .filter(u -> u.isActive() && !u.isAdmin())
            .toList();
        
        // Load shift templates
        shiftTemplates = scheduleService.getAllShiftTemplates();
        
        // Refresh displays
        refreshCalendar();
        loadLeaveRequests();
    }
    
    private void refreshCalendar() {
        calendarGrid.removeAll();
        
        // Update week label
        LocalDate weekEnd = currentWeekStart.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        weekLabel.setText("Tuần " + currentWeekStart.format(fmt) + " - " + weekEnd.format(fmt) + "/" + currentWeekStart.getYear());
        
        // Header row
        calendarGrid.add(createHeaderCell("Nhân viên"), "grow");
        String[] days = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        for (int i = 0; i < 7; i++) {
            LocalDate date = currentWeekStart.plusDays(i);
            String dayLabel = days[i] + " " + date.getDayOfMonth();
            calendarGrid.add(createHeaderCell(dayLabel), "grow");
        }
        
        // Load schedules for the week
        List<WorkSchedule> schedules = scheduleService.getSchedulesByDateRange(currentWeekStart, weekEnd);
        
        // Staff rows
        for (User staff : staffList) {
            // Staff name
            JLabel nameLabel = new JLabel(staff.getFullName());
            nameLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
            nameLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            calendarGrid.add(nameLabel, "grow");
            
            // Days
            for (int i = 0; i < 7; i++) {
                LocalDate date = currentWeekStart.plusDays(i);
                WorkSchedule schedule = findSchedule(schedules, staff.getId(), date);
                calendarGrid.add(createDayCell(staff, date, schedule), "grow, h 60!");
            }
        }
        
        calendarGrid.revalidate();
        calendarGrid.repaint();
    }
    
    private JPanel createHeaderCell(String text) {
        JPanel cell = new JPanel(new MigLayout("fill, insets 4", "[center]", "[center]"));
        cell.setBackground(new Color(240, 240, 240));
        
        JLabel label = new JLabel(text);
        label.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        cell.add(label);
        
        return cell;
    }
    
    private JPanel createDayCell(User staff, LocalDate date, WorkSchedule schedule) {
        JPanel cell = new JPanel(new MigLayout("wrap, insets 4", "[grow]", "[center][center]"));
        cell.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        if (schedule != null) {
            Color shiftColor = Color.decode(schedule.getColor());
            cell.setBackground(new Color(shiftColor.getRed(), shiftColor.getGreen(), shiftColor.getBlue(), 50));
            
            JLabel shiftLabel = new JLabel(schedule.getShiftName());
            shiftLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
            shiftLabel.setForeground(shiftColor.darker());
            cell.add(shiftLabel, "center");
            
            // Show time
            String timeStr = schedule.getEffectiveStartTime().toString().substring(0, 5) + 
                           "-" + schedule.getEffectiveEndTime().toString().substring(0, 5);
            JLabel timeLabel = new JLabel(timeStr);
            timeLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 9));
            timeLabel.setForeground(TEXT_SECONDARY);
            cell.add(timeLabel, "center");
        } else {
            cell.setBackground(Color.WHITE);
            JLabel emptyLabel = new JLabel("+");
            emptyLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 16));
            emptyLabel.setForeground(new Color(200, 200, 200));
            cell.add(emptyLabel, "center, span");
        }
        
        // Click to assign/edit shift
        cell.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showAssignShiftDialog(staff, date, schedule);
            }
        });
        
        return cell;
    }
    
    private WorkSchedule findSchedule(List<WorkSchedule> schedules, int userId, LocalDate date) {
        return schedules.stream()
            .filter(s -> s.getUserId() == userId && s.getWorkDate().equals(date))
            .findFirst()
            .orElse(null);
    }
    
    private void showAssignShiftDialog(User staff, LocalDate date, WorkSchedule existing) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Xếp ca làm", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        
        JPanel content = new JPanel(new MigLayout("fill, wrap, insets 20", "[grow]", ""));
        content.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel("Xếp ca cho " + staff.getFullName());
        titleLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        content.add(titleLabel, "gapbottom 8");
        
        JLabel dateLabel = new JLabel("📅 Ngày: " + date.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy")));
        dateLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        content.add(dateLabel, "gapbottom 16");
        
        content.add(new JLabel("Chọn ca:"));
        
        JComboBox<String> shiftCombo = new JComboBox<>();
        shiftCombo.addItem("-- Không xếp ca --");
        for (ShiftTemplate t : shiftTemplates) {
            String displayText = t.getName() + " (" + t.getStartTime().toString().substring(0,5) + " - " + t.getEndTime().toString().substring(0,5) + ")";
            shiftCombo.addItem(displayText);
        }
        if (existing != null && existing.getShiftTemplate() != null) {
            for (int i = 0; i < shiftTemplates.size(); i++) {
                if (shiftTemplates.get(i).getId() == existing.getShiftTemplate().getId()) {
                    shiftCombo.setSelectedIndex(i + 1);
                    break;
                }
            }
        }
        shiftCombo.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        content.add(shiftCombo, "growx, h 35!, gapbottom 16");
        
        // Delete button if existing
        if (existing != null) {
            JButton deleteBtn = new JButton("🗑️ Xóa ca này");
            deleteBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
            deleteBtn.setBackground(Color.decode("#E74C3C"));
            deleteBtn.setForeground(Color.WHITE);
            deleteBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(dialog, 
                    "Xóa ca làm của " + staff.getFullName() + " ngày " + date.format(DateTimeFormatter.ofPattern("dd/MM")) + "?",
                    "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    scheduleService.deleteSchedule(existing.getId());
                    ToastNotification.info(dialog, "Đã xóa ca");
                    dialog.dispose();
                    refreshCalendar();
                }
            });
            content.add(deleteBtn, "growx, h 38!, gapbottom 8");
        }
        
        // Spacer
        content.add(Box.createVerticalGlue(), "grow, pushy");
        
        // Action buttons
        JPanel buttons = new JPanel(new MigLayout("insets 0", "[grow][grow]", ""));
        buttons.setOpaque(false);
        
        JButton saveBtn = new JButton("💾 Lưu");
        saveBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        saveBtn.setBackground(PRIMARY);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> {
            int selectedIndex = shiftCombo.getSelectedIndex();
            if (selectedIndex > 0) {
                ShiftTemplate selected = shiftTemplates.get(selectedIndex - 1);
                WorkSchedule ws = new WorkSchedule(staff.getId(), date, selected);
                ws.setCreatedBy(currentUser.getId());
                if (scheduleService.createSchedule(ws)) {
                    ToastNotification.success(dialog, "Đã xếp ca!");
                    dialog.dispose();
                    refreshCalendar();
                } else {
                    ToastNotification.error(dialog, "Lỗi khi xếp ca");
                }
            } else {
                dialog.dispose();
            }
        });
        buttons.add(saveBtn, "grow, h 40!");
        
        JButton cancelBtn = new JButton("Hủy");
        cancelBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 13));
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttons.add(cancelBtn, "grow, h 40!");
        
        content.add(buttons, "growx, dock south");
        
        dialog.add(content);
        dialog.setVisible(true);
    }
    
    private void autoAssignShifts() {
        if (staffList.isEmpty() || shiftTemplates.isEmpty()) {
            ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Không có dữ liệu để phân công");
            return;
        }
        
        // Confirm dialog
        String message = "Phân công tự động cho tuần " + 
            currentWeekStart.format(DateTimeFormatter.ofPattern("dd/MM")) + "?\n\n" +
            "• Đầu bếp: Ca Sáng hoặc Ca Tối (xen kẽ)\n" +
            "• Thu ngân: Ca Chiều hoặc Ca Full\n" +
            "• Phục vụ: Luân phiên các ca\n\n" +
            "Lịch hiện tại sẽ bị ghi đè!";
        
        int confirm = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            message,
            "🤖 Phân công tự động",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (confirm != JOptionPane.OK_OPTION) return;
        
        // Get shift templates by code
        ShiftTemplate morning = shiftTemplates.stream().filter(t -> "MORNING".equals(t.getCode())).findFirst().orElse(shiftTemplates.get(0));
        ShiftTemplate afternoon = shiftTemplates.stream().filter(t -> "AFTERNOON".equals(t.getCode())).findFirst().orElse(shiftTemplates.get(0));
        ShiftTemplate evening = shiftTemplates.stream().filter(t -> "EVENING".equals(t.getCode())).findFirst().orElse(shiftTemplates.get(0));
        ShiftTemplate fullDay = shiftTemplates.stream().filter(t -> "FULL_DAY".equals(t.getCode())).findFirst().orElse(shiftTemplates.get(0));
        
        int assignedCount = 0;
        
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate date = currentWeekStart.plusDays(dayOffset);
            int staffIndex = 0;
            
            for (User staff : staffList) {
                ShiftTemplate shiftToAssign;
                String roleName = staff.getRole() != null ? staff.getRole().getName().toUpperCase() : "";
                
                if (roleName.contains("CHEF") || roleName.contains("ĐẦU BẾP")) {
                    // Chef: alternating morning/evening
                    shiftToAssign = (dayOffset % 2 == 0) ? morning : evening;
                } else if (roleName.contains("CASHIER") || roleName.contains("THU NGÂN")) {
                    // Cashier: afternoon or full day on weekends
                    shiftToAssign = (dayOffset >= 5) ? fullDay : afternoon;
                } else {
                    // Waiter/other: rotating shifts
                    int shiftIndex = (staffIndex + dayOffset) % 3;
                    shiftToAssign = switch (shiftIndex) {
                        case 0 -> morning;
                        case 1 -> afternoon;
                        default -> evening;
                    };
                }
                
                WorkSchedule ws = new WorkSchedule(staff.getId(), date, shiftToAssign);
                ws.setCreatedBy(currentUser.getId());
                if (scheduleService.createSchedule(ws)) {
                    assignedCount++;
                }
                
                staffIndex++;
            }
        }
        
        ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
            "Đã phân công " + assignedCount + " ca làm!");
        refreshCalendar();
    }
    
    private void navigateWeek(int direction) {
        currentWeekStart = currentWeekStart.plusWeeks(direction);
        refreshCalendar();
    }
    
    private void copyPreviousWeek() {
        LocalDate previousWeek = currentWeekStart.minusWeeks(1);
        if (scheduleService.copyWeekSchedule(previousWeek, currentWeekStart, currentUser.getId())) {
            ToastNotification.success(SwingUtilities.getWindowAncestor(this), "Đã copy lịch từ tuần trước!");
            refreshCalendar();
        } else {
            ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Không có lịch tuần trước để copy");
        }
    }
    
    private void loadLeaveRequests() {
        leaveTableModel.setRowCount(0);
        
        List<LeaveRequest> requests = scheduleService.getPendingLeaveRequests();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        for (LeaveRequest req : requests) {
            leaveTableModel.addRow(new Object[]{
                req.getId(),
                req.getUser().getFullName(),
                req.getLeaveType().getDisplayName(),
                req.getStartDate().format(fmt),
                req.getEndDate().format(fmt),
                req.getReason(),
                req.getStatus().getDisplayName(),
                "actions"
            });
        }
    }
    
    // Custom renderer for action buttons
    class ButtonRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new MigLayout("insets 2, gap 4", "[][]", ""));
            panel.setOpaque(true);
            panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            
            JButton approveBtn = new JButton("✓");
            approveBtn.setBackground(SUCCESS);
            approveBtn.setForeground(Color.WHITE);
            approveBtn.setPreferredSize(new Dimension(35, 30));
            panel.add(approveBtn);
            
            JButton rejectBtn = new JButton("✗");
            rejectBtn.setBackground(Color.decode("#E74C3C"));
            rejectBtn.setForeground(Color.WHITE);
            rejectBtn.setPreferredSize(new Dimension(35, 30));
            panel.add(rejectBtn);
            
            return panel;
        }
    }
    
    class ButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private int currentRow;
        
        public ButtonEditor() {
            super(new JCheckBox());
            
            panel = new JPanel(new MigLayout("insets 2, gap 4", "[][]", ""));
            
            JButton approveBtn = new JButton("✓");
            approveBtn.setBackground(SUCCESS);
            approveBtn.setForeground(Color.WHITE);
            approveBtn.setPreferredSize(new Dimension(35, 30));
            approveBtn.addActionListener(e -> {
                int requestId = (int) leaveTableModel.getValueAt(currentRow, 0);
                if (scheduleService.approveLeaveRequest(requestId, currentUser.getId())) {
                    ToastNotification.success(SwingUtilities.getWindowAncestor(ScheduleManagementPanel.this), "Đã duyệt!");
                    loadLeaveRequests();
                }
                fireEditingStopped();
            });
            panel.add(approveBtn);
            
            JButton rejectBtn = new JButton("✗");
            rejectBtn.setBackground(Color.decode("#E74C3C"));
            rejectBtn.setForeground(Color.WHITE);
            rejectBtn.setPreferredSize(new Dimension(35, 30));
            rejectBtn.addActionListener(e -> {
                String reason = JOptionPane.showInputDialog(panel, "Lý do từ chối:");
                if (reason != null && !reason.trim().isEmpty()) {
                    int requestId = (int) leaveTableModel.getValueAt(currentRow, 0);
                    if (scheduleService.rejectLeaveRequest(requestId, currentUser.getId(), reason)) {
                        ToastNotification.info(SwingUtilities.getWindowAncestor(ScheduleManagementPanel.this), "Đã từ chối");
                        loadLeaveRequests();
                    }
                }
                fireEditingStopped();
            });
            panel.add(rejectBtn);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "actions";
        }
    }
}
