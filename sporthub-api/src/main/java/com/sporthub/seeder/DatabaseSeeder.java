package com.sporthub.seeder;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleSeeder roleSeeder;
    private final PermissionSeeder permissionSeeder;
    private final DevelopmentSeeder developmentSeeder;

    @Override
    @Transactional
    public void run(String... args) {

        roleSeeder.seed();

        permissionSeeder.seed();

        developmentSeeder.seed();
    }
}