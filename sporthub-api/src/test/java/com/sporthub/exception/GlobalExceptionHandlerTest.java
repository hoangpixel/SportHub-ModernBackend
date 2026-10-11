package com.sporthub.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.sporthub.security.JwtService;
import com.sporthub.security.CustomUserDetailsService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.DummyController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.DummyController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @RestController
    public static class DummyController {
        @GetMapping("/test/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("Resource not found");
        }
        @GetMapping("/test/conflict")
        public void throwConflict() {
            throw new ConflictException("Conflict occurred");
        }
        @GetMapping("/test/access-denied")
        public void throwAccessDenied() {
            throw new AccessDeniedException("Access denied");
        }
        @GetMapping("/test/illegal-argument")
        public void throwIllegalArgument() {
            throw new IllegalArgumentException("Illegal argument");
        }
        @GetMapping("/test/unexpected")
        public void throwUnexpected() throws Exception {
            throw new Exception("Unexpected error");
        }
    }

    @Test
    @WithMockUser
    void handleResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser
    void handleConflictException() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser
    void handleAccessDeniedException() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser
    void handleIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser
    void handleUnexpectedException() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }
}
