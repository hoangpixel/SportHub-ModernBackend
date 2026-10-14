package com.sporthub.repository;


import com.sporthub.entity.TournamentTeam;
import com.sporthub.enums.RegistrationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TournamentTeamRepositoryTest {

    @Autowired
    TournamentTeamRepository tournamentTeamRepository;

    @Test
    void existsByTournamentIdAndTeamId() {
        tournamentTeamRepository.saveAndFlush(
                TournamentTeam.builder()
                        .tournamentId(1L)
                        .teamId(2L)
                        .registrationStatus(RegistrationStatus.PENDING)
                        .build()
        );

        assertThat(
                tournamentTeamRepository.existsByTournamentIdAndTeamId(1L, 2L)
        ).isTrue();

        assertThat(
                tournamentTeamRepository.existsByTournamentIdAndTeamId(1L, 3L)
        ).isFalse();
    }

    @Test
    void findByTournamentIdAndTeamId() {
        tournamentTeamRepository.saveAndFlush(
                TournamentTeam.builder()
                        .tournamentId(1L)
                        .teamId(2L)
                        .build()
        );

        assertThat(
                tournamentTeamRepository.findByTournamentIdAndTeamId(1L, 2L)
        ).isPresent();

        assertThat(
                tournamentTeamRepository.findByTournamentIdAndTeamId(1L, 3L)
        ).isEmpty();
    }

    @Test
    void findAllByTournamentId() {
        tournamentTeamRepository.saveAndFlush(
                TournamentTeam.builder()
                        .tournamentId(1L)
                        .teamId(10L)
                        .build()
        );

        tournamentTeamRepository.saveAndFlush(
                TournamentTeam.builder()
                        .tournamentId(1L)
                        .teamId(11L)
                        .build()
        );

        tournamentTeamRepository.saveAndFlush(
                TournamentTeam.builder()
                        .tournamentId(2L)
                        .teamId(12L)
                        .build()
        );

        assertThat(tournamentTeamRepository.findAllByTournamentId(1L))
                .hasSize(2);
    }

    @Test
    void countByTournamentId() {
        tournamentTeamRepository.saveAndFlush(
                TournamentTeam.builder()
                        .tournamentId(1L)
                        .teamId(10L)
                        .build()
        );

        tournamentTeamRepository.saveAndFlush(
                TournamentTeam.builder()
                        .tournamentId(1L)
                        .teamId(11L)
                        .build()
        );

        assertThat(tournamentTeamRepository.countByTournamentId(1L))
                .isEqualTo(2);

        assertThat(tournamentTeamRepository.countByTournamentId(2L))
                .isZero();
    }
}
