package com.sporthub.repository;

import com.sporthub.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookingId(Long bookingId);

    Page<Review> findByCourtId(Long courtId, Pageable pageable);

    Page<Review> findByUserId(Long userId, Pageable pageable);

    // Điểm trung bình cho court
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.courtId = :courtId AND r.status = 'VISIBLE'")
    Double averageRatingByCourtId(@Param("courtId") Long courtId);

    Optional<Review> findByBookingId(Long bookingId);
}