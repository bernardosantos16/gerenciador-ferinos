package com.bernardo.geradortimes.auth.security;

import com.bernardo.geradortimes.shared.enums.ActivityStatus;
import com.bernardo.geradortimes.shared.enums.UserRole;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record UserPrincipal(
        @NonNull UUID id,
        @NonNull String username,
        @NonNull UserRole role,
        @NonNull ActivityStatus status
) implements UserDetails {

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
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
