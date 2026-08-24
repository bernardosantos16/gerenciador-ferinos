package com.bernardo.geradortimes.club.dto.response;

import com.bernardo.geradortimes.shared.enums.MembershipRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record ClubMembershipRequestResponseDTO(
        Long id,
        UUID clubId,
        UUID userId,
        String name,
        String nickname,
        MembershipRequestStatus status,
        Instant requestedAt,
        Instant reviewedAt
) {
}
