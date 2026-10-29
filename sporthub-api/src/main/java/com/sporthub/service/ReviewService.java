package com.sporthub.service;

import com.sporthub.dto.request.CreateReviewRequest;
import com.sporthub.dto.response.ReviewResponse;
import com.sporthub.entity.Booking;
import com.sporthub.entity.Review;
import com.sporthub.entity.User;
import com.sporthub.enums.BookingStatus;
import com.sporthub.enums.ReviewStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.BookingRepository;
import com.sporthub.repository.ReviewRepository;
import com.sporthub.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReviewResponse createReview(CreateReviewRequest request) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Booking booking = bookingRepository.findByIdForUpdate(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy booking id = " + request.getBookingId()));

        // Chỉ review booking của mình
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền đánh giá booking này");
        }

        // Chỉ review khi booking đã COMPLETED
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Chỉ có thể đánh giá sau khi booking hoàn tất");
        }

        // Mỗi booking chỉ review 1 lần
        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new ConflictException("Booking này đã được đánh giá");
        }

        Review review = new Review();
        review.setUser(user);
        review.setCourtId(booking.getCourtId());
        review.setBooking(booking);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setStatus(ReviewStatus.VISIBLE);

        reviewRepository.save(review);
        log.info("Review created: bookingId={}, rating={}", booking.getId(), request.getRating());

        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByCourtId(Long courtId, int page, int size) {
        Pageable pageable = SearchPage.of(page, size, "createdAt", "desc", Set.of("createdAt"));
        return reviewRepository.findByCourtIdAndStatus(courtId, ReviewStatus.VISIBLE, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(Long courtId) {
        return reviewRepository.averageRatingByCourtId(courtId);
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getUser().getId(),
                review.getCourtId(),
                review.getBooking().getId(),
                review.getRating(),
                review.getComment(),
                review.getStatus(),
                review.getCreatedAt()
        );
    }
}
