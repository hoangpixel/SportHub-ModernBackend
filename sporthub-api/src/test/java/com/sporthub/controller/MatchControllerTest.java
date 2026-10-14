package com.sporthub.controller;


import com.sporthub.dto.request.CreateMatchRequest;
import com.sporthub.dto.request.UpdateMatchResultRequest;
import com.sporthub.security.JwtAuthenticationFilter;
import com.sporthub.service.MatchService;
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

@WebMvcTest(MatchController.class)
@AutoConfigureMockMvc(addFilters = false)
class MatchControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MatchService matchService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createMatch_shouldReturn201() throws Exception {
        String json = """
                {
                    "tournamentId": 10,
                    "roundNumber": 1,
                    "courtId": 5,
                    "team1Id": 20,
                    "team2Id": 30,
                    "scheduledDate": "2026-12-20",
                    "startTime": "09:00:00",
                    "endTime": "10:00:00"
                }
                """;

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(matchService).createMatch(any(CreateMatchRequest.class));
    }

    @Test
    void getMatch_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/matches/100"))
                .andExpect(status().isOk());

        verify(matchService).getMatch(100L);
    }

    @Test
    void updateResult_shouldReturn200() throws Exception {
        String json = """
                {
                    "team1Score": 3,
                    "team2Score": 1
                }
                """;

        mockMvc.perform(put("/api/matches/100/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(matchService).updateResult(
                eq(100L), any(UpdateMatchResultRequest.class));
    }

    @Test
    void updateResult_shouldReturn400WhenScoreIsNegative()
            throws Exception {
        String json = """
                {
                    "team1Score": -1,
                    "team2Score": 1
                }
                """;

        mockMvc.perform(put("/api/matches/100/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(matchService);
    }

    @Test
    void cancelMatch_shouldReturn200() throws Exception {
        mockMvc.perform(put("/api/matches/100/cancel"))
                .andExpect(status().isOk());

        verify(matchService).cancelMatch(100L);
    }
}

