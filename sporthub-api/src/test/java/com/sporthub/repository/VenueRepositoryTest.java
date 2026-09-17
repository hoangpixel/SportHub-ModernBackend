package com.sporthub.repository;

import com.sporthub.entity.User;
import com.sporthub.entity.Venue;
import com.sporthub.enums.VenueStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Transactional
class VenueRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void combinesOwnerDistrictAndCityWithStablePagination() {
        User first = owner("first");
        User second = owner("second");
        venue("Arena A", first, "HCMC", "Q7");
        venue("Arena B", first, "HCMC", "Q7");
        venue("Arena C", second, "HCMC", "Q7");
        venue("Arena D", first, "HCMC", "Q1");
        var result = venueRepository.searchVenues("ARENA", "hcmc", "q7", first.getId(),
                PageRequest.of(1, 1, org.springframework.data.domain.Sort.by("name")));
        assertEquals(2, result.getTotalElements());
        assertEquals("Arena B", result.getContent().getFirst().getName());
        assertTrue(venueRepository.searchVenues("missing", null, null, null, PageRequest.of(0, 10)).isEmpty());
        assertEquals(4, venueRepository.searchVenues(null, null, null, null, PageRequest.of(0, 10)).getTotalElements());
    }

    private User owner(String name) {
        User user = new User(); user.setUsername(name); user.setEmail(name + "@example.test"); user.setPasswordHash("test");
        return userRepository.save(user);
    }

    private void venue(String name, User owner, String city, String district) {
        Venue venue = new Venue(); venue.setName(name); venue.setAddress("Street"); venue.setOwner(owner);
        venue.setCity(city); venue.setDistrict(district); venueRepository.save(venue);
    }

    @Test
    void searchVenues_ByKeywordAndCity_Success() {
        User owner = new User();
        owner.setUsername("owner_test");
        owner.setEmail("owner_test@example.com");
        owner.setPasswordHash("hash");
        userRepository.save(owner);

        Venue venue1 = new Venue();
        venue1.setName("SportHub Arena");
        venue1.setAddress("123 Street");
        venue1.setCity("Hồ Chí Minh");
        venue1.setDistrict("Quận 7");
        venue1.setStatus(VenueStatus.ACTIVE);
        venue1.setOwner(owner);
        venueRepository.save(venue1);

        Venue venue2 = new Venue();
        venue2.setName("Green Park");
        venue2.setAddress("456 Street");
        venue2.setCity("Hà Nội");
        venue2.setDistrict("Cầu Giấy");
        venue2.setStatus(VenueStatus.ACTIVE);
        venue2.setOwner(owner);
        venueRepository.save(venue2);

        Page<Venue> result = venueRepository.searchVenues(
                "SportHub", "Hồ Chí Minh", null, null, PageRequest.of(0, 10));

        assertFalse(result.isEmpty());
        assertEquals("SportHub Arena", result.getContent().get(0).getName());
    }

    @Test
    void existsByNameIgnoreCaseAndOwner_Id_ReturnsTrue() {
        User owner = new User();
        owner.setUsername("owner_test2");
        owner.setEmail("owner_test2@example.com");
        owner.setPasswordHash("hash");
        userRepository.save(owner);

        Venue venue = new Venue();
        venue.setName("Arena Prime");
        venue.setAddress("123 Street");
        venue.setStatus(VenueStatus.ACTIVE);
        venue.setOwner(owner);
        venueRepository.save(venue);

        boolean exists = venueRepository.existsByNameIgnoreCaseAndOwner_Id("arena prime", owner.getId());
        assertTrue(exists);
    }
}
