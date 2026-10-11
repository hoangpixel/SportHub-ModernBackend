package com.sporthub.repository;

import com.sporthub.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;

@Repository
public interface MatchRepository
        extends JpaRepository<Match, Long> {

    @Query("""
                SELECT COUNT(m) > 0
                FROM Match m
                WHERE m.courtId = :courtId
                  AND m.scheduledDate = :scheduledDate
                  AND m.startTime < :endTime
                  AND m.endTime > :startTime
            """)
    boolean existsConflict(
            @Param("courtId") Long courtId,
            @Param("scheduledDate") LocalDate scheduledDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("""
                SELECT COUNT(m) > 0
                FROM Match m
                WHERE m.id <> :matchId
                  AND m.courtId = :courtId
                  AND m.scheduledDate = :scheduledDate
                  AND m.startTime < :endTime
                  AND m.endTime > :startTime
            """)
    boolean existsConflictForUpdate(
            @Param("matchId") Long matchId,
            @Param("courtId") Long courtId,
            @Param("scheduledDate") LocalDate scheduledDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
}
