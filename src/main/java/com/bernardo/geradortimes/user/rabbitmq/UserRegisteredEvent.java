package com.bernardo.geradortimes.user.rabbitmq;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String name,
        String email,
        String verificationToken
) {
}
