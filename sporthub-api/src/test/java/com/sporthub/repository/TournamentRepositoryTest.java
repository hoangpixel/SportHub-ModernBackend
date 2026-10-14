package com.sporthub.repository;


import com.sporthub.entity.Tournament;
import com.sporthub.enums.TournamentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TournamentRepositoryTest {

    @Autowired
    TournamentRepository tournamentRepository;

    @Test
    void saveAndFindTournament() {
        Tournament tournament = Tournament.builder()
                .sportId(1L)
                .createdBy(1L)
                .name("Badminton Cup")
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 3))
                .registrationStart(LocalDate.of(2026, 10, 1))
                .registrationEnd(LocalDate.of(2026, 10, 25))
                .maxTeams(8)
                .status(TournamentStatus.UPCOMING)
                .build();

        Tournament saved = tournamentRepository.saveAndFlush(tournament);

        assertThat(saved.getId()).isNotNull();
        assertThat(tournamentRepository.findById(saved.getId()))
                .isPresent();
        assertThat(tournamentRepository.findById(saved.getId()).get().getName())
                .isEqualTo("Badminton Cup");
    }

    @Test
    void deleteTournament() {
        Tournament tournament = tournamentRepository.saveAndFlush(
                Tournament.builder()
                        .name("Test Tournament")
                        .build()
        );

        tournamentRepository.deleteById(tournament.getId());
        tournamentRepository.flush();

        assertThat(tournamentRepository.findById(tournament.getId()))
                .isEmpty();
    }
}
