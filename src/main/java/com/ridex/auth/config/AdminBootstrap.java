package com.ridex.auth.config;

import com.ridex.auth.model.Role;
import com.ridex.auth.model.User;
import com.ridex.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds a single ADMIN account on startup if none exists, so there is always
 * someone who can approve drivers. Credentials come from ridex.admin.* config.
 */
@Configuration
@EnableConfigurationProperties(AdminProperties.class)
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    @Bean
    public ApplicationRunner seedAdmin(UserRepository users,
                                       PasswordEncoder encoder,
                                       AdminProperties props) {
        return args -> {
            if (users.existsByRole(Role.ADMIN)) {
                log.info("Admin account already present; skipping bootstrap.");
                return;
            }
            User admin = User.builder()
                    .email(props.bootstrapEmail().toLowerCase())
                    .passwordHash(encoder.encode(props.bootstrapPassword()))
                    .name("RideX Admin")
                    .role(Role.ADMIN)
                    .approved(true)
                    .build();
            users.save(admin);
            log.warn("Bootstrapped ADMIN account '{}'. Change the password in any real environment.",
                    props.bootstrapEmail());
        };
    }
}
