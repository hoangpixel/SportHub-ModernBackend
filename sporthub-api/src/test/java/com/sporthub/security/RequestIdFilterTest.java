package com.sporthub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldGenerateRequestIdWhenHeaderIsMissing() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);

        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);

        requestIdFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq("X-Request-Id"), requestIdCaptor.capture());
        verify(filterChain).doFilter(request, response);

        String generatedRequestId = requestIdCaptor.getValue();

        assertNotNull(generatedRequestId);
        assertDoesNotThrow(() -> UUID.fromString(generatedRequestId));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void shouldReuseValidRequestIdFromClient() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("sport-test-001");

        doAnswer(invocation -> {
            assertEquals("sport-test-001", MDC.get("requestId"));
            return null;
        }).when(filterChain).doFilter(request, response);

        requestIdFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-Request-Id", "sport-test-001");
        verify(filterChain).doFilter(request, response);

        assertNull(MDC.get("requestId"));
    }

    @Test
    void shouldGenerateNewRequestIdWhenClientRequestIdIsInvalid() throws Exception {
        when(request.getHeader("X-Request-Id"))
                .thenReturn("hello xin chao !!!");

        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);

        requestIdFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq("X-Request-Id"), requestIdCaptor.capture());
        verify(filterChain).doFilter(request, response);

        String generatedRequestId = requestIdCaptor.getValue();

        assertNotEquals("hello xin chao !!!", generatedRequestId);
        assertDoesNotThrow(() -> UUID.fromString(generatedRequestId));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void shouldRemoveRequestIdFromMdcEvenWhenFilterChainThrowsException() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("error-test-001");

        doAnswer(invocation -> {
            assertEquals("error-test-001", MDC.get("requestId"));
            throw new RuntimeException("Test exception");
        }).when(filterChain).doFilter(request, response);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> requestIdFilter.doFilterInternal(request, response, filterChain)
        );

        assertEquals("Test exception", exception.getMessage());
        assertNull(MDC.get("requestId"));
    }
}