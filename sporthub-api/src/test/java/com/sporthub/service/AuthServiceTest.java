package com.sporthub.service;

import com.sporthub.constant.Roles;
import com.sporthub.dto.request.LoginRequest;
import com.sporthub.dto.request.RegisterRequest;
import com.sporthub.dto.response.LoginResponse;
import com.sporthub.dto.response.RegisterResponse;
import com.sporthub.entity.Role;
import com.sporthub.entity.User;
import com.sporthub.entity.UserRole;
import com.sporthub.exception.ConflictException;
import com.sporthub.repository.RoleRepository;
import com.sporthub.repository.UserRepository;
import com.sporthub.repository.UserRoleRepository;
import com.sporthub.security.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {

        authService = new AuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                authenticationManager,
                jwtService,
                passwordEncoder
        );
    }

    @Test
    void register_success() {

        RegisterRequest request =
                createRegisterRequest();

        when(
                userRepository.existsByUsername(
                        request.getUsername()
                )
        ).thenReturn(false);

        when(
                userRepository.existsByEmail(
                        request.getEmail()
                )
        ).thenReturn(false);

        when(
                passwordEncoder.encode(
                        request.getPassword()
                )
        ).thenReturn("hashed-password");

        when(
                userRepository.save(any(User.class))
        ).thenAnswer(invocation -> {

            User user =
                    invocation.getArgument(0);

            user.setId(1L);

            return user;
        });

        Role role = new Role();
        role.setId(1L);
        role.setName(Roles.USER);

        when(
                roleRepository.findByName(
                        Roles.USER
                )
        ).thenReturn(
                Optional.of(role)
        );

        RegisterResponse response =
                authService.register(request);

        assertNotNull(response);

        assertEquals(
                1L,
                response.getId()
        );

        assertEquals(
                "user01",
                response.getUsername()
        );

        assertEquals(
                "user01@example.com",
                response.getEmail()
        );

        assertEquals(
                "SportHub User",
                response.getFullName()
        );

        verify(
                userRepository
        ).save(
                any(User.class)
        );

        verify(
                userRoleRepository
        ).save(
                any(UserRole.class)
        );
    }

    @Test
    void register_duplicateUsername_shouldThrowConflict() {

        RegisterRequest request =
                createRegisterRequest();

        when(
                userRepository.existsByUsername(
                        request.getUsername()
                )
        ).thenReturn(true);

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                authService.register(
                                        request
                                )
                );

        assertEquals(
                "Username đã tồn tại",
                exception.getMessage()
        );

        verify(
                userRepository,
                never()
        ).save(
                any(User.class)
        );
    }

    @Test
    void register_duplicateEmail_shouldThrowConflict() {

        RegisterRequest request =
                createRegisterRequest();

        when(
                userRepository.existsByUsername(
                        request.getUsername()
                )
        ).thenReturn(false);

        when(
                userRepository.existsByEmail(
                        request.getEmail()
                )
        ).thenReturn(true);

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                authService.register(
                                        request
                                )
                );

        assertEquals(
                "Email đã tồn tại",
                exception.getMessage()
        );

        verify(
                userRepository,
                never()
        ).save(
                any(User.class)
        );
    }

    @Test
    void login_success() {

        LoginRequest request =
                new LoginRequest();

        request.setUsername(
                "admin01"
        );

        request.setPassword(
                "12345678"
        );

        UserDetails userDetails =
                org.springframework.security
                        .core.userdetails.User
                        .withUsername("admin01")
                        .password("hashed-password")
                        .authorities(
                                List.of(
                                        new SimpleGrantedAuthority(
                                                "ROLE_ADMIN"
                                        ),
                                        new SimpleGrantedAuthority(
                                                "VIEW_USERS"
                                        ),
                                        new SimpleGrantedAuthority(
                                                "CREATE_SPORT"
                                        )
                                )
                        )
                        .build();

        Authentication authentication =
                mock(
                        Authentication.class
                );

        when(
                authentication.getPrincipal()
        ).thenReturn(
                userDetails
        );

        when(
                authenticationManager.authenticate(
                        any()
                )
        ).thenReturn(
                authentication
        );

        User user = new User();

        user.setId(3L);
        user.setUsername(
                "admin01"
        );

        when(
                userRepository.findByUsername(
                        "admin01"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                jwtService.generateToken(
                        userDetails,
                        3L
                )
        ).thenReturn(
                "fake-jwt-token"
        );

        LoginResponse response =
                authService.login(request);

        assertEquals(
                "fake-jwt-token",
                response.getAccessToken()
        );

        assertEquals(
                "Bearer",
                response.getTokenType()
        );

        assertEquals(
                3L,
                response.getUserId()
        );

        assertEquals(
                "admin01",
                response.getUsername()
        );

        assertTrue(
                response.getRoles()
                        .contains(
                                "ROLE_ADMIN"
                        )
        );

        assertTrue(
                response.getPermissions()
                        .contains(
                                "VIEW_USERS"
                        )
        );

        assertTrue(
                response.getPermissions()
                        .contains(
                                "CREATE_SPORT"
                        )
        );
    }

    @Test
    void login_badCredentials_shouldThrowException() {

        LoginRequest request =
                new LoginRequest();

        request.setUsername(
                "admin01"
        );

        request.setPassword(
                "wrong-password"
        );

        when(
                authenticationManager.authenticate(
                        any()
                )
        ).thenThrow(
                new BadCredentialsException(
                        "Bad credentials"
                )
        );

        assertThrows(
                BadCredentialsException.class,
                () ->
                        authService.login(
                                request
                        )
        );

        verify(
                jwtService,
                never()
        ).generateToken(
                any(),
                any()
        );
    }

    private RegisterRequest createRegisterRequest() {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername(
                "user01"
        );

        request.setEmail(
                "user01@example.com"
        );

        request.setPassword(
                "12345678"
        );

        request.setFullName(
                "SportHub User"
        );

        return request;
    }
}