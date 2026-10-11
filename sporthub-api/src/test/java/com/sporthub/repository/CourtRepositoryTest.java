package com.sporthub.repository;

import com.sporthub.entity.*;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class CourtRepositoryTest {
    @Autowired EntityManager em;
    @Autowired CourtRepository courts;
    Venue venue;
    Sport sport;

    @BeforeEach void fixtures() {
        User owner = new User();
        owner.setUsername("search-owner"); owner.setEmail("search@example.test"); owner.setPasswordHash("test");
        em.persist(owner);
        venue = new Venue(); venue.setName("Arena"); venue.setAddress("Street"); venue.setOwner(owner);
        em.persist(venue);
        sport = new Sport(); sport.setName("Football"); em.persist(sport);
        addCourt("Alpha", venue, sport, "100");
        addCourt("Beta", venue, sport, "200");
        addCourt("Gamma", venue, sport, "300");
        Venue other = new Venue(); other.setName("Other"); other.setAddress("Street"); other.setOwner(owner);
        em.persist(other);
        Sport tennis = new Sport(); tennis.setName("Tennis"); em.persist(tennis);
        addCourt("Alpha elsewhere", other, sport, "150");
        addCourt("Alpha tennis", venue, tennis, "150");
        em.flush(); em.clear();
    }

    private void addCourt(String name, Venue venue, Sport sport, String price) {
        Court c = new Court(); c.setName(name); c.setVenue(venue); c.setSport(sport);
        c.setPricePerHour(new BigDecimal(price)); em.persist(c);
    }

    @Test void combinesKeywordSportVenueAndInclusivePriceBounds() {
        var result = courts.searchCourts("ALPHA", sport.getId(), venue.getId(),
                new BigDecimal("100"), new BigDecimal("100"), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(Court::getName).containsExactly("Alpha");
    }

    @Test void paginatesAndSortsWithCorrectTotal() {
        var result = courts.searchCourts("", sport.getId(), venue.getId(), null, null,
                PageRequest.of(1, 1, Sort.by("pricePerHour").descending()));
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getContent()).extracting(Court::getName).containsExactly("Beta");
    }

    @Test void derivedQueryFiltersBothVenueAndSport() {
        var result = courts.findByVenue_IdAndSport_Id(venue.getId(), sport.getId(),
                PageRequest.of(0, 10, Sort.by("name")));
        assertThat(result.getContent()).extracting(Court::getName).containsExactly("Alpha", "Beta", "Gamma");
    }

    @Test void nullFiltersReturnAllAndUnknownKeywordReturnsEmpty() {
        assertThat(courts.searchCourts(null, null, null, null, null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(5);
        assertThat(courts.searchCourts("unmatched", null, null, null, null, PageRequest.of(0, 10))).isEmpty();
    }

    @Test void pageDtoRelationsNeedOnlyContentAndCountQueries() {
        var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var result = courts.searchCourts(null, null, null, null, null,
                PageRequest.of(0, 2, Sort.by("id")));
        result.forEach(c -> {
            assertThat(c.getVenue().getName()).isNotBlank();
            assertThat(c.getSport().getName()).isNotBlank();
        });
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
    }
}
