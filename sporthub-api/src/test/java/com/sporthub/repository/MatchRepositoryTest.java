package com.sporthub.repository;

import com.sporthub.entity.Match;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MatchRepositoryTest {

    @Autowired
    MatchRepository matchRepository;

    private Match createMatch(
            Long courtId,
            LocalDate date,
            LocalTime start,
            LocalTime end
    ) {
        return Match.builder()
                .tournamentId(1L)
                .roundNumber(1)
                .courtId(courtId)
                .team1Id(1L)
                .team2Id(2L)
                .scheduledDate(date)
                .startTime(start)
                .endTime(end)
                .build();
    }

    @Test
    void existsConflictWhenTimeOverlaps() {
        matchRepository.saveAndFlush(
                createMatch(
                        1L,
                        LocalDate.of(2026, 11, 1),
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0)
                )
        );

        assertThat(matchRepository.existsConflict(
                1L,
                LocalDate.of(2026, 11, 1),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30)
        )).isTrue();
    }

    @Test
    void existsConflictWhenTimeDoesNotOverlap() {
        matchRepository.saveAndFlush(
                createMatch(
                        1L,
                        LocalDate.of(2026, 11, 1),
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0)
                )
        );

        // Bắt đầu đúng lúc trận trước kết thúc
        assertThat(matchRepository.existsConflict(
                1L,
                LocalDate.of(2026, 11, 1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        )).isFalse();
    }

    @Test
    void existsConflictReturnsFalseForDifferentCourt() {
        matchRepository.saveAndFlush(
                createMatch(
                        1L,
                        LocalDate.of(2026, 11, 1),
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0)
                )
        );

        assertThat(matchRepository.existsConflict(
                2L,
                LocalDate.of(2026, 11, 1),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30)
        )).isFalse();
    }

    @Test
    void existsConflictForUpdateIgnoresCurrentMatch() {
        Match saved = matchRepository.saveAndFlush(
                createMatch(
                        1L,
                        LocalDate.of(2026, 11, 1),
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0)
                )
        );

        assertThat(matchRepository.existsConflictForUpdate(
                saved.getId(),
                1L,
                LocalDate.of(2026, 11, 1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        )).isFalse();
    }

    @Test
    void existsConflictForUpdateDetectsAnotherMatch() {
        Match first = matchRepository.saveAndFlush(
                createMatch(
                        1L,
                        LocalDate.of(2026, 11, 1),
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0)
                )
        );

        matchRepository.saveAndFlush(
                createMatch(
                        1L,
                        LocalDate.of(2026, 11, 1),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0)
                )
        );

        assertThat(matchRepository.existsConflictForUpdate(
                first.getId(),
                1L,
                LocalDate.of(2026, 11, 1),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30)
        )).isTrue();
    }
}