package com.sporthub.controller;


import com.sporthub.dto.request.AddTeamMemberRequest;
import com.sporthub.dto.request.CreateTeamRequest;
import com.sporthub.dto.request.UpdateTeamRequest;
import com.sporthub.security.JwtAuthenticationFilter;
import com.sporthub.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeamControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TeamService teamService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createTeam_shouldReturn201() throws Exception {
        String json = """
                {
                    "sportId": 1,
                    "name": "Team A",
                    "description": "Test team",
                    "logoUrl": "https://example.com/logo.png"
                }
                """;

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(teamService).createTeam(any(CreateTeamRequest.class));
    }

    @Test
    void createTeam_shouldReturn400WhenNameIsBlank() throws Exception {
        String json = """
                {
                    "sportId": 1,
                    "name": "",
                    "description": "Test team"
                }
                """;

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(teamService);
    }

    @Test
    void updateTeam_shouldReturn200() throws Exception {
        String json = """
                {
                    "name": "Team B",
                    "description": "Updated team",
                    "logoUrl": "https://example.com/logo.png"
                }
                """;

        mockMvc.perform(put("/api/teams/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(teamService).updateTeam(
                eq(10L), any(UpdateTeamRequest.class));
    }

    @Test
    void getTeamDetail_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/teams/10"))
                .andExpect(status().isOk());

        verify(teamService).getTeamDetail(10L);
    }

    @Test
    void addMember_shouldReturn200() throws Exception {
        String json = """
                {
                    "keyword": "member"
                }
                """;

        mockMvc.perform(post("/api/teams/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(teamService).addMember(
                eq(10L), any(AddTeamMemberRequest.class));
    }

    @Test
    void addMember_shouldReturn400WhenKeywordIsBlank() throws Exception {
        String json = """
                {
                    "keyword": ""
                }
                """;

        mockMvc.perform(post("/api/teams/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(teamService, never())
                .addMember(eq(10L), any(AddTeamMemberRequest.class));
    }

    @Test
    void removeMember_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/teams/10/members/2"))
                .andExpect(status().isNoContent());

        verify(teamService).removeMember(10L, 2L);
    }

    @Test
    void leaveTeam_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/teams/10/members/me"))
                .andExpect(status().isNoContent());

        verify(teamService).leaveTeam(10L);
    }
}

