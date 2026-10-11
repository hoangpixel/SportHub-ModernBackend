package com.sporthub.service;

import com.sporthub.dto.request.AddTeamMemberRequest;
import com.sporthub.dto.request.CreateTeamRequest;
import com.sporthub.dto.response.TeamResponse;
import com.sporthub.entity.Team;
import com.sporthub.entity.TeamMember;
import com.sporthub.entity.User;
import com.sporthub.enums.TeamStatus;
import com.sporthub.enums.UserStatus;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.repository.SportRepository;
import com.sporthub.repository.TeamMemberRepository;
import com.sporthub.repository.TeamRepository;
import com.sporthub.repository.UserRepository;
import com.sporthub.security.CurrentUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    TeamRepository teamRepository;

    @Mock
    TeamMemberRepository teamMemberRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    SportRepository sportRepository;

    @Mock
    CurrentUserService currentUserService;

    @InjectMocks
    TeamService teamService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test",
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                )
        );
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@gmail.com");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    @Test
    void createTeam_shouldSuccess() {
        User user = createUser(1L, "user1");

        CreateTeamRequest request = mock(CreateTeamRequest.class);

        when(request.getSportId()).thenReturn(1L);
        when(request.getName()).thenReturn("Team A");
        when(request.getDescription()).thenReturn("Description");
        when(request.getLogoUrl()).thenReturn("logo.png");

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(sportRepository.existsById(1L))
                .thenReturn(true);

        Team savedTeam = Team.builder()
                .id(10L)
                .ownerUserId(1L)
                .sportId(1L)
                .name("Team A")
                .description("Description")
                .status(TeamStatus.ACTIVE)
                .build();

        when(teamRepository.save(any(Team.class)))
                .thenReturn(savedTeam);

        TeamResponse response =
                teamService.createTeam(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Team A", response.getName());

        verify(teamRepository).save(any(Team.class));
        verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    void createTeam_shouldRejectInvalidSport() {
        User user = createUser(1L, "user1");

        CreateTeamRequest request = mock(CreateTeamRequest.class);

        when(request.getSportId()).thenReturn(99L);

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        when(sportRepository.existsById(99L))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> teamService.createTeam(request)
        );

        verify(teamRepository, never())
                .save(any(Team.class));

        verify(teamMemberRepository, never())
                .save(any(TeamMember.class));
    }

    @Test
    void addMember_shouldRejectDuplicateMember() {
        setAuthentication("CREATE_TEAM");

        Team team = Team.builder()
                .id(10L)
                .ownerUserId(1L)
                .status(TeamStatus.ACTIVE)
                .build();

        User owner = createUser(1L, "owner");
        User member = createUser(2L, "member");

        AddTeamMemberRequest request =
                mock(AddTeamMemberRequest.class);

        when(request.getKeyword())
                .thenReturn("member");

        when(teamRepository.findById(10L))
                .thenReturn(Optional.of(team));

        when(currentUserService.getCurrentUser())
                .thenReturn(owner);

        when(userRepository.findByUsernameOrEmail(
                "member",
                "member"))
                .thenReturn(Optional.of(member));

        when(teamMemberRepository.existsByTeamIdAndUserId(
                10L,
                2L))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> teamService.addMember(10L, request)
        );

        verify(teamMemberRepository, never())
                .save(any(TeamMember.class));
    }
}
