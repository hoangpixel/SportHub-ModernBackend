package com.sporthub.repository;

import com.sporthub.entity.TournamentTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentTeamRepository
        extends JpaRepository<TournamentTeam, Long> {
    boolean existsByTournamentIdAndTeamId(
            Long tournamentId,
            Long teamId);

    Optional<TournamentTeam> findByTournamentIdAndTeamId(
            Long tournamentId,
            Long teamId);

    List<TournamentTeam> findAllByTournamentId(
            Long tournamentId);

    long countByTournamentId(Long tournamentId);
}
