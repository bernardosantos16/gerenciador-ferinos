package com.bernardo.geradortimes.user.rabbitmq;

import com.bernardo.geradortimes.shared.enums.TokenType;

import java.util.UUID;

public record PasswordResetEvent(
        UUID userId,
        String email,
        String token
) {
    public TokenType tokenType() {
        return TokenType.PASSWORD_RESET;
    }
}
