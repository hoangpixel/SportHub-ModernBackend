package com.sporthub.controller;

import com.sporthub.config.PasswordConfig;
import com.sporthub.config.SecurityConfig;
import com.sporthub.exception.ConflictException;
import com.sporthub.exception.ResourceNotFoundException;
import com.sporthub.security.CustomUserDetailsService;
import com.sporthub.security.JwtAuthenticationFilter;
import com.sporthub.security.JwtService;
import com.sporthub.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({VenueController.class, OwnerVenueController.class, CourtController.class, OwnerCourtController.class})
@Import({SecurityConfig.class, PasswordConfig.class, JwtAuthenticationFilter.class})
class VenueControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean VenueService venues;
    @MockitoBean CourtService courts;
    @MockitoBean CourtScheduleService schedules;
    @MockitoBean CustomUserDetailsService users;
    @MockitoBean JwtService jwt;

    private static final String VENUE = "{\"name\":\"Arena\",\"address\":\"123 Street\"}";
    private static final String COURT = "{\"name\":\"Court A\",\"sportId\":1,\"pricePerHour\":100}";

    @Test void publicSearchDoesNotRequireLogin() throws Exception {
        when(venues.searchVenues(anyString(), any(), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Page.empty());
        when(courts.searchCourts(anyString(), any(), any(), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Page.empty());
        mvc.perform(get("/api/venues")).andExpect(status().isOk());
        mvc.perform(get("/api/courts")).andExpect(status().isOk());
    }

    @Test void unauthenticatedWriteReturns401() throws Exception {
        mvc.perform(post("/api/owner/venues").contentType(MediaType.APPLICATION_JSON).content(VENUE))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(venues);
    }

    @Test @WithMockUser(roles = "USER")
    void userWithoutPermissionCannotCreateVenueOrCourt() throws Exception {
        mvc.perform(post("/api/owner/venues").contentType(MediaType.APPLICATION_JSON).content(VENUE))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/owner/venues/1/courts").contentType(MediaType.APPLICATION_JSON).content(COURT))
                .andExpect(status().isForbidden());
        verifyNoInteractions(venues, courts);
    }

    @Test @WithMockUser(username = "owner", authorities = "CREATE_VENUE")
    void invalidVenueReturns400WithoutCallingService() throws Exception {
        mvc.perform(post("/api/owner/venues").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("errors.name").exists())
                .andExpect(jsonPath("errors.address").exists());
        verifyNoInteractions(venues);
    }

    @Test @WithMockUser(username = "owner", authorities = "MANAGE_OWN_COURTS")
    void negativePriceReturns400() throws Exception {
        mvc.perform(post("/api/owner/venues/1/courts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"sportId\":1,\"pricePerHour\":-1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("errors.pricePerHour").exists());
        verifyNoInteractions(courts);
    }

    @Test @WithMockUser(username = "owner", authorities = "CREATE_VENUE")
    void createUsesAuthenticatedUsernameAndReturns201() throws Exception {
        mvc.perform(post("/api/owner/venues").contentType(MediaType.APPLICATION_JSON).content(VENUE))
                .andExpect(status().isCreated());
        verify(venues).createVenue(eq("owner"), any());
    }

    @Test @WithMockUser(username = "owner", authorities = "UPDATE_OWN_VENUE")
    void ownershipFailureReturns403() throws Exception {
        doThrow(new AccessDeniedException("not owner")).when(venues).deleteVenue("owner", 1L);
        mvc.perform(delete("/api/owner/venues/1")).andExpect(status().isForbidden())
                .andExpect(jsonPath("status").value(403));
    }

    @Test void missingVenueAndCourtReturn404() throws Exception {
        when(venues.getVenueById(99L)).thenThrow(new ResourceNotFoundException("missing"));
        when(courts.getCourtById(99L)).thenThrow(new ResourceNotFoundException("missing"));
        mvc.perform(get("/api/venues/99")).andExpect(status().isNotFound());
        mvc.perform(get("/api/courts/99")).andExpect(status().isNotFound());
    }

    @Test @WithMockUser(username = "owner", authorities = "CREATE_VENUE")
    void duplicateVenueReturns409() throws Exception {
        when(venues.createVenue(eq("owner"), any())).thenThrow(new ConflictException("duplicate"));
        mvc.perform(post("/api/owner/venues").contentType(MediaType.APPLICATION_JSON).content(VENUE))
                .andExpect(status().isConflict()).andExpect(jsonPath("status").value(409));
    }

    @Test @WithMockUser(username = "owner", authorities = "MANAGE_OWN_COURTS")
    void mismatchedNestedCourtCannotBeUpdated() throws Exception {
        doThrow(new ResourceNotFoundException("wrong venue")).when(courts).requireCourtInVenue(2L, 1L);
        mvc.perform(put("/api/owner/venues/1/courts/2").contentType(MediaType.APPLICATION_JSON).content(COURT))
                .andExpect(status().isNotFound());
        verify(courts, never()).updateCourt(anyString(), anyLong(), any());
    }
}
