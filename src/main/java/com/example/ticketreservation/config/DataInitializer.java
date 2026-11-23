package com.example.ticketreservation.config;

import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        createUserIfNotExists(
                "admin",
                "admin123",
                "System Administrator",
                Role.ADMIN
        );

        createUserIfNotExists(
                "organizer",
                "organizer123",
                "Event Organizer",
                Role.ORGANIZER
        );

        createUserIfNotExists(
                "customer",
                "customer123",
                "Regular Customer",
                Role.CUSTOMER
        );
    }

    private void createUserIfNotExists(
            String username,
            String password,
            String fullName,
            Role role) {

        if (userRepository.existsByUsername(username)) {
            log.info("Seed user '{}' already exists.", username);
            return;
        }

        AppUser user = new AppUser();

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);

        userRepository.save(user);

        log.info("Seed user '{}' ({}) created successfully.", username, role);
    }
}
