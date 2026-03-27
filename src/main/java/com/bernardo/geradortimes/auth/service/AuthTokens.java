package com.bernardo.geradortimes.auth.service;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        long expiresInSeconds
) {
}
