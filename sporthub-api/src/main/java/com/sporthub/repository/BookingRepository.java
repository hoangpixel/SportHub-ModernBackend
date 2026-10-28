package com.sporthub.repository;

import com.sporthub.entity.Booking;
import com.sporthub.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        SELECT b FROM Booking b
        WHERE b.courtId = :courtId AND b.bookingDate = :date
          AND b.status NOT IN ('CANCELLED', 'EXPIRED')
          AND (b.status <> 'PENDING' OR b.expiresAt > :now)
        ORDER BY b.startTime
        """)
    List<Booking> findOccupying(@Param("courtId") Long courtId,
            @Param("date") LocalDate date, @Param("now") LocalDateTime now);

    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.courtId = :courtId AND b.bookingDate = :date
          AND b.status NOT IN ('CANCELLED', 'EXPIRED')
          AND (b.status <> 'PENDING' OR b.expiresAt > :now)
          AND b.startTime < :endTime AND b.endTime > :startTime
        """)
    boolean existsActiveOverlap(@Param("courtId") Long courtId, @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime,
            @Param("now") LocalDateTime now);

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        UPDATE Booking b SET b.status = 'EXPIRED', b.updatedAt = :now
        WHERE b.status = 'PENDING' AND (b.expiresAt IS NULL OR b.expiresAt <= :now)
        """)
    int expirePending(@Param("now") LocalDateTime now);

    @Query("""
        SELECT b FROM Booking b JOIN b.court c JOIN c.venue v
        WHERE (:owner IS NULL OR v.owner.username = :owner)
          AND (:courtId IS NULL OR b.courtId = :courtId)
          AND (:date IS NULL OR b.bookingDate = :date)
          AND (:status IS NULL OR
              CASE WHEN b.status = 'PENDING' AND (b.expiresAt IS NULL OR b.expiresAt <= :now)
                   THEN 'EXPIRED' ELSE CAST(b.status AS string) END = :status)
        """)
    Page<Booking> findManaged(@Param("owner") String owner, @Param("courtId") Long courtId,
            @Param("date") LocalDate date, @Param("status") String status,
            @Param("now") LocalDateTime now, Pageable pageable);


    @Query("SELECT b.courtId FROM Booking b WHERE b.id = :id")
    Optional<Long> findCourtIdForPayment(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    // Query chống trùng lịch — đây là query JPQL quan trọng nhất của module
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.courtId = :courtId
          AND b.bookingDate = :bookingDate
          AND b.status <> 'CANCELLED'
          AND b.startTime < :endTime
          AND b.endTime > :startTime
        """)
    boolean existsOverlapping(
        @Param("courtId") Long courtId,
        @Param("bookingDate") LocalDate bookingDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Page<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, Pageable pageable);

    Optional<Booking> findByBookingCode(String bookingCode);

    // Lịch sử booking của court (cho owner xem)
    Page<Booking> findByCourtId(Long courtId, Pageable pageable);

    // Aggregate: đếm booking theo status cho dashboard
    @Query("SELECT b.status, COUNT(b) FROM Booking b WHERE b.user.id = :userId GROUP BY b.status")
    List<Object[]> countByStatusForUser(@Param("userId") Long userId);
}
