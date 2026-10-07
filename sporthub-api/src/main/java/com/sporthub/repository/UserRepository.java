package com.sporthub.repository;

import com.sporthub.entity.User;
import com.sporthub.entity.UserProfile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByProfile(UserProfile profile);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {
        "userRoles",
        "userRoles.role"
    })
    Optional<User> findWithRolesByUsername(String username);

    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String username,
        String email,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {
        "userRoles",
        "userRoles.role"
    })
    Optional<User> findWithRolesById(Long id);

    Optional<User> findByUsernameOrEmail(
            String username,
            String email
    );
}