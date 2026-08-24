package com.bernardo.geradortimes.club.rabbitmq;

import java.util.UUID;

public record ClubMembershipRequestedEvent(
        UUID clubId,
        String clubName,
        UUID directorUserId,
        String requesterName,
        String requesterNickname
) {
}
