package com.truholdem.config;

import com.truholdem.model.Role;
import com.truholdem.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializes required data on application startup.
 * Ensures that default roles exist in the database.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initializeRoles();
    }

    private void initializeRoles() {
        createRoleIfNotExists("USER", "Standard user role");
        createRoleIfNotExists("ADMIN", "Administrator role");
        logger.info("Role initialization completed");
    }

    private void createRoleIfNotExists(String name, String description) {
        if (!roleRepository.existsByName(name)) {
            Role role = new Role(name, description);
            roleRepository.save(role);
            logger.info("Created role: {}", name);
        } else {
            logger.debug("Role already exists: {}", name);
        }
    }
}
