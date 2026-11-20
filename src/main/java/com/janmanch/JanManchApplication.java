package com.janmanch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

// Login is handled with JWT, so the default in-memory user is switched off.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableCaching
@EnableScheduling
public class JanManchApplication {

    public static void main(String[] args) {
        SpringApplication.run(JanManchApplication.class, args);
    }
}
