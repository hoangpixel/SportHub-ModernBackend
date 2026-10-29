package com.sporthub.service;

import com.sporthub.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "booking.expiry.enabled", havingValue = "true", matchIfMissing = true)
public class BookingExpiryJob {
    private final BookingRepository bookings;

    @Scheduled(fixedDelayString = "${booking.expiry.delay-ms:15000}")
    @Transactional
    public void expire() {
        bookings.expirePending(BookingTime.now());
    }
}
