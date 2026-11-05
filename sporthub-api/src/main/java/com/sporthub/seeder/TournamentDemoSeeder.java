package com.sporthub.seeder;

import com.sporthub.entity.Court;
import com.sporthub.entity.Match;
import com.sporthub.entity.Sport;
import com.sporthub.entity.Team;
import com.sporthub.entity.TeamMember;
import com.sporthub.entity.Tournament;
import com.sporthub.entity.TournamentTeam;
import com.sporthub.entity.User;
import com.sporthub.entity.Venue;
import com.sporthub.enums.MatchStatus;
import com.sporthub.enums.RegistrationStatus;
import com.sporthub.enums.TeamMemberStatus;
import com.sporthub.enums.TeamRole;
import com.sporthub.enums.TeamStatus;
import com.sporthub.enums.TournamentStatus;
import com.sporthub.repository.BookingRepository;
import com.sporthub.repository.CourtRepository;
import com.sporthub.repository.MatchRepository;
import com.sporthub.repository.SportRepository;
import com.sporthub.repository.TeamMemberRepository;
import com.sporthub.repository.TeamRepository;
import com.sporthub.repository.TournamentRepository;
import com.sporthub.repository.TournamentTeamRepository;
import com.sporthub.repository.UserRepository;
import com.sporthub.repository.VenueRepository;
import com.sporthub.service.BookingTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TournamentDemoSeeder {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserRepository userRepository;
    private final SportRepository sportRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final MatchRepository matchRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public void seed() {
        User user01 = requireUser("user01");
        User user02 = requireUser("user02");
        User user03 = requireUser("user03");
        User user04 = requireUser("user04");
        User user05 = requireUser("user05");
        User user06 = requireUser("user06");
        User user07 = requireUser("user07");
        User owner01 = requireUser("owner01");
        User owner02 = requireUser("owner02");
        User admin = requireUser("admin01");

        Sport football = requireSport("Football");
        Sport badminton = requireSport("Badminton");
        Sport pickleball = requireSport("Pickleball");
        Venue arena = requireVenue("SportHub Arena", owner01);
        Venue club = requireVenue("Saigon Badminton Club", owner02);
        Venue hub = requireVenue("Thao Dien Pickleball Hub", owner02);
        Court footballA = requireCourt(arena, "Sân bóng đá 5 người - A");
        Court footballB = requireCourt(arena, "Sân bóng đá 5 người - B");
        Court badminton2 = requireCourt(club, "Sân cầu lông 2");
        Court badminton3 = requireCourt(club, "Sân cầu lông 3");
        Court pickleballB = requireCourt(hub, "Sân pickleball B");

        // 10 doi, moi doi co captain. Giu nguyen 2 doi tu ban demo dau tien.
        Team f1 = team("Demo SportHub FC", user01, football);
        Team f2 = team("Demo Saigon United", owner01, football);
        Team f3 = team("Demo District 7 Warriors", user02, football);
        Team f4 = team("Demo Thu Duc Stars", user03, football);
        Team b1 = team("Demo Shuttle Masters", user04, badminton);
        Team b2 = team("Demo Smash Squad", user05, badminton);
        Team b3 = team("Demo Net Ninjas", user06, badminton);
        Team b4 = team("Demo Shuttle Rockets", user07, badminton);
        Team p1 = team("Demo Paddle Aces", owner02, pickleball);
        Team p2 = team("Demo Pickleball Storm", user01, pickleball);

        // 3 giai dau, so doi tham gia duoc duyet va cap so hat giong.
        Tournament footballCup = tournament("Demo SportHub Football Cup",
                "Giải bóng đá minh họa, không phải sự kiện thực tế", football, arena, admin, 14, 8);
        Tournament badmintonCup = tournament("Demo Saigon Badminton Open",
                "Giải cầu lông nội bộ dùng cho Swagger demo", badminton, club, admin, 21, 8);
        Tournament pickleballCup = tournament("Demo Thao Dien Pickleball Cup",
                "Giải pickleball giao hữu dùng cho Swagger demo", pickleball, hub, admin, 28, 8);

        register(footballCup, f1, f2, f3, f4);
        register(badmintonCup, b1, b2, b3, b4);
        register(pickleballCup, p1, p2);

        // 10 tran, cac khung gio khong trung nhau tren cung mot court.
        LocalDate fDate = footballCup.getStartDate();
        match(footballCup, f1, f2, footballA, fDate, 18, 19, 1); // Tran demo goc
        match(footballCup, f3, f4, footballB, fDate, 18, 19, 1);
        match(footballCup, f1, f3, footballA, fDate, 19, 20, 2);
        match(footballCup, f2, f4, footballB, fDate, 19, 20, 2);
        match(footballCup, f1, f4, footballA, fDate.plusDays(1), 18, 19, 3);

        LocalDate bDate = badmintonCup.getStartDate();
        match(badmintonCup, b1, b2, badminton2, bDate, 18, 19, 1);
        match(badmintonCup, b3, b4, badminton3, bDate, 19, 20, 1);
        match(badmintonCup, b1, b3, badminton2, bDate.plusDays(1), 18, 19, 2);
        match(badmintonCup, b2, b4, badminton3, bDate.plusDays(1), 19, 20, 2);

        match(pickleballCup, p1, p2, pickleballB, pickleballCup.getStartDate(), 18, 19, 1);
    }

    private User requireUser(String name) {
        return userRepository.findByUsername(name)
                .orElseThrow(() -> new IllegalStateException("Thieu user demo: " + name));
    }

    private Sport requireSport(String name) {
        return sportRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new IllegalStateException("Thieu sport demo: " + name));
    }

    private Venue requireVenue(String name, User owner) {
        return venueRepository.findByNameIgnoreCaseAndOwner_Id(name, owner.getId())
                .orElseThrow(() -> new IllegalStateException("Thieu venue demo: " + name));
    }

    private Court requireCourt(Venue venue, String name) {
        return courtRepository.findByNameIgnoreCaseAndVenue_Id(name, venue.getId())
                .orElseThrow(() -> new IllegalStateException("Thieu court demo: " + name));
    }

    private Team team(String name, User captain, Sport sport) {
        Team saved = teamRepository.findAll().stream()
                .filter(t -> name.equals(t.getName())
                        && Objects.equals(t.getOwnerUserId(), captain.getId())
                        && Objects.equals(t.getSportId(), sport.getId()))
                .findFirst()
                .orElseGet(() -> {
                    LocalDateTime now = BookingTime.now();
                    Team created = Team.builder()
                            .ownerUserId(captain.getId())
                            .sportId(sport.getId())
                            .name(name)
                            .description("Đội demo - không phải đội bóng thực tế")
                            .status(TeamStatus.ACTIVE)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return teamRepository.saveAndFlush(created);
                });
        ensureCaptain(saved, captain);
        return saved;
    }

    private void ensureCaptain(Team team, User captain) {
        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), captain.getId())) {
            return;
        }
        TeamMember member = TeamMember.builder()
                .teamId(team.getId())
                .userId(captain.getId())
                .teamRole(TeamRole.CAPTAIN)
                .status(TeamMemberStatus.ACTIVE)
                .joinedAt(BookingTime.now())
                .build();
        teamMemberRepository.save(member);
    }

    private Tournament tournament(String name, String description, Sport sport,
                                  Venue venue, User creator, int startAfterDays, int maxTeams) {
        return tournamentRepository.findAll().stream()
                .filter(t -> name.equals(t.getName()) && Objects.equals(t.getSportId(), sport.getId()))
                .findFirst()
                .orElseGet(() -> {
                    LocalDate today = LocalDate.now(ZONE);
                    LocalDateTime now = BookingTime.now();
                    Tournament created = Tournament.builder()
                            .sportId(sport.getId())
                            .venueId(venue.getId())
                            .createdBy(creator.getId())
                            .name(name)
                            .description(description)
                            .registrationStart(today.minusDays(1))
                            .registrationEnd(today.plusDays(startAfterDays - 7))
                            .startDate(today.plusDays(startAfterDays))
                            .endDate(today.plusDays(startAfterDays + 1))
                            .maxTeams(maxTeams)
                            .status(TournamentStatus.UPCOMING)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return tournamentRepository.saveAndFlush(created);
                });
    }

    private void register(Tournament tournament, Team... teams) {
        for (int i = 0; i < teams.length; i++) {
            Team team = teams[i];
            if (tournamentTeamRepository.existsByTournamentIdAndTeamId(tournament.getId(), team.getId())) {
                continue;
            }
            TournamentTeam registration = TournamentTeam.builder()
                    .tournamentId(tournament.getId())
                    .teamId(team.getId())
                    .seedNumber(i + 1)
                    .registrationStatus(RegistrationStatus.APPROVED)
                    .registeredAt(BookingTime.now())
                    .build();
            tournamentTeamRepository.save(registration);
        }
    }

    private void match(Tournament tournament, Team a, Team b, Court court, LocalDate date,
                       int startHour, int endHour, int round) {
        // Mot cap dau + vong trong mot giai chi duoc tao mot lan.
        boolean alreadyExists = matchRepository.findAll().stream()
                .anyMatch(m -> Objects.equals(m.getTournamentId(), tournament.getId())
                        && Objects.equals(m.getTeam1Id(), a.getId())
                        && Objects.equals(m.getTeam2Id(), b.getId())
                        && Objects.equals(m.getRoundNumber(), round));
        if (alreadyExists) {
            return;
        }

        LocalTime start = LocalTime.of(startHour, 0);
        LocalTime end = LocalTime.of(endHour, 0);
        if (matchRepository.existsConflict(court.getId(), date, start, end)
                || bookingRepository.existsActiveOverlap(court.getId(), date, start, end, BookingTime.now())) {
            return;
        }

        LocalDateTime now = BookingTime.now();
        Match created = Match.builder()
                .tournamentId(tournament.getId())
                .roundNumber(round)
                .courtId(court.getId())
                .team1Id(a.getId())
                .team2Id(b.getId())
                .scheduledDate(date)
                .startTime(start)
                .endTime(end)
                .team1Score(0)
                .team2Score(0)
                .status(MatchStatus.SCHEDULED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        matchRepository.save(created);
    }
}
