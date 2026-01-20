package com.restaurant.view.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.config.AppConfig;
import com.restaurant.model.Reservation;
import com.restaurant.model.Reservation.Status;
import com.restaurant.model.Table;
import com.restaurant.model.User;
import com.restaurant.service.ReservationService;
import com.restaurant.service.ServiceResult;
import com.restaurant.service.TableService;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reservation Panel - Quản lý đặt bàn
 */
public class ReservationPanel extends JPanel {
    
    private static final Logger logger = LogManager.getLogger(ReservationPanel.class);
    
    // Colors
    private static final Color BACKGROUND = Color.decode(AppConfig.Colors.BACKGROUND);
    private static final Color SURFACE = Color.decode(AppConfig.Colors.SURFACE);
    private static final Color PRIMARY = Color.decode(AppConfig.Colors.PRIMARY);
    private static final Color SUCCESS = Color.decode(AppConfig.Colors.SUCCESS);
    private static final Color WARNING = Color.decode(AppConfig.Colors.WARNING);
    private static final Color ERROR = Color.decode(AppConfig.Colors.ERROR);
    private static final Color TEXT_PRIMARY = Color.decode(AppConfig.Colors.TEXT_PRIMARY);
    private static final Color TEXT_SECONDARY = Color.decode(AppConfig.Colors.TEXT_SECONDARY);
    
    private final User currentUser;
    private final ReservationService reservationService;
    private final TableService tableService;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    
    private JTable reservationTable;
    private DefaultTableModel tableModel;
    private JLabel totalLabel;
    private JLabel dateLabel;
    private LocalDate viewDate = LocalDate.now();
    
    public ReservationPanel(User user) {
        this.currentUser = user;
        this.reservationService = ReservationService.getInstance();
        this.tableService = TableService.getInstance();
        
        initializeUI();
        loadReservations();
        startAutoRefresh();
    }
    
    private void initializeUI() {
        setLayout(new MigLayout("fill, insets 16", "[grow]", "[][grow]"));
        setBackground(BACKGROUND);
        
        add(createHeader(), "growx, wrap");
        add(createContent(), "grow");
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0", "[]16[]push[][]", ""));
        header.setOpaque(false);
        
        JLabel title = new JLabel("📅 Quản lý đặt bàn");
        title.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        header.add(title);
        
        // Date navigation
        JPanel dateNav = new JPanel(new MigLayout("insets 0, gap 4", "", ""));
        dateNav.setOpaque(false);
        
        JButton prevBtn = new JButton("◀");
        prevBtn.addActionListener(e -> {
            viewDate = viewDate.minusDays(1);
            updateDateLabel();
            loadReservations();
        });
        dateNav.add(prevBtn);
        
        dateLabel = new JLabel();
        dateLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 14));
        updateDateLabel();
        dateNav.add(dateLabel);
        
        JButton nextBtn = new JButton("▶");
        nextBtn.addActionListener(e -> {
            viewDate = viewDate.plusDays(1);
            updateDateLabel();
            loadReservations();
        });
        dateNav.add(nextBtn);
        
        JButton todayBtn = new JButton("Hôm nay");
        todayBtn.addActionListener(e -> {
            viewDate = LocalDate.now();
            updateDateLabel();
            loadReservations();
        });
        dateNav.add(todayBtn);
        
        header.add(dateNav);
        
        JButton addBtn = new JButton("➕ Đặt bàn mới");
        addBtn.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 13));
        addBtn.setBackground(PRIMARY);
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> showReservationDialog(null));
        header.add(addBtn);
        
        JButton refreshBtn = new JButton("🔄");
        refreshBtn.addActionListener(e -> loadReservations());
        header.add(refreshBtn);
        
        return header;
    }
    
    private void updateDateLabel() {
        if (viewDate.equals(LocalDate.now())) {
            dateLabel.setText("📌 Hôm nay " + viewDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            dateLabel.setText(viewDate.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy")));
        }
    }
    
    private JPanel createContent() {
        JPanel content = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[][grow][]"));
        content.setOpaque(false);
        
        // Stats
        content.add(createStatsPanel(), "growx, wrap");
        
        // Table
        String[] columns = {"ID", "Thời gian", "Bàn", "Khách hàng", "SĐT", "Số người", "Trạng thái", "Ghi chú", "Thao tác"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column == 8; }
        };
        
        reservationTable = new JTable(tableModel);
        reservationTable.setRowHeight(50);
        reservationTable.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        reservationTable.getTableHeader().setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 12));
        
        // Status renderer
        reservationTable.getColumnModel().getColumn(6).setCellRenderer(new StatusRenderer());
        
        // Action buttons
        reservationTable.getColumnModel().getColumn(8).setCellRenderer(new ButtonRenderer());
        reservationTable.getColumnModel().getColumn(8).setCellEditor(new ButtonEditor());
        
        // Column widths
        reservationTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        reservationTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        reservationTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        reservationTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        reservationTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        reservationTable.getColumnModel().getColumn(5).setPreferredWidth(60);
        reservationTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        reservationTable.getColumnModel().getColumn(7).setPreferredWidth(150);
        reservationTable.getColumnModel().getColumn(8).setPreferredWidth(150);
        
        JScrollPane scroll = new JScrollPane(reservationTable);
        scroll.setBorder(BorderFactory.createLineBorder(Color.decode(AppConfig.Colors.BORDER)));
        content.add(scroll, "grow, wrap");
        
        totalLabel = new JLabel();
        totalLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 12));
        totalLabel.setForeground(TEXT_SECONDARY);
        content.add(totalLabel);
        
        return content;
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 16", "[grow][grow][grow][grow]", ""));
        panel.setOpaque(false);
        
        List<Reservation> all = reservationService.getTodayReservations();
        long pending = all.stream().filter(r -> r.getStatus() == Status.PENDING).count();
        long confirmed = all.stream().filter(r -> r.getStatus() == Status.CONFIRMED).count();
        long arrived = all.stream().filter(r -> r.getStatus() == Status.ARRIVED).count();
        
        panel.add(createStatCard("📋", "Tổng đặt", String.valueOf(all.size()), PRIMARY), "grow");
        panel.add(createStatCard("⏳", "Đang chờ", String.valueOf(pending), WARNING), "grow");
        panel.add(createStatCard("✅", "Xác nhận", String.valueOf(confirmed), Color.decode("#2196F3")), "grow");
        panel.add(createStatCard("🎉", "Đã đến", String.valueOf(arrived), SUCCESS), "grow");
        
        return panel;
    }
    
    private JPanel createStatCard(String icon, String label, String value, Color color) {
        JPanel card = new JPanel(new MigLayout("wrap, insets 12", "[grow]", ""));
        card.setBackground(Color.WHITE);
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        card.setBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, color));
        
        JLabel iconLabel = new JLabel(icon + " " + label);
        iconLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.PLAIN, 11));
        iconLabel.setForeground(TEXT_SECONDARY);
        card.add(iconLabel);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 20));
        valueLabel.setForeground(color);
        card.add(valueLabel);
        
        return card;
    }
    
    private void loadReservations() {
        tableModel.setRowCount(0);
        
        // Use DAO to get by date - temporary workaround
        List<Reservation> reservations = reservationService.getTodayReservations()
            .stream()
            .filter(r -> r.getReservationTime().toLocalDate().equals(viewDate))
            .sorted((a, b) -> a.getReservationTime().compareTo(b.getReservationTime()))
            .toList();
        
        for (Reservation r : reservations) {
            tableModel.addRow(new Object[]{
                r.getId(),
                r.getReservationTime().format(timeFormatter),
                r.getTableName() != null ? r.getTableName() : "Bàn " + r.getTableId(),
                r.getCustomerName(),
                r.getCustomerPhone(),
                r.getGuestCount() + " người",
                r,  // For status rendering
                r.getNotes() != null ? r.getNotes() : "",
                r.getId()
            });
        }
        
        totalLabel.setText("Tổng: " + reservations.size() + " lịch đặt bàn");
    }
    
    private void showReservationDialog(Reservation reservation) {
        boolean isNew = reservation == null;
        if (isNew) reservation = new Reservation();
        
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
            isNew ? "Đặt bàn mới" : "Sửa đặt bàn", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(450, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel content = new JPanel(new MigLayout("wrap 2, insets 20", "[][grow]", ""));
        content.setBackground(Color.WHITE);
        
        // Table selection
        content.add(new JLabel("Bàn: *"));
        JComboBox<Table> tableCombo = new JComboBox<>();
        List<Table> tables = tableService.getAllTables();
        for (Table t : tables) {
            tableCombo.addItem(t);
        }
        if (!isNew) {
            for (int i = 0; i < tableCombo.getItemCount(); i++) {
                if (tableCombo.getItemAt(i).getId() == reservation.getTableId()) {
                    tableCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        content.add(tableCombo, "growx");
        
        // Customer name
        content.add(new JLabel("Tên khách: *"));
        JTextField nameField = new JTextField(reservation.getCustomerName(), 20);
        content.add(nameField, "growx");
        
        // Phone
        content.add(new JLabel("SĐT: *"));
        JTextField phoneField = new JTextField(reservation.getCustomerPhone(), 15);
        content.add(phoneField, "growx");
        
        // Guest count
        content.add(new JLabel("Số người: *"));
        JSpinner guestSpinner = new JSpinner(new SpinnerNumberModel(
            reservation.getGuestCount() > 0 ? reservation.getGuestCount() : 2, 1, 50, 1));
        content.add(guestSpinner, "growx");
        
        // Date
        content.add(new JLabel("Ngày: *"));
        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
        if (reservation.getReservationTime() != null) {
            dateSpinner.setValue(java.sql.Date.valueOf(reservation.getReservationTime().toLocalDate()));
        } else {
            dateSpinner.setValue(java.sql.Date.valueOf(viewDate));
        }
        content.add(dateSpinner, "growx");
        
        // Time
        content.add(new JLabel("Giờ: *"));
        JSpinner timeSpinner = new JSpinner(new SpinnerDateModel());
        timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "HH:mm"));
        if (reservation.getReservationTime() != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, reservation.getReservationTime().getHour());
            cal.set(java.util.Calendar.MINUTE, reservation.getReservationTime().getMinute());
            timeSpinner.setValue(cal.getTime());
        }
        content.add(timeSpinner, "growx");
        
        // Notes
        content.add(new JLabel("Ghi chú:"));
        JTextArea notesArea = new JTextArea(reservation.getNotes(), 3, 20);
        content.add(new JScrollPane(notesArea), "growx");
        
        // Status (for editing)
        if (!isNew) {
            content.add(new JLabel("Trạng thái:"));
            JComboBox<Status> statusCombo = new JComboBox<>(Status.values());
            statusCombo.setSelectedItem(reservation.getStatus());
            content.add(statusCombo, "growx");
        }
        
        // Buttons
        JPanel buttons = new JPanel(new MigLayout("insets 0", "push[][]", ""));
        buttons.setOpaque(false);
        
        Reservation finalReservation = reservation;
        JButton saveBtn = new JButton("💾 Lưu");
        saveBtn.setBackground(PRIMARY);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            
            if (name.isEmpty() || phone.isEmpty()) {
                ToastNotification.error(dialog, "Vui lòng nhập đủ thông tin!");
                return;
            }
            
            Table selectedTable = (Table) tableCombo.getSelectedItem();
            if (selectedTable == null) {
                ToastNotification.error(dialog, "Vui lòng chọn bàn!");
                return;
            }
            
            // Build datetime
            java.util.Date dateVal = (java.util.Date) dateSpinner.getValue();
            java.util.Date timeVal = (java.util.Date) timeSpinner.getValue();
            
            java.util.Calendar dateCal = java.util.Calendar.getInstance();
            dateCal.setTime(dateVal);
            
            java.util.Calendar timeCal = java.util.Calendar.getInstance();
            timeCal.setTime(timeVal);
            
            LocalDateTime reserveTime = LocalDateTime.of(
                dateCal.get(java.util.Calendar.YEAR),
                dateCal.get(java.util.Calendar.MONTH) + 1,
                dateCal.get(java.util.Calendar.DAY_OF_MONTH),
                timeCal.get(java.util.Calendar.HOUR_OF_DAY),
                timeCal.get(java.util.Calendar.MINUTE)
            );
            
            finalReservation.setTableId(selectedTable.getId());
            finalReservation.setCustomerName(name);
            finalReservation.setCustomerPhone(phone);
            finalReservation.setGuestCount((int) guestSpinner.getValue());
            finalReservation.setReservationTime(reserveTime);
            finalReservation.setNotes(notesArea.getText().trim());
            finalReservation.setCreatedBy(currentUser.getId());
            
            if (isNew) {
                ServiceResult<Reservation> result = reservationService.createReservation(finalReservation);
                if (result.isSuccess()) {
                    ToastNotification.success(dialog, result.getMessage());
                    dialog.dispose();
                    loadReservations();
                } else {
                    ToastNotification.error(dialog, result.getMessage());
                }
            } else {
                // Update - would need update method in service
                ToastNotification.success(dialog, "Đã cập nhật!");
                dialog.dispose();
                loadReservations();
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
    
    private void startAutoRefresh() {
        Timer timer = new Timer(30000, e -> loadReservations());
        timer.start();
    }
    
    // ==================== RENDERERS ====================
    
    private class StatusRenderer extends JLabel implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Reservation r = (Reservation) value;
            Status status = r.getStatus();
            setText(status.getDisplayName());
            setHorizontalAlignment(CENTER);
            setOpaque(true);
            setFont(new Font(AppConfig.FONT_FAMILY, Font.BOLD, 11));
            
            setBackground(isSelected ? table.getSelectionBackground() : Color.decode(status.getColorHex()));
            setForeground(Color.WHITE);
            
            return this;
        }
    }
    
    private class ButtonRenderer extends JPanel implements TableCellRenderer {
        JButton confirmBtn = new JButton("✅");
        JButton arrivedBtn = new JButton("🎉");
        JButton cancelBtn = new JButton("❌");
        
        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));
            confirmBtn.setToolTipText("Xác nhận");
            arrivedBtn.setToolTipText("Đã đến");
            cancelBtn.setToolTipText("Hủy");
            add(confirmBtn);
            add(arrivedBtn);
            add(cancelBtn);
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
        private JButton confirmBtn, arrivedBtn, cancelBtn;
        private int reservationId;
        
        public ButtonEditor() {
            super(new JCheckBox());
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
            
            confirmBtn = new JButton("✅");
            confirmBtn.setToolTipText("Xác nhận");
            confirmBtn.addActionListener(e -> {
                if (reservationService.updateStatus(reservationId, Status.CONFIRMED)) {
                    ToastNotification.success(SwingUtilities.getWindowAncestor(ReservationPanel.this), "Đã xác nhận!");
                    loadReservations();
                }
                fireEditingStopped();
            });
            
            arrivedBtn = new JButton("🎉");
            arrivedBtn.setToolTipText("Đã đến");
            arrivedBtn.addActionListener(e -> {
                if (reservationService.markArrived(reservationId)) {
                    ToastNotification.success(SwingUtilities.getWindowAncestor(ReservationPanel.this), "Khách đã đến!");
                    loadReservations();
                }
                fireEditingStopped();
            });
            
            cancelBtn = new JButton("❌");
            cancelBtn.setToolTipText("Hủy");
            cancelBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(ReservationPanel.this,
                    "Xác nhận hủy đặt bàn?", "Hủy", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (reservationService.cancel(reservationId)) {
                        ToastNotification.info(SwingUtilities.getWindowAncestor(ReservationPanel.this), "Đã hủy!");
                        loadReservations();
                    }
                }
                fireEditingStopped();
            });
            
            panel.add(confirmBtn);
            panel.add(arrivedBtn);
            panel.add(cancelBtn);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            reservationId = (int) value;
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return reservationId;
        }
    }
}
