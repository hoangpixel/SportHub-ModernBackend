package com.sporthub.seeder;

import com.sporthub.entity.*;
import com.sporthub.enums.CourtStatus;
import com.sporthub.enums.VenueStatus;
import com.sporthub.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class VenueCourtSeeder {

    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final CourtScheduleRepository courtScheduleRepository;
    private final UserRepository userRepository;
    private final SportRepository sportRepository;

    @Transactional
    public void seed() {

        User owner = userRepository
                .findByUsername("owner01")
                .orElse(null);

        if (owner == null) {
            return;
        }

        Venue venue1 = seedVenue(
                owner,
                "SportHub Arena",
                "Trung tâm thể thao đa năng",
                "123 Nguyễn Văn Linh, Quận 7",
                "Hồ Chí Minh",
                "Quận 7",
                "0901234567",
                "/images/venues/arena.png"
        );

        Venue venue2 = seedVenue(
                owner,
                "Green Court Center",
                "Sân thể thao ngoài trời",
                "456 Lê Văn Việt, TP Thủ Đức",
                "Hồ Chí Minh",
                "Thủ Đức",
                "0907654321",
                "/images/venues/green-court.png"
        );

        if (venue1 == null || venue2 == null) {
            return;
        }

        Sport football = sportRepository
                .findByNameIgnoreCase("Football")
                .orElse(null);

        Sport badminton = sportRepository
                .findByNameIgnoreCase("Badminton")
                .orElse(null);

        Sport tennis = sportRepository
                .findByNameIgnoreCase("Tennis")
                .orElse(null);

        if (football != null) {
            Court court = seedCourt(
                    venue1, football,
                    "Sân bóng đá 5 người - A",
                    "Sân cỏ nhân tạo chất lượng cao",
                    new BigDecimal("300000"),
                    LocalTime.of(6, 0),
                    LocalTime.of(22, 0)
            );
            if (court != null) {
                seedWeekdaySchedules(court,
                        LocalTime.of(6, 0),
                        LocalTime.of(22, 0));
            }
        }

        if (badminton != null) {
            Court court = seedCourt(
                    venue1, badminton,
                    "Sân cầu lông 1",
                    "Sân trong nhà có máy lạnh",
                    new BigDecimal("80000"),
                    LocalTime.of(6, 0),
                    LocalTime.of(22, 0)
            );
            if (court != null) {
                seedWeekdaySchedules(court,
                        LocalTime.of(6, 0),
                        LocalTime.of(22, 0));
            }
        }

        if (tennis != null) {
            Court court = seedCourt(
                    venue2, tennis,
                    "Sân tennis A",
                    "Sân ngoài trời mặt cứng",
                    new BigDecimal("150000"),
                    LocalTime.of(5, 30),
                    LocalTime.of(21, 0)
            );
            if (court != null) {
                seedWeekdaySchedules(court,
                        LocalTime.of(5, 30),
                        LocalTime.of(21, 0));
            }
        }

        // Du lieu demo mo rong: giu nguyen 2 venue va 3 court goc.
        User secondOwner = userRepository.findByUsername("owner02").orElse(null);
        if (secondOwner == null) {
            return;
        }
        Venue badmintonClub = seedVenue(secondOwner, "Saigon Badminton Club",
                "Câu lạc bộ cầu lông có máy lạnh", "88 Nguyen Thi Thap, Quan 7",
                "Hồ Chí Minh", "Quận 7", "0908000101", "/images/venues/badminton-club.png");
        Venue footballPark = seedVenue(owner, "Riverside Football Park",
                "Cụm sân bóng đá cỏ nhân tạo", "12 Pham Van Dong, Thu Duc",
                "Hồ Chí Minh", "Thủ Đức", "0908000102", "/images/venues/football-park.png");
        Venue pickleballHub = seedVenue(secondOwner, "Thao Dien Pickleball Hub",
                "Sân pickleball có mái che", "25 Quoc Huong, Thao Dien",
                "Hồ Chí Minh", "Thủ Đức", "0908000103", "/images/venues/pickleball-hub.png");
        Venue volleyballCenter = seedVenue(owner, "City Volleyball Center",
                "Nhà thi đấu bóng chuyền", "50 Ba Thang Hai, Quan 10",
                "Hồ Chí Minh", "Quận 10", "0908000104", "/images/venues/volleyball-center.png");

        Sport basketball = sportRepository.findByNameIgnoreCase("Basketball").orElse(null);
        Sport volleyball = sportRepository.findByNameIgnoreCase("Volleyball").orElse(null);
        Sport pickleball = sportRepository.findByNameIgnoreCase("Pickleball").orElse(null);

        addCourt(venue1, football, "Sân bóng đá 5 người - B", "Sân cỏ nhân tạo khu B", 280000, 6, 22);
        addCourt(venue1, basketball, "Sân bóng rổ trong nhà", "Sân bóng rổ tiêu chuẩn", 220000, 7, 22);
        addCourt(venue2, tennis, "Sân tennis B", "Sân tennis mặt cứng khu B", 170000, 6, 21);
        addCourt(venue2, pickleball, "Sân pickleball A", "Sân pickleball ngoài trời", 120000, 6, 21);
        addCourt(badmintonClub, badminton, "Sân cầu lông 2", "Sân cầu lông có máy lạnh", 90000, 6, 22);
        addCourt(badmintonClub, badminton, "Sân cầu lông 3", "Sân cầu lông thi đấu", 95000, 6, 22);
        addCourt(footballPark, football, "Sân bóng đá 5 người - C", "Sân cỏ nhân tạo ven sông", 320000, 6, 22);
        addCourt(pickleballHub, pickleball, "Sân pickleball B", "Sân pickleball có mái che", 140000, 6, 22);
        addCourt(volleyballCenter, volleyball, "Sân bóng chuyền 1", "Sân bóng chuyền trong nhà", 180000, 7, 22);
    }

    private void addCourt(Venue venue, Sport sport, String name, String description,
                          int price, int openHour, int closeHour) {
        if (venue == null || sport == null) {
            return;
        }
        LocalTime open = LocalTime.of(openHour, 0);
        LocalTime close = LocalTime.of(closeHour, 0);
        Court court = seedCourt(venue, sport, name, description,
                BigDecimal.valueOf(price), open, close);
        if (court != null) {
            seedWeekdaySchedules(court, open, close);
        }
    }

    private Venue seedVenue(
            User owner,
            String name,
            String description,
            String address,
            String city,
            String district,
            String phone,
            String imageUrl) {

        var existing = venueRepository.findByNameIgnoreCaseAndOwner_Id(name, owner.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Venue venue = new Venue();
        venue.setName(name);
        venue.setDescription(description);
        venue.setAddress(address);
        venue.setCity(city);
        venue.setDistrict(district);
        venue.setPhone(phone);
        venue.setImageUrl(imageUrl);
        venue.setStatus(VenueStatus.ACTIVE);
        venue.setOwner(owner);

        return venueRepository.save(venue);
    }

    private Court seedCourt(
            Venue venue,
            Sport sport,
            String name,
            String description,
            BigDecimal pricePerHour,
            LocalTime openTime,
            LocalTime closeTime) {

        var existing = courtRepository.findByNameIgnoreCaseAndVenue_Id(name, venue.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Court court = new Court();
        court.setName(name);
        court.setDescription(description);
        court.setVenue(venue);
        court.setSport(sport);
        court.setPricePerHour(pricePerHour);
        court.setOpenTime(openTime);
        court.setCloseTime(closeTime);
        court.setStatus(CourtStatus.ACTIVE);

        return courtRepository.save(court);
    }

    private void seedWeekdaySchedules(
            Court court,
            LocalTime openTime,
            LocalTime closeTime) {

        for (DayOfWeek day : DayOfWeek.values()) {

            if (courtScheduleRepository.existsByCourt_IdAndDayOfWeek(
                    court.getId(), day)) {
                continue;
            }

            CourtSchedule schedule = new CourtSchedule();
            schedule.setCourt(court);
            schedule.setDayOfWeek(day);
            schedule.setOpenTime(openTime);
            schedule.setCloseTime(closeTime);
            schedule.setAvailable(true);

            courtScheduleRepository.save(schedule);
        }
    }
}
