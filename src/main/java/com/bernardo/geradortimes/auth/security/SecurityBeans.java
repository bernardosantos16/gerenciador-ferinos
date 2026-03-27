package com.bernardo.geradortimes.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
public class SecurityBeans {

    /**
     * Prevent Spring Boot from auto-creating an in-memory default user (and printing a generated password)
     * since authentication is handled via JWT.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("UserDetailsService is not used (JWT auth)");
        };
    }
}

