package com.sporthub.controller;


import com.sporthub.dto.request.CreateTournamentRequest;
import com.sporthub.dto.request.UpdateTournamentRequest;
import com.sporthub.dto.request.UpdateTournamentStatusRequest;
import com.sporthub.security.JwtAuthenticationFilter;
import com.sporthub.service.TournamentService;
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

@WebMvcTest(TournamentController.class)
@AutoConfigureMockMvc(addFilters = false)
class TournamentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TournamentService tournamentService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createTournament_shouldReturn201() throws Exception {
        String json = """
                {
                    "sportId": 1,
                    "venueId": 1,
                    "name": "Football Cup",
                    "description": "Test tournament",
                    "startDate": "2026-12-20",
                    "endDate": "2026-12-21",
                    "registrationStart": "2026-12-01",
                    "registrationEnd": "2026-12-15",
                    "maxTeams": 8
                }
                """;

        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(tournamentService)
                .createTournament(any(CreateTournamentRequest.class));
    }

    @Test
    void createTournament_shouldReturn400WhenNameIsBlank()
            throws Exception {
        String json = """
                {
                    "sportId": 1,
                    "name": "",
                    "startDate": "2026-12-20",
                    "endDate": "2026-12-21",
                    "registrationStart": "2026-12-01",
                    "registrationEnd": "2026-12-15",
                    "maxTeams": 8
                }
                """;

        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tournamentService);
    }

    @Test
    void getTournament_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/tournaments/10"))
                .andExpect(status().isOk());

        verify(tournamentService).getTournament(10L);
    }

    @Test
    void updateTournament_shouldReturn200() throws Exception {
        String json = """
                {
                    "name": "Updated Cup",
                    "description": "Updated description",
                    "startDate": "2026-12-20",
                    "endDate": "2026-12-22",
                    "registrationStart": "2026-12-01",
                    "registrationEnd": "2026-12-15",
                    "maxTeams": 16
                }
                """;

        mockMvc.perform(put("/api/tournaments/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(tournamentService).updateTournament(
                eq(10L), any(UpdateTournamentRequest.class));
    }

    @Test
    void updateStatus_shouldReturn200() throws Exception {
        String json = """
                {
                    "status": "ONGOING"
                }
                """;

        mockMvc.perform(put("/api/tournaments/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(tournamentService).updateStatus(
                eq(10L), any(UpdateTournamentStatusRequest.class));
    }
}

