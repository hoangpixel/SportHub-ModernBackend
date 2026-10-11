package com.sporthub.integration;

import com.sporthub.entity.*;
import com.sporthub.repository.*;
import com.sporthub.seeder.VenueCourtSeeder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@SpringBootTest
@Transactional
class VenueCourtIntegrationTest {
    // Keep database/security integration real while isolating the unrelated external storage account.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.azure.storage.blob.BlobServiceClient blobServiceClient;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.azure.storage.blob.BlobContainerClient blobContainerClient;
    @Autowired WebApplicationContext context;
    @Autowired EntityManager em;
    @Autowired VenueRepository venues;
    @Autowired CourtRepository courts;
    @Autowired CourtScheduleRepository schedules;
    @Autowired VenueCourtSeeder seeder;
    MockMvc mvc;
    Venue venue;
    Court court;
    CourtSchedule schedule;
    Sport sport;
    private static final String VENUE_BODY = "{\"name\":\"Updated\",\"address\":\"Street\"}";

    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        User owner = new User(); owner.setUsername("integration-owner");
        owner.setEmail("integration@example.test"); owner.setPasswordHash("test"); em.persist(owner);
        venue = new Venue(); venue.setName("Integration venue"); venue.setAddress("Street");
        venue.setOwner(owner); em.persist(venue);
        sport = new Sport(); sport.setName("Integration sport"); em.persist(sport);
        court = new Court(); court.setName("Integration court"); court.setVenue(venue); court.setSport(sport);
        court.setPricePerHour(new BigDecimal("100")); em.persist(court);
        schedule = new CourtSchedule(); schedule.setCourt(court); schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setOpenTime(LocalTime.of(8,0)); schedule.setCloseTime(LocalTime.of(20,0));
        schedule.setAvailable(true); em.persist(schedule); em.flush();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor owner(String name) {
        return user(name).authorities(new SimpleGrantedAuthority("CREATE_VENUE"),
                new SimpleGrantedAuthority("UPDATE_OWN_VENUE"), new SimpleGrantedAuthority("MANAGE_OWN_COURTS"));
    }
    private String courtUrl() { return "/api/owner/venues/" + venue.getId() + "/courts/" + court.getId(); }
    private String courtBody() { return "{\"name\":\"Updated court\",\"sportId\":" + sport.getId() + ",\"pricePerHour\":200}"; }
    private String scheduleBody() { return "{\"dayOfWeek\":\"TUESDAY\",\"openTime\":\"08:00\",\"closeTime\":\"20:00\"}"; }

    @ParameterizedTest @ValueSource(strings = {"PUT", "DELETE"})
    void anotherOwnerCannotChangeVenue(String method) throws Exception {
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), "/api/owner/venues/" + venue.getId())
                .with(owner("other-owner")).contentType(MediaType.APPLICATION_JSON).content(VENUE_BODY))
                .andExpect(status().isForbidden());
        em.clear(); assertThat(venues.findById(venue.getId()).orElseThrow().getName()).isEqualTo("Integration venue");
    }

    @ParameterizedTest @ValueSource(strings = {"PUT", "DELETE"})
    void anotherOwnerCannotChangeCourt(String method) throws Exception {
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), courtUrl())
                .with(owner("other-owner")).contentType(MediaType.APPLICATION_JSON).content(courtBody()))
                .andExpect(status().isForbidden());
        em.clear(); assertThat(courts.findById(court.getId()).orElseThrow().getName()).isEqualTo("Integration court");
    }

    @ParameterizedTest @ValueSource(strings = {"PUT", "DELETE"})
    void anotherOwnerCannotChangeSchedule(String method) throws Exception {
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), courtUrl()+"/schedules/"+schedule.getId())
                .with(owner("other-owner")).contentType(MediaType.APPLICATION_JSON).content(scheduleBody()))
                .andExpect(status().isForbidden());
        em.clear(); assertThat(schedules.findById(schedule.getId()).orElseThrow().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test void anotherOwnerCannotCreateUnderOrListPrivateVenue() throws Exception {
        mvc.perform(post("/api/owner/venues/"+venue.getId()+"/courts").with(owner("other-owner"))
                .contentType(MediaType.APPLICATION_JSON).content(courtBody())).andExpect(status().isForbidden());
        mvc.perform(post(courtUrl()+"/schedules").with(owner("other-owner"))
                .contentType(MediaType.APPLICATION_JSON).content(scheduleBody())).andExpect(status().isForbidden());
        mvc.perform(get("/api/owner/venues/"+venue.getId()+"/courts").with(owner("other-owner")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/courts/"+court.getId())).andExpect(status().isOk());
    }

    @Test void ownerCanUpdateCourtAndSchedule() throws Exception {
        mvc.perform(put(courtUrl()).with(owner("integration-owner")).contentType(MediaType.APPLICATION_JSON).content(courtBody()))
                .andExpect(status().isOk()).andExpect(jsonPath("pricePerHour").value(200));
        mvc.perform(put(courtUrl()+"/schedules/"+schedule.getId()).with(owner("integration-owner"))
                .contentType(MediaType.APPLICATION_JSON).content(scheduleBody()))
                .andExpect(status().isOk()).andExpect(jsonPath("dayOfWeek").value("TUESDAY"));
    }

    @Test void invalidHoursReturn400AndDuplicateScheduleReturns409() throws Exception {
        String invalidCourt = courtBody().replace("}", ",\"openTime\":\"20:00\",\"closeTime\":\"08:00\"}");
        mvc.perform(put(courtUrl()).with(owner("integration-owner")).contentType(MediaType.APPLICATION_JSON).content(invalidCourt))
                .andExpect(status().isBadRequest());
        mvc.perform(post(courtUrl()+"/schedules").with(owner("integration-owner"))
                .contentType(MediaType.APPLICATION_JSON).content(scheduleBody().replace("TUESDAY", "MONDAY")))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest @ValueSource(strings = {"page=-1", "size=0", "size=101", "sortBy=unknown", "direction=sideways", "page=abc", "ownerId=abc&sportId=abc"})
    void invalidPaginationReturns400(String query) throws Exception {
        mvc.perform(get("/api/venues?"+query)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/courts?"+query)).andExpect(status().isBadRequest());
    }

    @Test void reversedPriceRangeReturns400() throws Exception {
        mvc.perform(get("/api/courts?minPrice=200&maxPrice=100")).andExpect(status().isBadRequest());
    }

    @Test void mismatchedParentsReturn404WithoutDeleting() throws Exception {
        mvc.perform(delete("/api/owner/venues/999999/courts/"+court.getId()).with(owner("integration-owner")))
                .andExpect(status().isNotFound());
        Court second = new Court(); second.setName("Second"); second.setVenue(venue); second.setSport(sport);
        second.setPricePerHour(BigDecimal.TEN); em.persist(second); em.flush();
        mvc.perform(delete("/api/owner/venues/"+venue.getId()+"/courts/"+second.getId()+"/schedules/"+schedule.getId())
                .with(owner("integration-owner"))).andExpect(status().isNotFound());
        assertThat(schedules.existsById(schedule.getId())).isTrue();
    }

    @Test void seedRepeatAndPartialRecoveryDoNotDuplicateData() {
        long venueCount = venues.count(); long courtCount = courts.count(); long scheduleCount = schedules.count();
        seeder.seed(); seeder.seed();
        assertThat(venues.count()).isEqualTo(venueCount);
        assertThat(courts.count()).isEqualTo(courtCount);
        assertThat(schedules.count()).isEqualTo(scheduleCount);
        Court demo = courts.findAll().stream().filter(c -> c.getName().equals("Sân cầu lông 1")).findFirst().orElseThrow();
        schedules.deleteAllByCourt_Id(demo.getId()); em.flush(); em.clear();
        seeder.seed(); em.flush();
        assertThat(schedules.count()).isEqualTo(scheduleCount);
        Court recovered = courts.findById(demo.getId()).orElseThrow();
        courts.delete(recovered); em.flush(); em.clear();
        seeder.seed(); em.flush();
        assertThat(venues.count()).isEqualTo(venueCount);
        assertThat(courts.count()).isEqualTo(courtCount);
        assertThat(schedules.count()).isEqualTo(scheduleCount);
    }
}
