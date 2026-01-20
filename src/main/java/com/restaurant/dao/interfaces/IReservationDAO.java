package com.restaurant.dao.interfaces;

import com.restaurant.model.Reservation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Reservation DAO Interface
 */
public interface IReservationDAO {
    
    /**
     * Create new reservation
     */
    int create(Reservation reservation);
    
    /**
     * Find reservation by ID
     */
    Optional<Reservation> findById(int id);
    
    /**
     * Find all reservations for a table
     */
    List<Reservation> findByTableId(int tableId);
    
    /**
     * Find reservations by date
     */
    List<Reservation> findByDate(LocalDate date);
    
    /**
     * Find reservations by customer phone
     */
    List<Reservation> findByPhone(String phone);
    
    /**
     * Find pending reservations that need reminder (within 15 minutes)
     */
    List<Reservation> findNeedingReminder();
    
    /**
     * Find upcoming reservations (today, not cancelled)
     */
    List<Reservation> findUpcomingToday();
    
    /**
     * Update reservation
     */
    boolean update(Reservation reservation);
    
    /**
     * Update reservation status
     */
    boolean updateStatus(int id, Reservation.Status status);
    
    /**
     * Mark reservation as notified
     */
    boolean markNotified(int id);
    
    /**
     * Delete reservation
     */
    boolean delete(int id);
    
    /**
     * Find active reservation for table (PENDING or CONFIRMED, today)
     */
    Optional<Reservation> findActiveForTable(int tableId);
    
    /**
     * Check if there is a time conflict for a table within a time range
     * @param tableId Table ID to check
     * @param startTime Proposed reservation start time
     * @param durationMinutes Duration of the reservation in minutes
     * @param excludeReservationId Exclude this reservation ID (for updates), pass -1 for new
     * @return true if there is a conflict
     */
    boolean hasTimeConflict(int tableId, java.time.LocalDateTime startTime, int durationMinutes, int excludeReservationId);
    
    /**
     * Find reservations that are no-shows (past reservation time by threshold minutes, still PENDING/CONFIRMED)
     * @param thresholdMinutes Minutes past reservation time to consider no-show (e.g., 30)
     */
    List<Reservation> findNoShows(int thresholdMinutes);
    
    /**
     * Mark reservations as no-show  
     */
    int markNoShows(int thresholdMinutes);
}
