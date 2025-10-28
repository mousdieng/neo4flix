package com.neo4flix.userservice.service;

import com.neo4flix.userservice.model.User;
import com.neo4flix.userservice.model.UserRole;
import com.neo4flix.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Startup runner to initialize default admin user
 */
@Component
public class StartupRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.firstName}")
    private String adminFirstName;

    @Value("${admin.lastName}")
    private String adminLastName;

    public StartupRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createDefaultAdminUser();
    }

    /**
     * Creates a default admin user if it doesn't exist
     */
    private void createDefaultAdminUser() {
        try {
            // Check if admin user already exists
            if (userRepository.existsByUsername(adminUsername)) {
                logger.info("Admin user '{}' already exists", adminUsername);
                return;
            }

            // Check if admin email already exists
            if (userRepository.existsByEmail(adminEmail)) {
                logger.warn("Admin email '{}' already exists with different username", adminEmail);
                return;
            }

            // Create new admin user
            User adminUser = new User();
            adminUser.setUsername(adminUsername);
            adminUser.setEmail(adminEmail);
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            adminUser.setFirstName(adminFirstName);
            adminUser.setLastName(adminLastName);
            adminUser.setRole(UserRole.ADMIN);
            adminUser.setEnabled(true);
            adminUser.setEmailVerified(true);
            adminUser.setAccountNonExpired(true);
            adminUser.setAccountNonLocked(true);
            adminUser.setCredentialsNonExpired(true);

            // Save admin user
            User savedAdmin = userRepository.save(adminUser);
            logger.info("========================================");
            logger.info("Default admin user created successfully!");
            logger.info("Username: {}", adminUsername);
            logger.info("Email: {}", adminEmail);
            logger.info("Password: {}", adminPassword);
            logger.info("User ID: {}", savedAdmin.getId());
            logger.info("========================================");
            logger.warn("IMPORTANT: Please change the default admin password after first login!");

        } catch (Exception e) {
            logger.error("Error creating default admin user: {}", e.getMessage(), e);
        }
    }
}
