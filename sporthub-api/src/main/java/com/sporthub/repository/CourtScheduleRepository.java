package com.sporthub.repository;

import com.sporthub.entity.CourtSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface CourtScheduleRepository extends JpaRepository<CourtSchedule, Long> {

    List<CourtSchedule> findByCourt_IdOrderByDayOfWeek(Long courtId);

    Optional<CourtSchedule> findByCourt_IdAndDayOfWeek(Long courtId, DayOfWeek dayOfWeek);

    boolean existsByCourt_IdAndDayOfWeek(Long courtId, DayOfWeek dayOfWeek);

    void deleteAllByCourt_Id(Long courtId);
}
