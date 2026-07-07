package com.bernardo.geradortimes.user.rabbitmq.email_verification;

import com.bernardo.geradortimes.shared.enums.TokenType;

public record EmailVerificationEvent(
        String email,
        String token
) {
    public TokenType tokenType() {
        return TokenType.EMAIL_VERIFICATION;
    }
}
