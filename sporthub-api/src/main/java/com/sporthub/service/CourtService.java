package com.sporthub.service;

import com.sporthub.dto.request.CourtRequest;
import com.sporthub.dto.response.CourtResponse;
import com.sporthub.entity.Court;
import com.sporthub.entity.Sport;
import com.sporthub.entity.Venue;
import com.sporthub.enums.CourtStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.CourtRepository;
import com.sporthub.repository.SportRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;
    private final SportRepository sportRepository;
    private final VenueService venueService;

    @Transactional(readOnly = true)
    public Page<CourtResponse> searchCourts(
            String keyword,
            Long sportId,
            Long venueId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String direction) {

        if ((minPrice != null && minPrice.signum() < 0)
                || (maxPrice != null && maxPrice.signum() < 0)
                || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)) {
            throw new IllegalArgumentException("Khoảng giá không hợp lệ");
        }
        Pageable pageable = SearchPage.of(page, size, sortBy, direction,
                Set.of("id", "name", "pricePerHour", "createdAt", "updatedAt"));

        Page<Court> courts = courtRepository.searchCourts(
                keyword, sportId, venueId,
                minPrice, maxPrice, pageable);

        return courts.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CourtResponse> getCourtsByVenue(
            Long venueId,
            int page,
            int size) {

        venueService.findVenueById(venueId);

        Pageable pageable = SearchPage.of(page, size, "id", "asc", Set.of("id"));

        return courtRepository
                .findByVenue_Id(venueId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CourtResponse> getMyCourtsByVenue(String username, Long venueId, int page, int size) {
        checkVenueOwnership(venueService.findVenueById(venueId), username);
        return getCourtsByVenue(venueId, page, size);
    }

    @Transactional(readOnly = true)
    public void requireCourtInVenue(Long courtId, Long venueId) {
        if (!findCourtById(courtId).getVenue().getId().equals(venueId)) {
            throw new ResourceNotFoundException("Sân không thuộc cơ sở trong đường dẫn");
        }
    }

    @Transactional(readOnly = true)
    public CourtResponse getCourtById(Long id) {
        Court court = findCourtById(id);
        return toResponse(court);
    }

    @Transactional
    public CourtResponse createCourt(
            String username,
            Long venueId,
            CourtRequest request) {

        Venue venue = venueService.findVenueById(venueId);
        checkVenueOwnership(venue, username);
        validateHours(request);

        String name = request.getName().trim();

        if (courtRepository.existsByNameIgnoreCaseAndVenue_Id(
                name, venueId)) {
            throw new ConflictException(
                    "Sân trùng tên trong cơ sở này: " + name);
        }

        Sport sport = findSportById(request.getSportId());

        Court court = new Court();
        court.setName(name);
        court.setDescription(request.getDescription());
        court.setVenue(venue);
        court.setSport(sport);
        court.setPricePerHour(request.getPricePerHour());
        court.setOpenTime(request.getOpenTime());
        court.setCloseTime(request.getCloseTime());
        court.setStatus(CourtStatus.ACTIVE);

        courtRepository.save(court);

        return toResponse(court);
    }

    @Transactional
    public CourtResponse updateCourt(
            String username,
            Long courtId,
            CourtRequest request) {

        Court court = findCourtById(courtId);
        checkVenueOwnership(court.getVenue(), username);

        validateHours(request);

        String newName = request.getName().trim();

        if (!court.getName().equalsIgnoreCase(newName)
                && courtRepository.existsByNameIgnoreCaseAndVenue_Id(
                    newName, court.getVenue().getId())) {
            throw new ConflictException(
                    "Sân trùng tên trong cơ sở này: " + newName);
        }

        Sport sport = findSportById(request.getSportId());

        court.setName(newName);
        court.setDescription(request.getDescription());
        court.setSport(sport);
        court.setPricePerHour(request.getPricePerHour());
        court.setOpenTime(request.getOpenTime());
        court.setCloseTime(request.getCloseTime());

        courtRepository.saveAndFlush(court);

        return toResponse(court);
    }

    @Transactional
    public void deleteCourt(String username, Long courtId) {
        Court court = findCourtById(courtId);
        checkVenueOwnership(court.getVenue(), username);
        courtRepository.delete(court);
    }

    public Court findCourtById(Long id) {
        return courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sân có id = " + id));
    }

    private Sport findSportById(Long id) {
        return sportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy môn thể thao có id = " + id));
    }

    private void validateHours(CourtRequest request) {
        if ((request.getOpenTime() == null) != (request.getCloseTime() == null)
                || (request.getOpenTime() != null && !request.getOpenTime().isBefore(request.getCloseTime()))) {
            throw new IllegalArgumentException("Cần cung cấp cả giờ mở và đóng cửa, giờ mở phải trước giờ đóng");
        }
    }

    private void checkVenueOwnership(Venue venue, String username) {
        if (!venue.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "Bạn không có quyền thao tác trên cơ sở này");
        }
    }

    private CourtResponse toResponse(Court court) {
        return new CourtResponse(
                court.getId(),
                court.getName(),
                court.getDescription(),
                court.getVenue().getId(),
                court.getVenue().getName(),
                court.getSport().getId(),
                court.getSport().getName(),
                court.getPricePerHour(),
                court.getOpenTime(),
                court.getCloseTime(),
                court.getStatus(),
                court.getCreatedAt(),
                court.getUpdatedAt());
    }
}
