package com.sporthub.service;

import com.sporthub.dto.request.CreateReviewRequest;
import com.sporthub.dto.response.ReviewResponse;
import com.sporthub.entity.Booking;
import com.sporthub.entity.Review;
import com.sporthub.entity.User;
import com.sporthub.enums.BookingStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.BookingRepository;
import com.sporthub.repository.ReviewRepository;
import com.sporthub.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private Booking completedBooking;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("user01");

        user = new User();
        user.setId(1L);
        user.setUsername("user01");

        completedBooking = new Booking();
        completedBooking.setId(1L);
        completedBooking.setUser(user);
        completedBooking.setCourtId(10L);
        completedBooking.setStatus(BookingStatus.COMPLETED);
    }

    @Test
    void createReview_success() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setBookingId(1L);
        request.setRating(5);
        request.setComment("Sân tuyệt vời!");

        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(completedBooking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);

        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        ReviewResponse response = reviewService.createReview(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(5, response.getRating());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_alreadyReviewed_shouldThrowConflict() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setBookingId(1L);

        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(completedBooking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> reviewService.createReview(request));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}
