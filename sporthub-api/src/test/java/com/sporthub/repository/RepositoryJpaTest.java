package com.sporthub.repository;

import com.sporthub.entity.*;
import com.sporthub.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryJpaTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.saveAndFlush(user);
    }

    // Derived Query + Pageable
    @Test
    void search_user_by_username_or_email_success() {
        createUser("alpha01", "alpha@gmail.com");
        createUser("beta01", "beta@gmail.com");

        Page<User> result = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("ALPHA", "ALPHA", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("alpha01", result.getContent().getFirst().getUsername());
    }

    // existsByUsername + existsByEmail
    @Test
    void exists_username_and_email_success() {
        createUser("user01", "user01@gmail.com");

        assertTrue(userRepository.existsByUsername("user01"));
        assertTrue(userRepository.existsByEmail("user01@gmail.com"));
        assertFalse(userRepository.existsByUsername("unknown"));
    }

    // JPQL PermissionRepository
    @Test
    void find_permissions_by_username_success() {
        User user = createUser("admin01", "admin@gmail.com");

        Role role = new Role();
        role.setName("ADMIN");
        role = roleRepository.saveAndFlush(role);

        Permission permission = new Permission();
        permission.setCode("VIEW_USERS");
        permission.setDescription("Xem người dùng");
        permission = permissionRepository.saveAndFlush(permission);

        UserRole userRole = new UserRole();
        userRole.setId(new UserRoleId(user.getId(), role.getId()));
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.saveAndFlush(userRole);

        RolePermission rolePermission = new RolePermission();
        rolePermission.setId(new RolePermissionId(role.getId(), permission.getId()));
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);
        rolePermissionRepository.saveAndFlush(rolePermission);

        List<Permission> result = permissionRepository.findPermissionsByUsername("admin01");

        assertEquals(1, result.size());
        assertEquals("VIEW_USERS", result.getFirst().getCode());
    }

    @Test
    void find_permissions_unknown_user_should_return_empty() {
        List<Permission> result = permissionRepository.findPermissionsByUsername("unknown");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}