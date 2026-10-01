package com.sporthub.controller;

import com.sporthub.dto.request.LoginRequest;
import com.sporthub.dto.request.RegisterRequest;
import com.sporthub.dto.response.LoginResponse;
import com.sporthub.dto.response.RegisterResponse;
import com.sporthub.security.CustomUserDetailsService;
import com.sporthub.security.JwtAuthenticationFilter;
import com.sporthub.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    void register_success_should_return_201() throws Exception {
        RegisterResponse response = new RegisterResponse(1L, "user01", "user01@gmail.com", "SportHub User");

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        String json = "{\"username\":\"user01\",\"email\":\"user01@gmail.com\",\"password\":\"12345678\",\"fullName\":\"SportHub User\"}";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("user01"))
                .andExpect(jsonPath("$.email").value("user01@gmail.com"))
                .andExpect(jsonPath("$.fullName").value("SportHub User"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void register_invalid_data_should_return_400() throws Exception {
        String json = "{\"username\":\"a\",\"email\":\"invalid-email\",\"password\":\"123\",\"fullName\":\"\"}";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void login_success_should_return_200() throws Exception {
        LoginResponse response = new LoginResponse("fake-jwt-token", "Bearer", 1L, "admin01", List.of("ROLE_ADMIN"), List.of("VIEW_USERS"));

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        String json = "{\"username\":\"admin01\",\"password\":\"12345678\"}";

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("fake-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("admin01"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.permissions[0]").value("VIEW_USERS"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void login_blank_username_should_return_400() throws Exception {
        String json = "{\"username\":\"\",\"password\":\"12345678\"}";

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}