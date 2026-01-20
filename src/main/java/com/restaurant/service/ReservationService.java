package com.restaurant.service;

import com.restaurant.dao.ReservationDAO;
import com.restaurant.dao.interfaces.IReservationDAO;
import com.restaurant.model.Reservation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Reservation Service - Business logic for reservations
 */
public class ReservationService {
    
    private static final Logger logger = LogManager.getLogger(ReservationService.class);
    private static ReservationService instance;
    
    private final IReservationDAO reservationDAO;
    private Timer reminderTimer;
    private Consumer<Reservation> reminderCallback;
    
    private ReservationService() {
        this.reservationDAO = new ReservationDAO();
    }
    
    public static synchronized ReservationService getInstance() {
        if (instance == null) {
            instance = new ReservationService();
        }
        return instance;
    }
    
    /**
     * Create new reservation with time conflict detection
     */
    public ServiceResult<Reservation> createReservation(Reservation reservation) {
        // Validate
        if (reservation.getCustomerName() == null || reservation.getCustomerName().trim().isEmpty()) {
            return ServiceResult.error("Vui lòng nhập tên khách");
        }
        if (reservation.getCustomerPhone() == null || reservation.getCustomerPhone().trim().isEmpty()) {
            return ServiceResult.error("Vui lòng nhập số điện thoại");
        }
        if (reservation.getReservationTime() == null) {
            return ServiceResult.error("Vui lòng chọn thời gian đặt bàn");
        }
        if (reservation.getReservationTime().isBefore(LocalDateTime.now())) {
            return ServiceResult.error("Thời gian đặt bàn phải trong tương lai");
        }
        if (reservation.getGuestCount() <= 0) {
            return ServiceResult.error("Số khách phải lớn hơn 0");
        }
        
        // Check for time conflict (90 minute reservation window by default)
        int reservationDurationMinutes = 90;
        if (reservationDAO.hasTimeConflict(reservation.getTableId(), 
                reservation.getReservationTime(), reservationDurationMinutes, -1)) {
            return ServiceResult.error(
                "Bàn đã có lịch đặt trong khoảng thời gian này.\n" +
                "Vui lòng chọn thời gian cách ít nhất " + reservationDurationMinutes + " phút.");
        }
        
        int id = reservationDAO.create(reservation);
        if (id > 0) {
            reservation.setId(id);
            logger.info("Created reservation: {} for {}", id, reservation.getCustomerName());
            return ServiceResult.success(reservation, "Đặt bàn thành công!");
        }
        
        return ServiceResult.error("Không thể tạo đặt bàn. Vui lòng thử lại.");
    }
    
    /**
     * Get reservation by ID
     */
    public Optional<Reservation> getById(int id) {
        return reservationDAO.findById(id);
    }
    
    /**
     * Get reservations for today
     */
    public List<Reservation> getTodayReservations() {
        return reservationDAO.findByDate(LocalDate.now());
    }
    
    /**
     * Get upcoming reservations for today
     */
    public List<Reservation> getUpcomingToday() {
        return reservationDAO.findUpcomingToday();
    }
    
    /**
     * Find active reservation for a table (today, pending/confirmed)
     */
    public Optional<Reservation> getActiveForTable(int tableId) {
        return reservationDAO.findActiveForTable(tableId);
    }
    
    /**
     * Update reservation status
     */
    public boolean updateStatus(int id, Reservation.Status status) {
        boolean result = reservationDAO.updateStatus(id, status);
        if (result) {
            logger.info("Updated reservation {} status to {}", id, status);
        }
        return result;
    }
    
    /**
     * Mark customer as arrived
     */
    public boolean markArrived(int id) {
        return updateStatus(id, Reservation.Status.ARRIVED);
    }
    
    /**
     * Cancel reservation
     */
    public boolean cancel(int id) {
        return updateStatus(id, Reservation.Status.CANCELLED);
    }
    
    /**
     * Start reminder timer (checks every minute)
     */
    public void startReminderTimer(Consumer<Reservation> callback) {
        this.reminderCallback = callback;
        
        if (reminderTimer != null) {
            reminderTimer.cancel();
        }
        
        reminderTimer = new Timer("ReservationReminder", true);
        reminderTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkReminders();
            }
        }, 0, 60000); // Check every minute
        
        logger.info("Started reservation reminder timer");
    }
    
    /**
     * Stop reminder timer
     */
    public void stopReminderTimer() {
        if (reminderTimer != null) {
            reminderTimer.cancel();
            reminderTimer = null;
        }
    }
    
    /**
     * Check for reservations needing reminder and mark no-shows
     */
    private void checkReminders() {
        // 1. Check for no-shows (past 30 minutes)
        int noShowCount = reservationDAO.markNoShows(30);
        if (noShowCount > 0) {
            logger.info("Auto-marked {} reservations as NO_SHOW", noShowCount);
        }
        
        // 2. Send reminders for upcoming reservations
        List<Reservation> needReminder = reservationDAO.findNeedingReminder();
        
        for (Reservation reservation : needReminder) {
            // Mark as notified first
            reservationDAO.markNotified(reservation.getId());
            
            // Trigger callback
            if (reminderCallback != null) {
                reminderCallback.accept(reservation);
            }
            
            logger.info("Sent reminder for reservation: {} - {} at {}", 
                reservation.getId(), reservation.getCustomerName(), reservation.getFormattedTime());
        }
    }
    
    /**
     * Search by phone
     */
    public List<Reservation> searchByPhone(String phone) {
        return reservationDAO.findByPhone(phone);
    }
}
