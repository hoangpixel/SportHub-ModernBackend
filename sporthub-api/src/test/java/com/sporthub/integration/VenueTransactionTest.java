package com.sporthub.integration;

import com.sporthub.entity.User;
import com.sporthub.entity.Venue;
import com.sporthub.repository.UserRepository;
import com.sporthub.repository.VenueRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(VenueTransactionTest.ProbeConfiguration.class)
class VenueTransactionTest {

    @Autowired
    private TransactionProbe probe;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @Test
    void successfulOperationCommitsData() {
        String marker = "tx-" + UUID.randomUUID();

        try {
            assertEquals(0L, countVenues(marker));

            probe.createVenue(marker, false);

            assertEquals(1L, countVenues(marker));

            System.out.println(
                    "COMMIT VERIFIED: venue exists in a new transaction"
            );
        } finally {
            cleanup(marker);
        }
    }

    @Test
    void runtimeExceptionRollsBackFlushedData() {
        String marker = "tx-" + UUID.randomUUID();

        try {
            assertEquals(0L, countVenues(marker));

            IllegalStateException failure = assertThrows(
                    IllegalStateException.class,
                    () -> probe.createVenue(marker, true)
            );

            assertEquals("Intentional failure after flush", failure.getMessage());
            assertEquals(0L, countVenues(marker));

            System.out.println(
                    "ROLLBACK VERIFIED: venue is absent in a new transaction"
            );
        } finally {
            cleanup(marker);
        }
    }

    private TransactionTemplate newTransaction() {
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(
                Propagation.REQUIRES_NEW.value()
        );
        return transaction;
    }

    private long countVenues(String marker) {
        Long count = newTransaction().execute(status ->
                entityManager.createQuery(
                                "select count(venue) from Venue venue "
                                        + "where venue.name = :name",
                                Long.class
                        )
                        .setParameter("name", marker)
                        .getSingleResult()
        );

        return count.longValue();
    }

    private void cleanup(String marker) {
        newTransaction().executeWithoutResult(status -> {
            entityManager.createQuery(
                            "delete from Venue venue where venue.name = :name"
                    )
                    .setParameter("name", marker)
                    .executeUpdate();

            entityManager.createQuery(
                            "delete from User owner where owner.username = :username"
                    )
                    .setParameter("username", marker)
                    .executeUpdate();
        });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {

        @Bean
        TransactionProbe transactionProbe(
                VenueRepository venues,
                UserRepository users) {
            return new TransactionProbe(venues, users);
        }
    }

    public static class TransactionProbe {

        private final VenueRepository venues;
        private final UserRepository users;

        public TransactionProbe(
                VenueRepository venues,
                UserRepository users) {
            this.venues = venues;
            this.users = users;
        }

        @Transactional
        public void createVenue(String marker, boolean failAfterFlush) {
            User owner = new User();
            owner.setUsername(marker);
            owner.setEmail(marker + "@example.test");
            owner.setPasswordHash("test-only");
            users.saveAndFlush(owner);

            Venue venue = new Venue();
            venue.setName(marker);
            venue.setAddress("Transaction test address");
            venue.setOwner(owner);

            venues.saveAndFlush(venue);

            System.out.println(
                    "WRITE FLUSHED: venueId=" + venue.getId()
                            + ", failAfterFlush=" + failAfterFlush
            );

            if (failAfterFlush) {
                throw new IllegalStateException(
                        "Intentional failure after flush"
                );
            }
        }
    }
}