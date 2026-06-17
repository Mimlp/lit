package com.litsite.lit.config;

import com.litsite.lit.models.Role;
import com.litsite.lit.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private static final String[] DEFAULT_ROLES = {
            "ROLE_USER", "ROLE_MODERATOR", "ROLE_ADMIN"
    };

    @Override
    @Transactional
    public void run(String... args) {
        for (String roleName : DEFAULT_ROLES) {
            roleRepository.findByName(roleName).orElseGet(() -> {
                return roleRepository.save(new Role(roleName));
            });
        }
    }
}
