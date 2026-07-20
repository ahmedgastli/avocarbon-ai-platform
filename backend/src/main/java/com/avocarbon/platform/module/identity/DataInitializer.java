package com.avocarbon.platform.module.identity;

import com.avocarbon.platform.module.identity.Role;
import com.avocarbon.platform.module.identity.User;
import com.avocarbon.platform.module.identity.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Database is empty. Seeding default admin user...");
            User admin = User.builder()
                    .firstName("Admin")
                    .lastName("AVOCarbon")
                    .email("admin@avocarbon.com")
                    .password(passwordEncoder.encode("admin12345"))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Default admin user seeded: admin@avocarbon.com / admin12345");
        }
    }
}
