package com.sporthub.integration;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.sporthub.dto.request.*;
import com.sporthub.dto.response.BookingResponse;
import com.sporthub.entity.*;
import com.sporthub.enums.*;
import com.sporthub.exception.ConflictException;
import com.sporthub.repository.*;
import com.sporthub.seeder.DatabaseSeeder;
import com.sporthub.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class BookingWorkflowIntegrationTest {
    @MockitoBean BlobServiceClient blobServiceClient;
    @MockitoBean BlobContainerClient blobContainerClient;
    @MockitoBean DatabaseSeeder databaseSeeder;
    @MockitoSpyBean BookingRepository bookings;
    @Autowired PaymentRepository payments;
    @Autowired ReviewRepository reviews;
    @Autowired CourtRepository courts;
    @Autowired VenueRepository venues;
    @Autowired BookingService bookingService;
    @Autowired PaymentService paymentService;
    @Autowired ReviewService reviewService;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager entityManager;
    @Autowired WebApplicationContext context;
    @Autowired tools.jackson.databind.ObjectMapper objectMapper;
    TransactionTemplate transactions;
    MockMvc mvc;
    Court court;
    User player;
    User secondPlayer;
    User owner;
    User anotherOwner;
    LocalDate date;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String mysqlUrl = System.getenv("BOOKING_TEST_MYSQL_URL");
        boolean mysql = mysqlUrl != null && !mysqlUrl.isBlank();
        if (mysql && !mysqlUrl.matches("jdbc:mysql://(?:localhost|127[.]0[.]0[.]1)(?::[0-9]+)?/sporthub_booking_test_[A-Za-z0-9_]+(?:[?].*)?")) {
            throw new IllegalArgumentException("Use a dedicated local sporthub_booking_test_* database only");
        }
        registry.add("spring.datasource.url", () -> mysql ? mysqlUrl
                : "jdbc:h2:mem:booking_workflow;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        registry.add("spring.datasource.username", () -> mysql ? System.getenv("BOOKING_TEST_MYSQL_USER") : "sa");
        registry.add("spring.datasource.password", () -> mysql ? System.getenv("BOOKING_TEST_MYSQL_PASSWORD") : "");
        registry.add("spring.datasource.driver-class-name", () -> mysql ? "com.mysql.cj.jdbc.Driver" : "org.h2.Driver");
        registry.add("spring.jpa.database-platform", () -> mysql ? "org.hibernate.dialect.MySQLDialect" : "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> mysql ? "validate" : "create-drop");
        registry.add("spring.flyway.enabled", () -> mysql);
        registry.add("spring.flyway.clean-disabled", () -> true);
    }

    @BeforeEach
    void setUp() {
        org.springframework.security.test.context.TestSecurityContextHolder.clearContext();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        transactions = new TransactionTemplate(transactionManager);
        date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(2);
        transactions.executeWithoutResult(status -> {
            owner = persistUser("owner"); player = persistUser("player");
            secondPlayer = persistUser("player2"); anotherOwner = persistUser("other-owner");
            Sport sport = new Sport(); sport.setName("Booking sport " + UUID.randomUUID()); entityManager.persist(sport);
            Venue venue = new Venue(); venue.setName("Booking venue"); venue.setAddress("Test address");
            venue.setOwner(owner); entityManager.persist(venue);
            court = new Court(); court.setName("Booking court"); court.setVenue(venue); court.setSport(sport);
            court.setPricePerHour(new BigDecimal("150000")); court.setOpenTime(LocalTime.of(6, 0));
            court.setCloseTime(LocalTime.of(22, 0)); entityManager.persist(court);
        });
    }

    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.test.context.TestSecurityContextHolder.clearContext();
    }

    @Test
    void paidBookingCompletesAndCanBeReviewedThroughPublicApi() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        assertThat(bookings.findById(bookingId).orElseThrow().getTotalAmount()).isEqualByComparingTo("300000");
        mvc.perform(get("/api/owner/bookings/{id}", bookingId).with(user(anotherOwner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/owner/bookings/{id}", bookingId).with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
        as(player, () -> paymentService.createPayment(payment(bookingId)));
        assertThat(bookings.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(payments.findByBookingId(bookingId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThatThrownBy(() -> as(player, () -> bookingService.cancelBooking(bookingId))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> as(owner, () -> bookingService.completeBooking(bookingId), "ROLE_OWNER"))
                .isInstanceOf(ConflictException.class);
        ageBooking(bookingId);
        as(owner, () -> bookingService.completeBooking(bookingId), "ROLE_OWNER");
        as(player, () -> reviewService.createReview(review(bookingId, 5)));
        assertThatThrownBy(() -> as(player, () -> reviewService.createReview(review(bookingId, 5))))
                .isInstanceOf(ConflictException.class);
        mvc.perform(get("/api/reviews/court/{id}", court.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/reviews/court/{id}/average", court.getId()))
                .andExpect(status().isOk()).andExpect(content().string("5.0"));
    }

    @Test
    void cancellationReleasesTimeAndAdjacentBookingsAreAllowed() {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        as(player, () -> bookingService.cancelBooking(bookingId));
        assertThat(as(secondPlayer, () -> bookingService.createBooking(request(8, 10))).getStatus())
                .isEqualTo(BookingStatus.PENDING);
        assertThat(as(player, () -> bookingService.createBooking(request(10, 12))).getStatus())
                .isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void hiddenReviewsAreExcludedFromBothPublicListAndAverage() throws Exception {
        Long bookingId = paidCompletedBooking();
        Long reviewId = as(player, () -> reviewService.createReview(review(bookingId, 1))).getId();
        transactions.executeWithoutResult(status -> {
            Review stored = reviews.findById(reviewId).orElseThrow(); stored.setStatus(ReviewStatus.HIDDEN);
        });
        mvc.perform(get("/api/reviews/court/{id}", court.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        assertThat(reviewService.getAverageRating(court.getId())).isEqualTo(0.0);
    }

    @Test
    void securityChainSeparatesAnonymousPermissionAndOwnership() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        mvc.perform(get("/api/bookings/{id}", bookingId)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/owner/bookings/{id}", bookingId).with(user(owner.getUsername())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/bookings/{id}", bookingId).with(user(secondPlayer.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWN_BOOKINGS"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/bookings/{id}", bookingId).with(user("admin")
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/payments").with(user(secondPlayer.getUsername())
                .authorities(new SimpleGrantedAuthority("CREATE_BOOKING")))
                .contentType(MediaType.APPLICATION_JSON).content(paymentJson(bookingId)))
                .andExpect(status().isForbidden());
        as(player, () -> paymentService.createPayment(payment(bookingId)));
        mvc.perform(get("/api/payments/booking/{id}", bookingId).with(user(secondPlayer.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWN_BOOKINGS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void inputValidationAndMissingCourtReturnExpectedHttpStatus() throws Exception {
        mvc.perform(post("/api/bookings").with(user(player.getUsername())
                .authorities(new SimpleGrantedAuthority("CREATE_BOOKING")))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        String body = "{\"courtId\":9223372036854775807,\"bookingDate\":\"" + date
                + "\",\"startTime\":\"08:00\",\"endTime\":\"10:00\"}";
        mvc.perform(post("/api/bookings").with(user(player.getUsername())
                .authorities(new SimpleGrantedAuthority("CREATE_BOOKING")))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
        mvc.perform(post("/api/payments").with(user(player.getUsername())
                .authorities(new SimpleGrantedAuthority("CREATE_BOOKING")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"bookingId\":1,\"paymentMethod\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paymentFailureAfterInsertRollsBackPaymentAndBooking() {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        doThrow(new IllegalStateException("Injected failure after payment insert"))
                .when(bookings).saveAndFlush(any(Booking.class));
        assertThatThrownBy(() -> as(player, () -> paymentService.createPayment(payment(bookingId))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(payments.findByBookingId(bookingId)).isEmpty();
        assertThat(bookings.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @RepeatedTest(3)
    void twoUsersCompetingForOverlappingTimesOnlyCreateOneBooking() throws Exception {
        List<Integer> outcomes = race(
                () -> as(player, () -> bookingService.createBooking(request(8, 10))),
                () -> as(secondPlayer, () -> bookingService.createBooking(request(9, 11))));
        assertThat(outcomes).containsExactlyInAnyOrder(200, 409);
        assertThat(bookings.findByCourtId(court.getId(), PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void concurrentPaymentsOnlyCreateOnePayment() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        assertThat(race(() -> as(player, () -> paymentService.createPayment(payment(bookingId))),
                () -> as(player, () -> paymentService.createPayment(payment(bookingId)))))
                .containsExactlyInAnyOrder(200, 409);
        assertThat(payments.findAll().stream().filter(stored -> stored.getBooking().getId().equals(bookingId)).count())
                .isEqualTo(1);
        assertThat(bookings.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void paymentRacingCancellationCannotLeavePaidCancelledBooking() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        assertThat(race(() -> as(player, () -> paymentService.createPayment(payment(bookingId))),
                () -> as(player, () -> bookingService.cancelBooking(bookingId))))
                .containsExactlyInAnyOrder(200, 409);
        BookingStatus status = bookings.findById(bookingId).orElseThrow().getStatus();
        assertThat(status).isIn(BookingStatus.CONFIRMED, BookingStatus.CANCELLED);
        assertThat(payments.existsByBookingId(bookingId)).isEqualTo(status == BookingStatus.CONFIRMED);
    }

    @Test
    void concurrentReviewsOnlyCreateOneReview() throws Exception {
        Long bookingId = paidCompletedBooking();
        assertThat(race(() -> as(player, () -> reviewService.createReview(review(bookingId, 5))),
                () -> as(player, () -> reviewService.createReview(review(bookingId, 4)))))
                .containsExactlyInAnyOrder(200, 409);
        assertThat(reviews.findByCourtId(court.getId(), PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void ownerCreatedCourtAndScheduleFlowThroughBookingPaymentAndReview() throws Exception {
        String venueBody = mvc.perform(post("/api/owner/venues").with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("CREATE_VENUE")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"HTTP venue\",\"address\":\"Test address\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long venueId = objectMapper.readTree(venueBody).get("id").asLong();
        String courtBody = mvc.perform(post("/api/owner/venues/{id}/courts", venueId)
                .with(user(owner.getUsername()).authorities(new SimpleGrantedAuthority("MANAGE_OWN_COURTS")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"HTTP court\",\"sportId\":"
                        + court.getSport().getId() + ",\"pricePerHour\":240000,\"openTime\":\"06:00\",\"closeTime\":\"22:00\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long courtId = objectMapper.readTree(courtBody).get("id").asLong();
        mvc.perform(post("/api/owner/venues/{venueId}/courts/{courtId}/schedules", venueId, courtId)
                .with(user(owner.getUsername()).authorities(new SimpleGrantedAuthority("MANAGE_OWN_COURTS")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"dayOfWeek\":\"" + date.getDayOfWeek()
                        + "\",\"openTime\":\"09:00\",\"closeTime\":\"12:00\",\"available\":true}"))
                .andExpect(status().isCreated());
        String bookingBody = "{\"courtId\":" + courtId + ",\"bookingDate\":\"" + date
                + "\",\"startTime\":\"09:00\",\"endTime\":\"10:30\"}";
        mvc.perform(post("/api/bookings").with(user(player.getUsername())
                .authorities(new SimpleGrantedAuthority("CREATE_BOOKING")))
                .contentType(MediaType.APPLICATION_JSON).content(bookingBody.replace("09:00", "08:00")))
                .andExpect(status().isConflict());
        String created = mvc.perform(post("/api/bookings").with(user(player.getUsername())
                .authorities(new SimpleGrantedAuthority("CREATE_BOOKING")))
                .contentType(MediaType.APPLICATION_JSON).content(bookingBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(360000))
                .andReturn().getResponse().getContentAsString();
        long bookingId = objectMapper.readTree(created).get("id").asLong();
        mvc.perform(get("/api/owner/bookings/{id}", bookingId).with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("ROLE_OWNER"), new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/payments").with(user(player.getUsername())
                .authorities(new SimpleGrantedAuthority("CREATE_BOOKING")))
                .contentType(MediaType.APPLICATION_JSON).content(paymentJson(bookingId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.amount").value(360000));
        ageBooking(bookingId);
        mvc.perform(patch("/api/bookings/{id}/complete", bookingId).with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("ROLE_OWNER"), new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/reviews").with(user(player.getUsername())
                .authorities(new SimpleGrantedAuthority("CREATE_REVIEW")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"bookingId\":" + bookingId + ",\"rating\":5}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/reviews/court/{id}", courtId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void expiredHoldIsVisibleAndDoesNotBlockBeforeTheJobRuns() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        transactions.executeWithoutResult(status -> bookings.findById(bookingId).orElseThrow()
                .setExpiresAt(BookingTime.now().minusSeconds(1)));
        assertThat(as(player, () -> bookingService.getBookingById(bookingId)).getStatus())
                .isEqualTo(BookingStatus.EXPIRED);
        assertThat(bookings.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThatThrownBy(() -> as(player, () -> paymentService.createPayment(payment(bookingId))))
                .isInstanceOf(ConflictException.class);
        mvc.perform(get("/api/courts/{id}/availability", court.getId()).param("date", date.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.occupied.length()").value(0));
        assertThat(as(secondPlayer, () -> bookingService.createBooking(request(8, 10))).getStatus())
                .isEqualTo(BookingStatus.PENDING);
        transactions.executeWithoutResult(status -> new BookingExpiryJob(bookings).expire());
        assertThat(bookings.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(payments.existsByBookingId(bookingId)).isFalse();
    }

    @Test
    void expiryJobCannotExpirePaidBookings() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        as(player, () -> paymentService.createPayment(payment(bookingId)));
        transactions.executeWithoutResult(status -> bookings.findById(bookingId).orElseThrow()
                .setExpiresAt(BookingTime.now().minusMinutes(1)));
        transactions.executeWithoutResult(status -> new BookingExpiryJob(bookings).expire());
        assertThat(bookings.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void expiryRacingLatePaymentNeverCreatesAPayment() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        transactions.executeWithoutResult(status -> bookings.findById(bookingId).orElseThrow()
                .setExpiresAt(BookingTime.now().minusSeconds(1)));
        assertThat(race(() -> transactions.execute(status -> bookings.expirePending(BookingTime.now())),
                () -> as(player, () -> paymentService.createPayment(payment(bookingId)))))
                .containsExactlyInAnyOrder(200, 409);
        assertThat(bookings.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(payments.existsByBookingId(bookingId)).isFalse();
    }

    @Test
    void managedListsFilterOwnershipAndEffectiveExpiry() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        mvc.perform(get("/api/owner/bookings").with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].customerUsername").value(player.getUsername()));
        mvc.perform(get("/api/owner/bookings").with(user(anotherOwner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/admin/bookings/{id}", bookingId).with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/bookings/{id}", bookingId).with(user("admin")
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(bookingId));
        transactions.executeWithoutResult(status -> bookings.findById(bookingId).orElseThrow()
                .setExpiresAt(BookingTime.now().minusSeconds(1)));
        mvc.perform(get("/api/owner/bookings").param("status", "EXPIRED").param("courtId", court.getId().toString())
                .param("date", date.toString()).with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("EXPIRED"));
        mvc.perform(get("/api/owner/bookings").param("status", "PENDING").with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/owner/bookings").param("size", "101").with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerCancelsPendingWithReasonAndCustomerCanReadIt() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        mvc.perform(patch("/api/owner/bookings/{id}/cancel", bookingId).with(user(anotherOwner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Court damaged\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/owner/bookings/{id}/cancel", bookingId).with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/owner/bookings/{id}/cancel", bookingId).with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Court damaged\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/api/bookings/{id}", bookingId).with(user(player.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWN_BOOKINGS"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cancellationReason").value("Court damaged"))
                .andExpect(jsonPath("$.cancelledBy").value(owner.getUsername()));
        assertThat(as(secondPlayer, () -> bookingService.createBooking(request(8, 10))).getStatus())
                .isEqualTo(BookingStatus.PENDING);
        assertThatThrownBy(() -> as(player, () -> paymentService.createPayment(payment(bookingId))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void paidIncidentRequiresRefundAndOnlyAdminCanRecordManualSettlement() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        as(player, () -> paymentService.createPayment(payment(bookingId)));
        mvc.perform(patch("/api/owner/bookings/{id}/cancel", bookingId).with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Lighting failure\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("REFUND_PENDING"));
        mvc.perform(patch("/api/admin/bookings/{id}/refund", bookingId).with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reference\":\"manual-transfer-123\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/bookings/{id}/refund", bookingId).with(user("admin")
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reference\":\"manual-transfer-123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("REFUNDED"))
                .andExpect(jsonPath("$.refundReference").value("manual-transfer-123"));
        Payment stored = payments.findByBookingId(bookingId).orElseThrow();
        assertThat(stored.getRefundedAt()).isNotNull();
        assertThat(stored.getRefundedBy()).isEqualTo("admin");
        assertThatThrownBy(() -> as(owner, () -> bookingService.completeBooking(bookingId), "ROLE_OWNER"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> as(owner, () -> bookingService.markNoShow(bookingId, "absent"), "ROLE_OWNER"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> as(owner, () -> bookingService.recordRefund(bookingId, "duplicate"), "ROLE_ADMIN"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void noShowOnlyAfterEndKeepsPaymentAndPreventsReview() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        as(player, () -> paymentService.createPayment(payment(bookingId)));
        mvc.perform(patch("/api/owner/bookings/{id}/no-show", bookingId).with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Customer absent\"}"))
                .andExpect(status().isConflict());
        ageBooking(bookingId);
        mvc.perform(patch("/api/owner/bookings/{id}/no-show", bookingId).with(user(anotherOwner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Customer absent\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/owner/bookings/{id}/no-show", bookingId).with(user(owner.getUsername())
                .authorities(new SimpleGrantedAuthority("VIEW_OWNER_BOOKINGS")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Customer absent\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NO_SHOW"))
                .andExpect(jsonPath("$.outcomeReason").value("Customer absent"));
        assertThat(payments.findByBookingId(bookingId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThatThrownBy(() -> as(player, () -> reviewService.createReview(review(bookingId, 5))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> as(owner, () -> bookingService.markNoShow(bookingId, "again"), "ROLE_OWNER"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> as(owner, () -> bookingService.completeBooking(bookingId), "ROLE_OWNER"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void paymentRacingOwnerIncidentCancellationIsConsistent() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        List<Integer> outcomes = race(() -> as(player, () -> paymentService.createPayment(payment(bookingId))),
                () -> as(owner, () -> bookingService.cancelManagedBooking(bookingId, "Court damaged"), "ROLE_OWNER"));
        assertThat(outcomes).contains(200).allMatch(code -> code == 200 || code == 409);
        assertThat(bookings.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        payments.findByBookingId(bookingId).ifPresent(payment ->
                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING));
    }

    @Test
    void completeRacingNoShowOnlyRecordsOneOutcome() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        as(player, () -> paymentService.createPayment(payment(bookingId)));
        ageBooking(bookingId);
        assertThat(race(() -> as(owner, () -> bookingService.completeBooking(bookingId), "ROLE_OWNER"),
                () -> as(owner, () -> bookingService.markNoShow(bookingId, "Absent"), "ROLE_OWNER")))
                .containsExactlyInAnyOrder(200, 409);
        assertThat(bookings.findById(bookingId).orElseThrow().getStatus())
                .isIn(BookingStatus.COMPLETED, BookingStatus.NO_SHOW);
    }

    @Test
    void availabilityIsPublicAndReflectsHoldsPaymentsAndCancellation() throws Exception {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        mvc.perform(get("/api/courts/{id}/availability", court.getId()).param("date", date.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.occupied.length()").value(1))
                .andExpect(jsonPath("$.available.length()").value(2))
                .andExpect(jsonPath("$.available[0].startTime").value("06:00:00"))
                .andExpect(jsonPath("$.available[0].endTime").value("08:00:00"))
                .andExpect(jsonPath("$.available[1].startTime").value("10:00:00"));
        as(player, () -> paymentService.createPayment(payment(bookingId)));
        mvc.perform(get("/api/courts/{id}/availability", court.getId()).param("date", date.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.occupied.length()").value(1));
        as(owner, () -> bookingService.cancelManagedBooking(bookingId, "Court damaged"), "ROLE_OWNER");
        mvc.perform(get("/api/courts/{id}/availability", court.getId()).param("date", date.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.occupied.length()").value(0))
                .andExpect(jsonPath("$.customerUsername").doesNotExist());
        mvc.perform(get("/api/courts/{id}/availability", court.getId()).param("date", date.minusDays(3).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.acceptingBookings").value(false));
        mvc.perform(get("/api/courts/9223372036854775807/availability").param("date", date.toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/courts/{id}/availability", court.getId()).param("date", "invalid"))
                .andExpect(status().isBadRequest());
    }

    private Long paidCompletedBooking() {
        Long bookingId = as(player, () -> bookingService.createBooking(request(8, 10))).getId();
        as(player, () -> paymentService.createPayment(payment(bookingId)));
        ageBooking(bookingId);
        as(owner, () -> bookingService.completeBooking(bookingId), "ROLE_OWNER");
        return bookingId;
    }

    private void ageBooking(Long bookingId) {
        transactions.executeWithoutResult(status -> bookings.findById(bookingId).orElseThrow()
                .setBookingDate(date.minusDays(3)));
    }

    private User persistUser(String prefix) {
        User result = new User(); result.setUsername(prefix + "-" + UUID.randomUUID());
        result.setEmail(result.getUsername() + "@example.test"); result.setPasswordHash("test-only");
        entityManager.persist(result); return result;
    }

    private CreateBookingRequest request(int startHour, int endHour) {
        CreateBookingRequest result = new CreateBookingRequest(); result.setCourtId(court.getId());
        result.setBookingDate(date); result.setStartTime(LocalTime.of(startHour, 0));
        result.setEndTime(LocalTime.of(endHour, 0)); return result;
    }

    private CreatePaymentRequest payment(Long bookingId) {
        CreatePaymentRequest result = new CreatePaymentRequest(); result.setBookingId(bookingId);
        result.setPaymentMethod("CASH"); return result;
    }

    private String paymentJson(Long bookingId) {
        return "{\"bookingId\":" + bookingId + ",\"paymentMethod\":\"CASH\"}";
    }

    private CreateReviewRequest review(Long bookingId, int rating) {
        CreateReviewRequest result = new CreateReviewRequest(); result.setBookingId(bookingId);
        result.setRating(rating); return result;
    }

    private <Result> Result as(User actor, Supplier<Result> action, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor.getUsername(), null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
        try { return action.get(); } finally { SecurityContextHolder.clearContext(); }
    }

    private List<Integer> race(Supplier<?> first, Supplier<?> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> results = List.of(executor.submit(contender(first, ready, start)),
                    executor.submit(contender(second, ready, start)));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            return List.of(results.get(0).get(20, TimeUnit.SECONDS), results.get(1).get(20, TimeUnit.SECONDS));
        } finally { start.countDown(); executor.shutdownNow(); executor.awaitTermination(10, TimeUnit.SECONDS); }
    }

    private Callable<Integer> contender(Supplier<?> operation, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("Start barrier timed out"); }
            try { operation.get(); return 200; } catch (ConflictException conflict) { return 409; }
        };
    }
}
