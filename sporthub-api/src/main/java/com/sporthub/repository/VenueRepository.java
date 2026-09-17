package com.sporthub.repository;

import com.sporthub.entity.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    java.util.Optional<Venue> findByNameIgnoreCaseAndOwner_Id(String name, Long ownerId);

    @EntityGraph(attributePaths = "owner")
    Page<Venue> findByOwner_Id(Long ownerId, Pageable pageable);

    boolean existsByNameIgnoreCaseAndOwner_Id(String name, Long ownerId);

    @EntityGraph(attributePaths = "owner")
    @Query("""
        SELECT v FROM Venue v
        WHERE (:keyword IS NULL OR :keyword = ''
               OR LOWER(v.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.city) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:city IS NULL OR :city = '' OR LOWER(v.city) = LOWER(:city))
          AND (:district IS NULL OR :district = '' OR LOWER(v.district) = LOWER(:district))
          AND (:ownerId IS NULL OR v.owner.id = :ownerId)
    """)
    Page<Venue> searchVenues(
        @Param("keyword") String keyword,
        @Param("city") String city,
        @Param("district") String district,
        @Param("ownerId") Long ownerId,
        Pageable pageable
    );
}
