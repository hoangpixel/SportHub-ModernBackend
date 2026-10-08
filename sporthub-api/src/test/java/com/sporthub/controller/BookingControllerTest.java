package com.sporthub.controller;

import com.sporthub.config.PasswordConfig;
import com.sporthub.config.SecurityConfig;
import com.sporthub.dto.request.CreateBookingRequest;
import com.sporthub.dto.response.BookingResponse;
import com.sporthub.enums.BookingStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.GlobalExceptionHandler;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.security.CustomUserDetailsService;
import com.sporthub.security.JwtAuthenticationFilter;
import com.sporthub.security.JwtService;
import com.sporthub.service.BookingService;

import io.jsonwebtoken.MalformedJwtException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import({
        SecurityConfig.class,
        PasswordConfig.class,
        JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class
})
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private String validRequest() {
        return """
                {
                  "courtId": 1,
                  "bookingDate": "%s",
                  "startTime": "08:00:00",
                  "endTime": "09:30:00"
                }
                """.formatted(LocalDate.now().plusDays(2));
    }

    @Test
    void create_withoutLogin_returns401() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message")
                        .value("Chưa đăng nhập hoặc token không hợp lệ"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void create_invalidToken_returns401() throws Exception {
        when(jwtService.extractUsername("bad-token"))
                .thenThrow(new MalformedJwtException("Invalid token"));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer bad-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(bookingService);
    }

    @Test
    @WithMockUser(
            username = "user01",
            authorities = "VIEW_OWN_BOOKINGS"
    )
    void create_withoutPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(bookingService);
    }

    @Test
    @WithMockUser(
            username = "user01",
            authorities = "CREATE_BOOKING"
    )
    void create_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Dữ liệu không hợp lệ"));

        verifyNoInteractions(bookingService);
    }

    @Test
    @WithMockUser(
            username = "user01",
            authorities = "VIEW_OWN_BOOKINGS"
    )
    void get_missingBooking_returns404() throws Exception {
        when(bookingService.getBookingById(999L))
                .thenThrow(new ResourceNotFoundException(
                        "Không tìm thấy booking"
                ));

        mockMvc.perform(get("/api/bookings/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Không tìm thấy booking"));
    }

    @Test
    @WithMockUser(
            username = "user01",
            authorities = "CREATE_BOOKING"
    )
    void create_overlap_returns409() throws Exception {
        when(bookingService.createBooking(any(CreateBookingRequest.class)))
                .thenThrow(new ConflictException(
                        "Sân đã có người đặt trong khung giờ này"
                ));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Sân đã có người đặt trong khung giờ này"));
    }

    @Test
    @WithMockUser(
            username = "user01",
            authorities = "CREATE_BOOKING"
    )
    void create_validRequest_returns201() throws Exception {
        BookingResponse response = new BookingResponse(
                10L,
                1L,
                LocalDate.now().plusDays(2),
                LocalTime.of(8, 0),
                LocalTime.of(9, 30),
                new BigDecimal("150000.00"),
                BookingStatus.PENDING,
                null,
                null
        );

        when(bookingService.createBooking(any(CreateBookingRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.courtId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(150000.0));

        verify(bookingService)
                .createBooking(any(CreateBookingRequest.class));
    }
}