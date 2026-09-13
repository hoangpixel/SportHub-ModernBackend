package com.sporthub.repository;

import com.sporthub.entity.Sport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SportRepository
        extends JpaRepository<Sport, Long> {

    Optional<Sport> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
    Page<Sport> findByNameContainingIgnoreCase(
        String name,
        Pageable pageable
    );
}