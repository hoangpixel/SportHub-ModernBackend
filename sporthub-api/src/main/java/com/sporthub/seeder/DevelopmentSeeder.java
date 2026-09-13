package com.sporthub.seeder;

import com.sporthub.constant.Roles;
import com.sporthub.entity.*;
import com.sporthub.enums.UserStatus;
import com.sporthub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DevelopmentSeeder {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SportRepository sportRepository;

    private final PasswordEncoder passwordEncoder;

    public void seed() {

        seedUser(
                "user01",
                "user01@example.com",
                "12345678",
                "SportHub User",
                List.of(Roles.USER)
        );

        seedUser(
                "owner01",
                "owner01@example.com",
                "12345678",
                "SportHub Owner",
                List.of(
                        Roles.USER,
                        Roles.OWNER
                )
        );

        seedUser(
                "admin01",
                "admin01@example.com",
                "12345678",
                "SportHub Admin",
                List.of(
                        Roles.USER,
                        Roles.ADMIN
                )
        );

        seedSports();
    }

    private void seedUser(
            String username,
            String email,
            String password,
            String fullName,
            List<String> roleNames) {

        User user = userRepository
                .findByUsername(username)
                .orElse(null);

        if (user == null) {

            user = new User();

            user.setUsername(username);
            user.setEmail(email);

            user.setPasswordHash(
                    passwordEncoder.encode(password)
            );

            user.setStatus(UserStatus.ACTIVE);

            UserProfile profile = new UserProfile();
            profile.setFullName(fullName);
            profile.setUser(user);

            user.setProfile(profile);

            user = userRepository.save(user);
        }

        for (String roleName : roleNames) {

            Role role = roleRepository
                    .findByName(roleName)
                    .orElseThrow();

            boolean hasRole =
                    userRoleRepository
                            .existsByUser_IdAndRole_Id(
                                    user.getId(),
                                    role.getId()
                            );

            if (hasRole) {
                continue;
            }

            UserRole userRole = new UserRole();

            userRole.setId(
                    new UserRoleId(
                            user.getId(),
                            role.getId()
                    )
            );

            userRole.setUser(user);
            userRole.setRole(role);

            userRoleRepository.save(userRole);
        }
    }

        private void seedSports() {

        seedSport(
                "Football",
                "Môn bóng đá",
                "/images/sports/football.png"
        );

        seedSport(
                "Badminton",
                "Môn cầu lông",
                "/images/sports/badminton.png"
        );

        seedSport(
                "Tennis",
                "Môn quần vợt",
                "/images/sports/tennis.png"
        );

        seedSport(
                "Basketball",
                "Môn bóng rổ",
                "/images/sports/basketball.png"
        );

        seedSport(
                "Volleyball",
                "Môn bóng chuyền",
                "/images/sports/volleyball.png"
        );

        seedSport(
                "Pickleball",
                "Môn Pickleball",
                "/images/sports/pickleball.png"
        );
        }

        private void seedSport(
                String name,
                String description,
                String iconUrl) {

        if (sportRepository.existsByNameIgnoreCase(name)) {
                return;
        }

        Sport sport = new Sport();

        sport.setName(name);
        sport.setDescription(description);
        sport.setIconUrl(iconUrl);

        sportRepository.save(sport);
        }
}