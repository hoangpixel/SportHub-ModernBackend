package com.sporthub.repository;

import com.sporthub.entity.Court;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CourtRepository extends JpaRepository<Court, Long> {

    java.util.Optional<Court> findByNameIgnoreCaseAndVenue_Id(String name, Long venueId);

    List<Court> findByVenue_Id(Long venueId);

    @EntityGraph(attributePaths = {"venue", "sport"})
    Page<Court> findByVenue_Id(Long venueId, Pageable pageable);

    boolean existsByNameIgnoreCaseAndVenue_Id(String name, Long venueId);

    @EntityGraph(attributePaths = {"venue", "sport"})
    Page<Court> findByVenue_IdAndSport_Id(Long venueId, Long sportId, Pageable pageable);

    @Query(value = """
        SELECT c FROM Court c
          JOIN FETCH c.venue v
          JOIN FETCH c.sport s
        WHERE (:keyword IS NULL OR :keyword = ''
               OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:sportId IS NULL OR s.id = :sportId)
          AND (:venueId IS NULL OR v.id = :venueId)
          AND (:minPrice IS NULL OR c.pricePerHour >= :minPrice)
          AND (:maxPrice IS NULL OR c.pricePerHour <= :maxPrice)
    """, countQuery = """
        SELECT COUNT(c) FROM Court c
          JOIN c.venue v
        WHERE (:keyword IS NULL OR :keyword = ''
               OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:sportId IS NULL OR c.sport.id = :sportId)
          AND (:venueId IS NULL OR v.id = :venueId)
          AND (:minPrice IS NULL OR c.pricePerHour >= :minPrice)
          AND (:maxPrice IS NULL OR c.pricePerHour <= :maxPrice)
    """)
    Page<Court> searchCourts(
        @Param("keyword") String keyword,
        @Param("sportId") Long sportId,
        @Param("venueId") Long venueId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );
}
