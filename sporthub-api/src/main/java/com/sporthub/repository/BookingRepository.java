package com.sporthub.repository;

import com.sporthub.entity.Booking;
import com.sporthub.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

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