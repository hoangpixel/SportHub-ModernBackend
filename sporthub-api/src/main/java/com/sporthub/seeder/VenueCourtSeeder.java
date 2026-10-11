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
