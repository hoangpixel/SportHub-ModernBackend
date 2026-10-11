package com.sporthub.service;

import com.sporthub.dto.request.VenueRequest;
import com.sporthub.dto.response.VenueResponse;
import com.sporthub.entity.User;
import com.sporthub.entity.Venue;
import com.sporthub.enums.VenueStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.UserRepository;
import com.sporthub.repository.VenueRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<VenueResponse> searchVenues(
            String keyword,
            String city,
            String district,
            Long ownerId,
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = SearchPage.of(page, size, sortBy, direction,
                Set.of("id", "name", "city", "district", "createdAt", "updatedAt"));

        Page<Venue> venues = venueRepository.searchVenues(
                keyword, city, district, ownerId, pageable);

        return venues.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public VenueResponse getVenueById(Long id) {
        Venue venue = findVenueById(id);
        return toResponse(venue);
    }

    @Transactional(readOnly = true)
    public Page<VenueResponse> getMyVenues(
            String username,
            int page,
            int size) {

        User owner = findUserByUsername(username);

        Pageable pageable = SearchPage.of(page, size, "id", "asc", Set.of("id"));

        Page<Venue> venues = venueRepository
                .findByOwner_Id(owner.getId(), pageable);

        return venues.map(this::toResponse);
    }

    @Transactional
    public VenueResponse createVenue(
            String username,
            VenueRequest request) {

        User owner = findUserByUsername(username);

        String name = request.getName().trim();

        if (venueRepository.existsByNameIgnoreCaseAndOwner_Id(
                name, owner.getId())) {
            throw new ConflictException(
                    "Bạn đã có cơ sở trùng tên: " + name);
        }

        Venue venue = new Venue();
        venue.setName(name);
        venue.setDescription(request.getDescription());
        venue.setAddress(request.getAddress());
        venue.setCity(request.getCity());
        venue.setDistrict(request.getDistrict());
        venue.setPhone(request.getPhone());
        venue.setImageUrl(request.getImageUrl());
        venue.setStatus(VenueStatus.ACTIVE);
        venue.setOwner(owner);

        venueRepository.save(venue);

        return toResponse(venue);
    }

    @Transactional
    public VenueResponse updateVenue(
            String username,
            Long venueId,
            VenueRequest request) {

        Venue venue = findVenueById(venueId);
        checkOwnership(venue, username);

        String newName = request.getName().trim();

        if (!venue.getName().equalsIgnoreCase(newName)
                && venueRepository.existsByNameIgnoreCaseAndOwner_Id(
                    newName, venue.getOwner().getId())) {
            throw new ConflictException(
                    "Bạn đã có cơ sở trùng tên: " + newName);
        }

        venue.setName(newName);
        venue.setDescription(request.getDescription());
        venue.setAddress(request.getAddress());
        venue.setCity(request.getCity());
        venue.setDistrict(request.getDistrict());
        venue.setPhone(request.getPhone());
        venue.setImageUrl(request.getImageUrl());

        venueRepository.saveAndFlush(venue);

        return toResponse(venue);
    }

    @Transactional
    public void deleteVenue(String username, Long venueId) {
        Venue venue = findVenueById(venueId);
        checkOwnership(venue, username);
        venueRepository.delete(venue);
    }

    public Venue findVenueById(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy cơ sở có id = " + id));
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy user: " + username));
    }

    private void checkOwnership(Venue venue, String username) {
        if (!venue.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "Bạn không có quyền thao tác trên cơ sở này");
        }
    }

    private VenueResponse toResponse(Venue venue) {
        return new VenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getDescription(),
                venue.getAddress(),
                venue.getCity(),
                venue.getDistrict(),
                venue.getPhone(),
                venue.getImageUrl(),
                venue.getStatus(),
                venue.getOwner().getId(),
                venue.getOwner().getUsername(),
                venue.getCreatedAt(),
                venue.getUpdatedAt());
    }
}
