package com.sporthub.service;

import com.sporthub.constant.Roles;
import com.sporthub.dto.request.LoginRequest;
import com.sporthub.dto.request.RegisterRequest;
import com.sporthub.dto.response.LoginResponse;
import com.sporthub.dto.response.RegisterResponse;
import com.sporthub.entity.*;
import com.sporthub.enums.UserStatus;
import com.sporthub.exception.ConflictException;
import com.sporthub.repository.RoleRepository;
import com.sporthub.repository.UserRepository;
import com.sporthub.repository.UserRoleRepository;
import com.sporthub.security.JwtService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(
                request.getUsername())) {

        throw new ConflictException(
                "Username đã tồn tại"
        );
        }

        if (userRepository.existsByEmail(
                request.getEmail())) {

        throw new ConflictException(
                "Email đã tồn tại"
        );
        }

        // 3. Tạo User
        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setStatus(UserStatus.ACTIVE);

        // 4. Tạo Profile
        UserProfile profile = new UserProfile();

        profile.setFullName(request.getFullName());
        profile.setUser(user);

        user.setProfile(profile);

        // 5. Save User
        user = userRepository.save(user);

        // 6. Lấy ROLE USER
        Role userRole = roleRepository
                .findByName(Roles.USER)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy role USER"
                        )
                );

        // 7. Gán USER role
        UserRole userRoleEntity = new UserRole();

        userRoleEntity.setId(
                new UserRoleId(
                        user.getId(),
                        userRole.getId()
                )
        );

        userRoleEntity.setUser(user);
        userRoleEntity.setRole(userRole);

        userRoleRepository.save(userRoleEntity);

        // 8. Trả DTO
        return new RegisterResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfile().getFullName()
        );
    }

        public LoginResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(),
                                request.getPassword()
                        )
                );

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        User user = userRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy user"
                        )
                );

        List<String> authorities =
                userDetails.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

        List<String> roles =
                authorities.stream()
                        .filter(a ->
                                a.startsWith("ROLE_")
                        )
                        .toList();

        List<String> permissions =
                authorities.stream()
                        .filter(a ->
                                !a.startsWith("ROLE_")
                        )
                        .toList();

        String accessToken =
                jwtService.generateToken(
                        userDetails,
                        user.getId()
                );

        return new LoginResponse(
                accessToken,
                "Bearer",
                user.getId(),
                userDetails.getUsername(),
                roles,
                permissions
        );
        }
}