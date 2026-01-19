package com.restaurant.service;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.LeaveRequest;
import com.restaurant.model.LeaveRequest.LeaveStatus;
import com.restaurant.model.LeaveRequest.LeaveType;
import com.restaurant.model.ShiftTemplate;
import com.restaurant.model.User;
import com.restaurant.model.WorkSchedule;
import com.restaurant.model.WorkSchedule.ScheduleStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Schedule Service - Quản lý lịch làm việc và nghỉ phép
 */
public class ScheduleService {
    
    private static final Logger logger = LogManager.getLogger(ScheduleService.class);
    private static ScheduleService instance;
    
    private ScheduleService() {}
    
    public static synchronized ScheduleService getInstance() {
        if (instance == null) {
            instance = new ScheduleService();
        }
        return instance;
    }
    
    // ==================== SHIFT TEMPLATES ====================
    
    public List<ShiftTemplate> getAllShiftTemplates() {
        List<ShiftTemplate> templates = new ArrayList<>();
        String sql = "SELECT * FROM shift_templates WHERE is_active = TRUE ORDER BY start_time";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                templates.add(mapShiftTemplate(rs));
            }
        } catch (SQLException e) {
            logger.error("Error loading shift templates", e);
        }
        return templates;
    }
    
    private ShiftTemplate mapShiftTemplate(ResultSet rs) throws SQLException {
        ShiftTemplate t = new ShiftTemplate();
        t.setId(rs.getInt("id"));
        t.setName(rs.getString("name"));
        t.setCode(rs.getString("code"));
        t.setStartTime(rs.getTime("start_time").toLocalTime());
        t.setEndTime(rs.getTime("end_time").toLocalTime());
        t.setColor(rs.getString("color"));
        t.setActive(rs.getBoolean("is_active"));
        return t;
    }
    
    // ==================== WORK SCHEDULES ====================
    
    public List<WorkSchedule> getSchedulesByDateRange(LocalDate startDate, LocalDate endDate) {
        List<WorkSchedule> schedules = new ArrayList<>();
        String sql = """
            SELECT ws.*, u.full_name, u.username, st.name as shift_name, st.start_time as template_start, 
                   st.end_time as template_end, st.color as template_color
            FROM work_schedules ws
            JOIN users u ON ws.user_id = u.id
            LEFT JOIN shift_templates st ON ws.shift_template_id = st.id
            WHERE ws.work_date BETWEEN ? AND ?
            ORDER BY ws.work_date, COALESCE(st.start_time, ws.custom_start_time)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapWorkSchedule(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading schedules", e);
        }
        return schedules;
    }
    
    public List<WorkSchedule> getSchedulesByUser(int userId, LocalDate startDate, LocalDate endDate) {
        List<WorkSchedule> schedules = new ArrayList<>();
        String sql = """
            SELECT ws.*, u.full_name, u.username, st.name as shift_name, st.start_time as template_start, 
                   st.end_time as template_end, st.color as template_color
            FROM work_schedules ws
            JOIN users u ON ws.user_id = u.id
            LEFT JOIN shift_templates st ON ws.shift_template_id = st.id
            WHERE ws.user_id = ? AND ws.work_date BETWEEN ? AND ?
            ORDER BY ws.work_date
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapWorkSchedule(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading user schedules", e);
        }
        return schedules;
    }
    
    public boolean createSchedule(WorkSchedule schedule) {
        String sql = """
            INSERT INTO work_schedules (user_id, work_date, shift_template_id, custom_start_time, 
                                        custom_end_time, notes, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE 
                shift_template_id = VALUES(shift_template_id),
                custom_start_time = VALUES(custom_start_time),
                custom_end_time = VALUES(custom_end_time),
                notes = VALUES(notes)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, schedule.getUserId());
            stmt.setDate(2, Date.valueOf(schedule.getWorkDate()));
            
            if (schedule.getShiftTemplateId() != null) {
                stmt.setInt(3, schedule.getShiftTemplateId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            if (schedule.getCustomStartTime() != null) {
                stmt.setTime(4, Time.valueOf(schedule.getCustomStartTime()));
                stmt.setTime(5, Time.valueOf(schedule.getCustomEndTime()));
            } else {
                stmt.setNull(4, Types.TIME);
                stmt.setNull(5, Types.TIME);
            }
            
            stmt.setString(6, schedule.getNotes());
            stmt.setInt(7, schedule.getCreatedBy());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating schedule", e);
            return false;
        }
    }
    
    public boolean deleteSchedule(int scheduleId) {
        String sql = "DELETE FROM work_schedules WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, scheduleId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting schedule", e);
            return false;
        }
    }
    
    public boolean copyWeekSchedule(LocalDate fromWeekStart, LocalDate toWeekStart, int createdBy) {
        // Copy schedules from one week to another
        String sql = """
            INSERT INTO work_schedules (user_id, work_date, shift_template_id, custom_start_time, 
                                        custom_end_time, notes, created_by)
            SELECT user_id, DATE_ADD(work_date, INTERVAL ? DAY), shift_template_id, 
                   custom_start_time, custom_end_time, notes, ?
            FROM work_schedules
            WHERE work_date BETWEEN ? AND DATE_ADD(?, INTERVAL 6 DAY)
            ON DUPLICATE KEY UPDATE shift_template_id = VALUES(shift_template_id)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(fromWeekStart, toWeekStart);
            stmt.setLong(1, daysDiff);
            stmt.setInt(2, createdBy);
            stmt.setDate(3, Date.valueOf(fromWeekStart));
            stmt.setDate(4, Date.valueOf(fromWeekStart));
            
            int rows = stmt.executeUpdate();
            logger.info("Copied {} schedules from {} to {}", rows, fromWeekStart, toWeekStart);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Error copying week schedule", e);
            return false;
        }
    }
    
    private WorkSchedule mapWorkSchedule(ResultSet rs) throws SQLException {
        WorkSchedule ws = new WorkSchedule();
        ws.setId(rs.getInt("id"));
        ws.setUserId(rs.getInt("user_id"));
        ws.setWorkDate(rs.getDate("work_date").toLocalDate());
        
        int templateId = rs.getInt("shift_template_id");
        if (!rs.wasNull()) {
            ws.setShiftTemplateId(templateId);
            ShiftTemplate st = new ShiftTemplate();
            st.setId(templateId);
            st.setName(rs.getString("shift_name"));
            st.setStartTime(rs.getTime("template_start").toLocalTime());
            st.setEndTime(rs.getTime("template_end").toLocalTime());
            st.setColor(rs.getString("template_color"));
            ws.setShiftTemplate(st);
        }
        
        Time customStart = rs.getTime("custom_start_time");
        if (customStart != null) {
            ws.setCustomStartTime(customStart.toLocalTime());
        }
        Time customEnd = rs.getTime("custom_end_time");
        if (customEnd != null) {
            ws.setCustomEndTime(customEnd.toLocalTime());
        }
        
        ws.setStatus(ScheduleStatus.valueOf(rs.getString("status")));
        
        // Map user
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setUsername(rs.getString("username"));
        ws.setUser(user);
        
        return ws;
    }
    
    // ==================== LEAVE REQUESTS ====================
    
    public List<LeaveRequest> getPendingLeaveRequests() {
        return getLeaveRequestsByStatus(LeaveStatus.PENDING);
    }
    
    public List<LeaveRequest> getLeaveRequestsByStatus(LeaveStatus status) {
        List<LeaveRequest> requests = new ArrayList<>();
        String sql = """
            SELECT lr.*, u.full_name, u.username, r.full_name as reviewer_name
            FROM leave_requests lr
            JOIN users u ON lr.user_id = u.id
            LEFT JOIN users r ON lr.reviewed_by = r.id
            WHERE lr.status = ?
            ORDER BY lr.created_at DESC
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapLeaveRequest(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading leave requests", e);
        }
        return requests;
    }
    
    public List<LeaveRequest> getLeaveRequestsByUser(int userId) {
        List<LeaveRequest> requests = new ArrayList<>();
        String sql = """
            SELECT lr.*, u.full_name, u.username, r.full_name as reviewer_name
            FROM leave_requests lr
            JOIN users u ON lr.user_id = u.id
            LEFT JOIN users r ON lr.reviewed_by = r.id
            WHERE lr.user_id = ?
            ORDER BY lr.created_at DESC
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapLeaveRequest(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading user leave requests", e);
        }
        return requests;
    }
    
    public boolean createLeaveRequest(LeaveRequest request) {
        // Validate: must request at least 1 day in advance
        if (!request.isValidRequest()) {
            logger.warn("Invalid leave request: must request at least 1 day in advance");
            return false;
        }
        
        String sql = """
            INSERT INTO leave_requests (user_id, leave_type, start_date, end_date, reason)
            VALUES (?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, request.getUserId());
            stmt.setString(2, request.getLeaveType().name());
            stmt.setDate(3, Date.valueOf(request.getStartDate()));
            stmt.setDate(4, Date.valueOf(request.getEndDate()));
            stmt.setString(5, request.getReason());
            
            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                // Notify all Managers and Admins about new request
                notifyManagersNewLeaveRequest(request);
            }
            return success;
        } catch (SQLException e) {
            logger.error("Error creating leave request", e);
            return false;
        }
    }
    
    private void notifyManagersNewLeaveRequest(LeaveRequest request) {
        // Get staff name
        String staffName = "Nhân viên";
        String sql = "SELECT full_name FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, request.getUserId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    staffName = rs.getString("full_name");
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting staff name", e);
        }
        
        // Get all managers and admins
        String managerSql = """
            SELECT u.id FROM users u
            JOIN roles r ON u.role_id = r.id
            WHERE r.name IN ('ADMIN', 'MANAGER') AND u.is_active = TRUE
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(managerSql);
             ResultSet rs = stmt.executeQuery()) {
            
            NotificationService notifService = NotificationService.getInstance();
            String details = staffName + " xin nghỉ " + request.getLeaveType().getDisplayName() + 
                           " từ " + request.getStartDate() + " đến " + request.getEndDate();
            
            while (rs.next()) {
                int managerId = rs.getInt("id");
                com.restaurant.model.Notification n = new com.restaurant.model.Notification(
                    managerId, 
                    "📝 Yêu cầu nghỉ phép mới", 
                    details,
                    com.restaurant.model.Notification.NotificationType.INFO
                );
                notifService.createNotification(n);
            }
            logger.info("Notified managers about new leave request from {}", staffName);
        } catch (SQLException e) {
            logger.error("Error notifying managers", e);
        }
    }
    
    public boolean approveLeaveRequest(int requestId, int reviewerId) {
        String sql = """
            UPDATE leave_requests 
            SET status = 'APPROVED', reviewed_by = ?, reviewed_at = NOW()
            WHERE id = ? AND status = 'PENDING'
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, reviewerId);
            stmt.setInt(2, requestId);
            
            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                // Update work schedules to ON_LEAVE status
                updateSchedulesForApprovedLeave(requestId, conn);
                
                // Send notification to user
                sendLeaveApprovalNotification(requestId, true, null);
            }
            return success;
        } catch (SQLException e) {
            logger.error("Error approving leave request", e);
            return false;
        }
    }
    
    public boolean rejectLeaveRequest(int requestId, int reviewerId, String reason) {
        String sql = """
            UPDATE leave_requests 
            SET status = 'REJECTED', reviewed_by = ?, reviewed_at = NOW(), rejection_reason = ?
            WHERE id = ? AND status = 'PENDING'
            """;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, reviewerId);
            stmt.setString(2, reason);
            stmt.setInt(3, requestId);
            
            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                // Send notification to user
                sendLeaveApprovalNotification(requestId, false, reason);
            }
            return success;
        } catch (SQLException e) {
            logger.error("Error rejecting leave request", e);
            return false;
        }
    }
    
    private void updateSchedulesForApprovedLeave(int requestId, Connection conn) throws SQLException {
        // Get the leave request details
        String selectSql = "SELECT user_id, start_date, end_date FROM leave_requests WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setInt(1, requestId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    LocalDate start = rs.getDate("start_date").toLocalDate();
                    LocalDate end = rs.getDate("end_date").toLocalDate();
                    
                    // Update existing schedules to ON_LEAVE
                    String updateSql = """
                        UPDATE work_schedules 
                        SET status = 'ON_LEAVE'
                        WHERE user_id = ? AND work_date BETWEEN ? AND ?
                        """;
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, userId);
                        updateStmt.setDate(2, Date.valueOf(start));
                        updateStmt.setDate(3, Date.valueOf(end));
                        updateStmt.executeUpdate();
                    }
                }
            }
        }
    }
    
    private LeaveRequest mapLeaveRequest(ResultSet rs) throws SQLException {
        LeaveRequest lr = new LeaveRequest();
        lr.setId(rs.getInt("id"));
        lr.setUserId(rs.getInt("user_id"));
        lr.setLeaveType(LeaveType.valueOf(rs.getString("leave_type")));
        lr.setStartDate(rs.getDate("start_date").toLocalDate());
        lr.setEndDate(rs.getDate("end_date").toLocalDate());
        lr.setReason(rs.getString("reason"));
        lr.setStatus(LeaveStatus.valueOf(rs.getString("status")));
        
        int reviewedBy = rs.getInt("reviewed_by");
        if (!rs.wasNull()) {
            lr.setReviewedBy(reviewedBy);
            User reviewer = new User();
            reviewer.setId(reviewedBy);
            reviewer.setFullName(rs.getString("reviewer_name"));
            lr.setReviewer(reviewer);
        }
        
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        if (reviewedAt != null) {
            lr.setReviewedAt(reviewedAt.toLocalDateTime());
        }
        
        lr.setRejectionReason(rs.getString("rejection_reason"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            lr.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        // Map user
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setUsername(rs.getString("username"));
        lr.setUser(user);
        
        return lr;
    }
    
    private void sendLeaveApprovalNotification(int requestId, boolean approved, String rejectionReason) {
        try {
            // Get the leave request to get user_id and details
            String sql = "SELECT lr.*, u.full_name FROM leave_requests lr JOIN users u ON lr.user_id = u.id WHERE lr.id = ?";
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, requestId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("user_id");
                        String leaveType = rs.getString("leave_type");
                        LocalDate startDate = rs.getDate("start_date").toLocalDate();
                        LocalDate endDate = rs.getDate("end_date").toLocalDate();
                        
                        String details = "Nghỉ " + leaveType + " từ " + startDate + " đến " + endDate;
                        
                        NotificationService notifService = NotificationService.getInstance();
                        if (approved) {
                            notifService.notifyLeaveApproved(userId, details);
                        } else {
                            notifService.notifyLeaveRejected(userId, details, rejectionReason);
                        }
                        logger.info("Sent leave {} notification to user {}", approved ? "approval" : "rejection", userId);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error sending leave notification", e);
        }
    }
}
