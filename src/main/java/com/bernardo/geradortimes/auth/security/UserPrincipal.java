package com.bernardo.geradortimes.auth.security;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record UserPrincipal(
        @NonNull UUID id,
        @NonNull String username
) implements UserDetails {

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public @NonNull String getPassword() {
        // We don't authenticate with a password for JWT requests, but Spring Security expects non-null.
        return "";
    }

    @Override
    public @NonNull String getUsername() {
        return this.username;
    }

}
