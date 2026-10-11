package com.sporthub.repository;

import com.sporthub.entity.Team;
import com.sporthub.enums.TeamStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TeamRepositoryTest {

    @Autowired
    TeamRepository teamRepository;

    @Test
    void saveAndFindTeam() {
        Team team = Team.builder()
                .ownerUserId(1L)
                .sportId(1L)
                .name("Badminton Team")
                .status(TeamStatus.ACTIVE)
                .build();

        Team saved = teamRepository.saveAndFlush(team);

        assertThat(saved.getId()).isNotNull();
        assertThat(teamRepository.findById(saved.getId())).isPresent();
        assertThat(teamRepository.findById(saved.getId()).get().getName())
                .isEqualTo("Badminton Team");
    }

    @Test
    void deleteTeam() {
        Team team = teamRepository.saveAndFlush(
                Team.builder()
                        .name("Test Team")
                        .build()
        );

        teamRepository.deleteById(team.getId());
        teamRepository.flush();

        assertThat(teamRepository.findById(team.getId())).isEmpty();
    }
}
