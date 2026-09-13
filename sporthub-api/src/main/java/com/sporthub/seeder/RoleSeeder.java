package com.sporthub.seeder;

import com.sporthub.constant.Roles;
import com.sporthub.entity.Role;
import com.sporthub.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleSeeder {

    private final RoleRepository roleRepository;

    public void seed() {

        createIfNotExists(Roles.USER);
        createIfNotExists(Roles.OWNER);
        createIfNotExists(Roles.ADMIN);
    }

    private void createIfNotExists(String roleName) {

        if (roleRepository.findByName(roleName).isPresent()) {
            return;
        }

        Role role = new Role();
        role.setName(roleName);

        roleRepository.save(role);
    }
}