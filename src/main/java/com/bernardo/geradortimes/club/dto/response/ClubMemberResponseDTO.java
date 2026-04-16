package com.bernardo.geradortimes.club.dto.response;

import com.bernardo.geradortimes.shared.enums.ClubRole;

import java.util.UUID;

public record ClubMemberResponseDTO(
        Long id,
        UUID userId,
        String name,
        Integer rating,
        Integer timesMvp,
        Integer timesChampion,
        Long teamId,
        ClubRole clubRole
) {
}

