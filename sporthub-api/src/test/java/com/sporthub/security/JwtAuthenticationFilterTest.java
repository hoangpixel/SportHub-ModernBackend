package com.sporthub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtService, userDetailsService);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueFilterChainWhenAuthorizationHeaderIsMissing() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // Then
        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void shouldAuthenticateWhenTokenIsValidAndUserIsEnabled() throws Exception {
        // Given
        String jwt = "valid-token";
        String username = "user01";

        UserDetails userDetails = User
                .withUsername(username)
                .password("password")
                .authorities("ROLE_USER")
                .disabled(false)
                .build();

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + jwt);

        when(jwtService.extractUsername(jwt))
                .thenReturn(username);

        when(userDetailsService.loadUserByUsername(username))
                .thenReturn(userDetails);

        when(jwtService.isTokenValid(jwt, userDetails))
                .thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // Then
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertNotNull(authentication);
        assertEquals(username, authentication.getName());
        assertTrue(authentication.isAuthenticated());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenTokenIsValidButUserIsDisabled() throws Exception {
        // Given
        String jwt = "valid-token";
        String username = "user01";

        UserDetails userDetails = User
                .withUsername(username)
                .password("password")
                .authorities("ROLE_USER")
                .disabled(true)
                .build();

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + jwt);

        when(jwtService.extractUsername(jwt))
                .thenReturn(username);

        when(userDetailsService.loadUserByUsername(username))
                .thenReturn(userDetails);

        when(jwtService.isTokenValid(jwt, userDetails))
                .thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain
        );

        // Then
        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(jwtService).isTokenValid(jwt, userDetails);
        verify(filterChain).doFilter(request, response);
    }
}