package com.bernardo.geradortimes.notification.dto.response;

import com.bernardo.geradortimes.notification.model.NotificationType;

import java.time.Instant;

public record NotificationResponseDTO(
        Long id,
        NotificationType type,
        String title,
        String message,
        boolean read,
        Instant createdAt
) {
}
