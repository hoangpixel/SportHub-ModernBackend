package com.sporthub.controller;


import com.sporthub.dto.request.RegisterTournamentTeamRequest;
import com.sporthub.security.JwtAuthenticationFilter;
import com.sporthub.service.TournamentTeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TournamentTeamController.class)
@AutoConfigureMockMvc(addFilters = false)
class TournamentTeamControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TournamentTeamService tournamentTeamService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void registerTeam_shouldReturn201() throws Exception {
        String json = """
                {
                    "teamId": 20
                }
                """;

        mockMvc.perform(post("/api/tournaments/10/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(tournamentTeamService).registerTeam(
                eq(10L), any(RegisterTournamentTeamRequest.class));
    }

    @Test
    void registerTeam_shouldReturn400WhenTeamIdIsMissing()
            throws Exception {
        String json = "{}";

        mockMvc.perform(post("/api/tournaments/10/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tournamentTeamService);
    }

    @Test
    void getTeams_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/tournaments/10/teams"))
                .andExpect(status().isOk());

        verify(tournamentTeamService).getTeams(10L);
    }

    @Test
    void unregisterTeam_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/tournaments/10/teams/20"))
                .andExpect(status().isNoContent());

        verify(tournamentTeamService).unregisterTeam(10L, 20L);
    }
}

