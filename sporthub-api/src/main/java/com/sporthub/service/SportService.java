package com.sporthub.service;

import com.sporthub.dto.request.SportRequest;
import com.sporthub.dto.response.SportResponse;
import com.sporthub.entity.Sport;
import com.sporthub.enums.SportStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.SportRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SportService {

        private final SportRepository sportRepository;

        // =========================
        // GET ALL
        // =========================
        @Transactional(readOnly = true)
        public Page<SportResponse> getAllSports(
                        String keyword,
                        int page,
                        int size) {

                Pageable pageable = PageRequest.of(
                                page,
                                size,
                                Sort.by("id").ascending());

                Page<Sport> sports;

                if (keyword == null || keyword.isBlank()) {

                        sports = sportRepository.findAll(pageable);

                } else {

                        sports = sportRepository
                                        .findByNameContainingIgnoreCase(
                                                        keyword,
                                                        pageable);
                }

                return sports.map(this::toResponse);
        }

        // =========================
        // GET BY ID
        // =========================
        @Transactional(readOnly = true)
        public SportResponse getSportById(Long id) {

                Sport sport = findSportById(id);

                return toResponse(sport);
        }

        // =========================
        // CREATE
        // =========================
        @Transactional
        public SportResponse createSport(
                        SportRequest request) {

                String name = request
                                .getName()
                                .trim();

                // Kiểm tra trùng tên
                if (sportRepository
                                .existsByNameIgnoreCase(name)) {

                        throw new ConflictException(
                                        "Môn thể thao đã tồn tại");
                }

                Sport sport = new Sport();

                sport.setName(name);
                sport.setDescription(
                                request.getDescription());
                sport.setIconUrl(
                                request.getIconUrl());

                // Sport mới mặc định ACTIVE
                sport.setStatus(
                                SportStatus.ACTIVE);

                /*
                 * createdAt không cần set ở đây.
                 * 
                 * @PrePersist trong Sport Entity
                 * sẽ tự tạo.
                 */

                sportRepository.save(sport);

                return toResponse(sport);
        }

        // =========================
        // UPDATE
        // =========================
        @Transactional
        public SportResponse updateSport(
                        Long id,
                        SportRequest request) {

                Sport sport = findSportById(id);

                String newName = request
                                .getName()
                                .trim();

                Sport existingSport = sportRepository
                                .findByNameIgnoreCase(newName)
                                .orElse(null);

                if (existingSport != null
                                && !existingSport
                                                .getId()
                                                .equals(id)) {

                        throw new ConflictException(
                                        "Tên môn thể thao đã tồn tại");
                }

                sport.setName(newName);

                sport.setDescription(
                                request.getDescription());

                sport.setIconUrl(
                                request.getIconUrl());

                Sport updatedSport = sportRepository.saveAndFlush(sport);

                return toResponse(updatedSport);
        }

        // =========================
        // DELETE
        // =========================
        @Transactional
        public void deleteSport(Long id) {

                Sport sport = findSportById(id);

                sportRepository.delete(sport);
        }

        // =========================
        // FIND ENTITY
        // =========================
        private Sport findSportById(
                        Long id) {

                return sportRepository
                                .findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Không tìm thấy môn thể thao có id = "
                                                                + id));
        }

        // =========================
        // ENTITY -> RESPONSE DTO
        // =========================
        private SportResponse toResponse(
                        Sport sport) {

                return new SportResponse(
                                sport.getId(),
                                sport.getName(),
                                sport.getDescription(),
                                sport.getIconUrl(),
                                sport.getStatus(),
                                sport.getCreatedAt(),
                                sport.getUpdatedAt());
        }
}