package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.LeaveRequest;
import com.restaurant.model.LeaveRequest.LeaveStatus;
import com.restaurant.model.LeaveRequest.LeaveType;
import com.restaurant.model.ShiftTemplate;
import com.restaurant.model.User;
import com.restaurant.model.WorkSchedule;
import com.restaurant.service.ScheduleService;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * My Schedule Panel - Cho Staff xem lịch và xin nghỉ phép
 * For WAITER, CHEF, CASHIER (non-admin/manager)
 */
public class MySchedulePanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(MySchedulePanel.class);
    
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
    
    // State
    private LocalDate currentWeekStart;
    
    // UI Components
    private JLabel weekLabel;
    private JPanel scheduleGrid;
    private JTable myRequestsTable;
    private DefaultTableModel requestsTableModel;
    
    public MySchedulePanel(User user) {
        this.currentUser = user;
        this.scheduleService = ScheduleService.getInstance();
        this.currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        initializeUI();
        loadData();
    }
    
    private void initializeUI() {
        setLayout(new MigLayout("fill, insets 16", "[grow]", "[][grow]"));
        setBackground(BACKGROUND);
        
        // Header
        add(createHeader(), "growx, wrap");
        
        // Tabbed content
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        tabbedPane.putClientProperty(FlatClientProperties.STYLE, "tabType: card");
        
        tabbedPane.addTab("📅 Lịch của tôi", createMyScheduleTab());
        tabbedPane.addTab("📝 Xin nghỉ phép", createLeaveRequestTab());
        tabbedPane.addTab("📋 Trạng thái yêu cầu", createRequestStatusTab());
        
        add(tabbedPane, "grow");
        
        // Auto-refresh status every 5 seconds
        javax.swing.Timer refreshTimer = new javax.swing.Timer(5000, e -> loadMyRequests());
        refreshTimer.start();
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0", "[]push", ""));
        header.setOpaque(false);
        
        JLabel title = new JLabel("📅 Lịch làm việc của tôi");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        header.add(title);
        
        return header;
    }
    
    private JPanel createMyScheduleTab() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 16", "[grow]", "[][grow]"));
        panel.setBackground(SURFACE);
        
        // Week navigation
        JPanel navPanel = new JPanel(new MigLayout("insets 0", "[][grow][]", ""));
        navPanel.setOpaque(false);
        
        JButton prevBtn = new JButton("◀ Tuần trước");
        prevBtn.addActionListener(e -> navigateWeek(-1));
        navPanel.add(prevBtn);
        
        weekLabel = new JLabel();
        weekLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 16));
        weekLabel.setForeground(TEXT_PRIMARY);
        weekLabel.setHorizontalAlignment(SwingConstants.CENTER);
        navPanel.add(weekLabel, "grow, center");
        
        JButton nextBtn = new JButton("Tuần sau ▶");
        nextBtn.addActionListener(e -> navigateWeek(1));
        navPanel.add(nextBtn);
        
        panel.add(navPanel, "growx, wrap");
        
        // Schedule grid
        scheduleGrid = new JPanel(new MigLayout("fill, wrap 2, insets 16, gap 12", "[150!][grow]", ""));
        scheduleGrid.setBackground(Color.WHITE);
        scheduleGrid.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        
        JScrollPane scroll = new JScrollPane(scheduleGrid);
        scroll.setBorder(null);
        panel.add(scroll, "grow");
        
        return panel;
    }
    
    private JPanel createLeaveRequestTab() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 16", "[grow]", "[][][][grow]"));
        panel.setBackground(SURFACE);
        
        // Rules notice
        JPanel rulesPanel = new JPanel(new MigLayout("wrap, insets 16", "[grow]", ""));
        rulesPanel.setBackground(new Color(255, 243, 205));
        rulesPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 193, 7)));
        rulesPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        
        JLabel rulesTitle = new JLabel("📋 Nội quy xin nghỉ phép");
        rulesTitle.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        rulesPanel.add(rulesTitle, "gapbottom 8");
        
        String[] rules = {
            "• Xin nghỉ phép thường: Trước 1-3 ngày làm việc",
            "• Xin nghỉ ốm: Có thể xin trong ngày (cần giấy khám)",
            "• Việc khẩn cấp: Liên hệ trực tiếp Manager qua điện thoại",
            "• Số ngày phép năm: 12 ngày/năm",
            "• Số ngày nghỉ ốm: 5 ngày/năm (có lương)"
        };
        
        for (String rule : rules) {
            JLabel ruleLabel = new JLabel(rule);
            ruleLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
            rulesPanel.add(ruleLabel);
        }
        
        panel.add(rulesPanel, "growx, wrap, gapbottom 16");
        
        // Request form
        JPanel formPanel = new JPanel(new MigLayout("wrap 2, insets 16", "[][grow]", ""));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createTitledBorder("Tạo yêu cầu nghỉ phép mới"));
        formPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        
        formPanel.add(new JLabel("Loại nghỉ:"));
        JComboBox<String> leaveTypeCombo = new JComboBox<>(new String[]{
            "Nghỉ phép năm",
            "Nghỉ ốm",
            "Việc riêng",
            "Khẩn cấp"
        });
        formPanel.add(leaveTypeCombo, "growx");
        
        // Calculate minimum start date based on leave type
        LocalDate today = LocalDate.now();
        LocalDate minStartDate = today.plusDays(1); // Default: tomorrow for ANNUAL/PERSONAL
        
        formPanel.add(new JLabel("Từ ngày:"));
        JSpinner startDateSpinner = new JSpinner(new SpinnerDateModel(
            java.sql.Date.valueOf(minStartDate), 
            java.sql.Date.valueOf(today),
            null, 
            java.util.Calendar.DAY_OF_MONTH
        ));
        startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, "dd/MM/yyyy"));
        formPanel.add(startDateSpinner, "growx");
        
        formPanel.add(new JLabel("Đến ngày:"));
        JSpinner endDateSpinner = new JSpinner(new SpinnerDateModel(
            java.sql.Date.valueOf(minStartDate),
            java.sql.Date.valueOf(today),
            null,
            java.util.Calendar.DAY_OF_MONTH
        ));
        endDateSpinner.setEditor(new JSpinner.DateEditor(endDateSpinner, "dd/MM/yyyy"));
        formPanel.add(endDateSpinner, "growx");
        
        // Days count preview
        JLabel daysCountLabel = new JLabel("📅 Số ngày nghỉ: 1 ngày");
        daysCountLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.ITALIC, 12));
        daysCountLabel.setForeground(TEXT_SECONDARY);
        formPanel.add(new JLabel(""));
        formPanel.add(daysCountLabel);
        
        // Validation hint
        JLabel validationHint = new JLabel("");
        validationHint.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 11));
        validationHint.setForeground(Color.decode("#E74C3C"));
        formPanel.add(new JLabel(""));
        formPanel.add(validationHint);
        
        // Update days count when dates change
        Runnable updateDaysCount = () -> {
            java.util.Date startDate = (java.util.Date) startDateSpinner.getValue();
            java.util.Date endDate = (java.util.Date) endDateSpinner.getValue();
            LocalDate start = startDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            LocalDate end = endDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            
            if (end.isBefore(start)) {
                daysCountLabel.setText("⚠️ Ngày kết thúc phải sau ngày bắt đầu!");
                daysCountLabel.setForeground(Color.decode("#E74C3C"));
            } else {
                long days = ChronoUnit.DAYS.between(start, end) + 1;
                daysCountLabel.setText("📅 Số ngày nghỉ: " + days + " ngày");
                daysCountLabel.setForeground(TEXT_SECONDARY);
            }
        };
        
        startDateSpinner.addChangeListener(e -> {
            // Auto-sync end date if it's before start date
            java.util.Date startDate = (java.util.Date) startDateSpinner.getValue();
            java.util.Date endDate = (java.util.Date) endDateSpinner.getValue();
            if (endDate.before(startDate)) {
                endDateSpinner.setValue(startDate);
            }
            updateDaysCount.run();
        });
        
        endDateSpinner.addChangeListener(e -> updateDaysCount.run());
        
        // Update min dates when leave type changes
        leaveTypeCombo.addActionListener(e -> {
            int typeIndex = leaveTypeCombo.getSelectedIndex();
            LocalDate newMinDate;
            String hint;
            
            if (typeIndex == 1 || typeIndex == 3) { // SICK or EMERGENCY
                newMinDate = today;
                hint = "✅ Có thể xin trong ngày";
            } else { // ANNUAL or PERSONAL
                newMinDate = today.plusDays(1);
                hint = "⚠️ Cần xin trước ít nhất 1 ngày";
            }
            
            validationHint.setText(hint);
            validationHint.setForeground(typeIndex == 1 || typeIndex == 3 ? SUCCESS : WARNING);
            
            // Update spinner values
            java.util.Date currentStart = (java.util.Date) startDateSpinner.getValue();
            LocalDate currentStartLocal = currentStart.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            
            if (currentStartLocal.isBefore(newMinDate)) {
                startDateSpinner.setValue(java.sql.Date.valueOf(newMinDate));
                endDateSpinner.setValue(java.sql.Date.valueOf(newMinDate));
            }
        });
        
        // Trigger initial hint
        leaveTypeCombo.setSelectedIndex(0);
        
        formPanel.add(new JLabel("Lý do:"));
        JTextArea reasonArea = new JTextArea(3, 20);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        JScrollPane reasonScroll = new JScrollPane(reasonArea);
        formPanel.add(reasonScroll, "growx");
        
        JButton submitBtn = new JButton("📤 Gửi yêu cầu");
        submitBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        submitBtn.setBackground(PRIMARY);
        submitBtn.setForeground(Color.WHITE);
        submitBtn.addActionListener(e -> {
            // Get values
            java.util.Date startDateVal = (java.util.Date) startDateSpinner.getValue();
            java.util.Date endDateVal = (java.util.Date) endDateSpinner.getValue();
            String reason = reasonArea.getText().trim();
            
            LocalDate start = startDateVal.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            LocalDate end = endDateVal.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            
            // Comprehensive validation
            if (reason.isEmpty()) {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Vui lòng nhập lý do!");
                return;
            }
            
            if (end.isBefore(start)) {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu!");
                return;
            }
            
            int typeIndex = leaveTypeCombo.getSelectedIndex();
            LeaveType leaveType = switch (typeIndex) {
                case 0 -> LeaveType.ANNUAL;
                case 1 -> LeaveType.SICK;
                case 2 -> LeaveType.PERSONAL;
                default -> LeaveType.EMERGENCY;
            };
            
            // Check advance notice
            long daysUntilStart = ChronoUnit.DAYS.between(LocalDate.now(), start);
            
            if (leaveType == LeaveType.ANNUAL || leaveType == LeaveType.PERSONAL) {
                if (daysUntilStart < 1) {
                    ToastNotification.error(SwingUtilities.getWindowAncestor(this), 
                        "Nghỉ phép năm/việc riêng cần xin trước ít nhất 1 ngày!");
                    return;
                }
            } else {
                // SICK/EMERGENCY: start date must be today or future
                if (start.isBefore(LocalDate.now())) {
                    ToastNotification.error(SwingUtilities.getWindowAncestor(this), 
                        "Không thể xin nghỉ cho ngày trong quá khứ!");
                    return;
                }
            }
            
            // Check max days (prevent abuse)
            long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
            if (totalDays > 30) {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), 
                    "Không thể xin nghỉ quá 30 ngày liên tục!");
                return;
            }
            
            // Create request
            LeaveRequest request = new LeaveRequest(currentUser.getId(), leaveType, start, end, reason);
            if (scheduleService.createLeaveRequest(request)) {
                ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                    "Đã gửi yêu cầu nghỉ " + totalDays + " ngày! Chờ Manager duyệt.");
                reasonArea.setText("");
                // Reset form
                startDateSpinner.setValue(java.sql.Date.valueOf(today.plusDays(1)));
                endDateSpinner.setValue(java.sql.Date.valueOf(today.plusDays(1)));
                leaveTypeCombo.setSelectedIndex(0);
                loadMyRequests();
            } else {
                ToastNotification.error(SwingUtilities.getWindowAncestor(this), "Lỗi khi gửi yêu cầu!");
            }
        });
        formPanel.add(submitBtn, "span 2, center, gaptop 16");
        
        panel.add(formPanel, "growx, wrap");
        
        // My requests table
        JPanel historyPanel = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[][grow]"));
        historyPanel.setOpaque(false);
        
        JLabel historyTitle = new JLabel("📋 Yêu cầu của tôi");
        historyTitle.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        historyPanel.add(historyTitle, "wrap, gaptop 16, gapbottom 8");
        
        String[] columns = {"Loại", "Từ ngày", "Đến ngày", "Lý do", "Trạng thái", "Phản hồi"};
        requestsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        myRequestsTable = new JTable(requestsTableModel);
        myRequestsTable.setRowHeight(35);
        myRequestsTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        
        JScrollPane tableScroll = new JScrollPane(myRequestsTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(Color.decode(AppConfig.Colors.BORDER)));
        historyPanel.add(tableScroll, "grow");
        
        panel.add(historyPanel, "grow");
        
        return panel;
    }
    
    private void loadData() {
        refreshSchedule();
        loadMyRequests();
    }
    
    private void refreshSchedule() {
        scheduleGrid.removeAll();
        
        // Update week label
        LocalDate weekEnd = currentWeekStart.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        weekLabel.setText("Tuần " + currentWeekStart.format(fmt) + " - " + weekEnd.format(fmt) + "/" + currentWeekStart.getYear());
        
        // Load my schedules
        List<WorkSchedule> schedules = scheduleService.getSchedulesByUser(currentUser.getId(), currentWeekStart, weekEnd);
        
        String[] days = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM");
        
        for (int i = 0; i < 7; i++) {
            LocalDate date = currentWeekStart.plusDays(i);
            WorkSchedule schedule = findSchedule(schedules, date);
            
            // Day label
            JLabel dayLabel = new JLabel(days[i] + " " + date.format(dateFmt));
            dayLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
            dayLabel.setForeground(TEXT_PRIMARY);
            scheduleGrid.add(dayLabel);
            
            // Shift info
            if (schedule != null) {
                JPanel shiftPanel = new JPanel(new MigLayout("insets 8", "[]", ""));
                Color shiftColor = Color.decode(schedule.getColor());
                shiftPanel.setBackground(new Color(shiftColor.getRed(), shiftColor.getGreen(), shiftColor.getBlue(), 40));
                shiftPanel.setBorder(BorderFactory.createLineBorder(shiftColor, 2));
                shiftPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
                
                String text = schedule.getShiftName() + " (" + 
                    schedule.getEffectiveStartTime().toString().substring(0, 5) + " - " + 
                    schedule.getEffectiveEndTime().toString().substring(0, 5) + ")";
                JLabel shiftLabel = new JLabel(text);
                shiftLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
                shiftLabel.setForeground(shiftColor.darker());
                shiftPanel.add(shiftLabel);
                
                scheduleGrid.add(shiftPanel, "growx");
            } else {
                JLabel noShiftLabel = new JLabel("— Nghỉ —");
                noShiftLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.ITALIC, 12));
                noShiftLabel.setForeground(TEXT_SECONDARY);
                scheduleGrid.add(noShiftLabel);
            }
        }
        
        scheduleGrid.revalidate();
        scheduleGrid.repaint();
    }
    
    private WorkSchedule findSchedule(List<WorkSchedule> schedules, LocalDate date) {
        return schedules.stream()
            .filter(s -> s.getWorkDate().equals(date))
            .findFirst()
            .orElse(null);
    }
    
    private void loadMyRequests() {
        requestsTableModel.setRowCount(0);
        
        List<LeaveRequest> requests = scheduleService.getLeaveRequestsByUser(currentUser.getId());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        for (LeaveRequest req : requests) {
            String statusText = switch (req.getStatus()) {
                case PENDING -> "⏳ Chờ duyệt";
                case APPROVED -> "✅ Đã duyệt";
                case REJECTED -> "❌ Từ chối";
            };
            
            String feedback = req.getStatus() == LeaveStatus.REJECTED && req.getRejectionReason() != null
                ? req.getRejectionReason()
                : (req.getStatus() == LeaveStatus.APPROVED ? "Đã được duyệt" : "—");
            
            requestsTableModel.addRow(new Object[]{
                req.getLeaveType().getDisplayName(),
                req.getStartDate().format(fmt),
                req.getEndDate().format(fmt),
                req.getReason(),
                statusText,
                feedback
            });
        }
    }
    
    private void navigateWeek(int direction) {
        currentWeekStart = currentWeekStart.plusWeeks(direction);
        refreshSchedule();
    }
    
    private JPanel createRequestStatusTab() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 16", "[grow]", "[][grow]"));
        panel.setBackground(SURFACE);
        
        // Header with refresh hint
        JPanel header = new JPanel(new MigLayout("insets 0", "[]push[]", ""));
        header.setOpaque(false);
        
        JLabel title = new JLabel("📋 Trạng thái yêu cầu nghỉ phép");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        header.add(title);
        
        JLabel refreshHint = new JLabel("🔄 Tự động cập nhật mỗi 5 giây");
        refreshHint.setFont(new Font(AppConfig.FONT_FAMILY, Font.ITALIC, 11));
        refreshHint.setForeground(TEXT_SECONDARY);
        header.add(refreshHint);
        
        panel.add(header, "growx, wrap, gapbottom 16");
        
        // Status cards panel
        JPanel cardsPanel = new JPanel(new MigLayout("wrap, insets 0, gap 12", "[grow]", ""));
        cardsPanel.setOpaque(false);
        
        // Load requests and create cards
        List<LeaveRequest> requests = scheduleService.getLeaveRequestsByUser(currentUser.getId());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        if (requests.isEmpty()) {
            JPanel emptyPanel = new JPanel(new MigLayout("wrap, insets 40", "[grow, center]", ""));
            emptyPanel.setBackground(Color.WHITE);
            emptyPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
            
            JLabel emptyIcon = new JLabel("📭");
            emptyIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
            emptyIcon.setHorizontalAlignment(SwingConstants.CENTER);
            emptyPanel.add(emptyIcon, "center");
            
            JLabel emptyText = new JLabel("Chưa có yêu cầu nào");
            emptyText.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 16));
            emptyText.setForeground(TEXT_SECONDARY);
            emptyPanel.add(emptyText, "center");
            
            cardsPanel.add(emptyPanel, "growx");
        } else {
            for (LeaveRequest req : requests) {
                JPanel card = new JPanel(new MigLayout("fill, insets 16", "[grow][]", ""));
                card.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
                
                // Color based on status
                Color statusColor = switch (req.getStatus()) {
                    case PENDING -> WARNING;
                    case APPROVED -> SUCCESS;
                    case REJECTED -> Color.decode("#E74C3C");
                };
                
                card.setBackground(new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), 20));
                card.setBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, statusColor));
                
                // Left side: Request details
                JPanel details = new JPanel(new MigLayout("wrap, insets 0, gap 4", "", ""));
                details.setOpaque(false);
                
                JLabel typeLabel = new JLabel(req.getLeaveType().getDisplayName());
                typeLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
                typeLabel.setForeground(TEXT_PRIMARY);
                details.add(typeLabel);
                
                JLabel dateLabel = new JLabel("📅 " + req.getStartDate().format(fmt) + " → " + req.getEndDate().format(fmt));
                dateLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
                dateLabel.setForeground(TEXT_SECONDARY);
                details.add(dateLabel);
                
                JLabel reasonLabel = new JLabel("💬 " + req.getReason());
                reasonLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
                reasonLabel.setForeground(TEXT_SECONDARY);
                details.add(reasonLabel);
                
                if (req.getStatus() == LeaveStatus.REJECTED && req.getRejectionReason() != null) {
                    JLabel rejectLabel = new JLabel("❌ Lý do từ chối: " + req.getRejectionReason());
                    rejectLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.ITALIC, 11));
                    rejectLabel.setForeground(Color.decode("#E74C3C"));
                    details.add(rejectLabel);
                }
                
                card.add(details, "grow");
                
                // Right side: Status badge
                String statusEmoji = switch (req.getStatus()) {
                    case PENDING -> "⏳";
                    case APPROVED -> "✅";
                    case REJECTED -> "❌";
                };
                String statusText = switch (req.getStatus()) {
                    case PENDING -> "Chờ duyệt";
                    case APPROVED -> "ĐÃ DUYỆT";
                    case REJECTED -> "TỪ CHỐI";
                };
                
                JPanel statusBadge = new JPanel(new MigLayout("wrap, insets 8", "[center]", ""));
                statusBadge.setBackground(statusColor);
                statusBadge.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
                
                JLabel emojiLabel = new JLabel(statusEmoji);
                emojiLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                statusBadge.add(emojiLabel);
                
                JLabel textLabel = new JLabel(statusText);
                textLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
                textLabel.setForeground(Color.WHITE);
                statusBadge.add(textLabel);
                
                card.add(statusBadge, "aligny top");
                
                cardsPanel.add(card, "growx");
            }
        }
        
        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scroll, "grow");
        
        return panel;
    }
}
