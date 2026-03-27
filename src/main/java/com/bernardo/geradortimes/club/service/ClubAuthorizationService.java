package com.bernardo.geradortimes.club.service;

import com.bernardo.geradortimes.auth.security.CurrentUserService;
import com.bernardo.geradortimes.club.repository.ClubMemberRepository;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@Transactional(readOnly = true)
public class ClubAuthorizationService {

    private final ClubMemberRepository clubMemberRepository;
    private final CurrentUserService currentUserService;

    public ClubAuthorizationService(ClubMemberRepository clubMemberRepository, CurrentUserService currentUserService) {
        this.clubMemberRepository = clubMemberRepository;
        this.currentUserService = currentUserService;
    }

    public void requireMember(UUID clubId) {
        UUID userId = currentUserService.requireUserId();
        boolean isMember = clubMemberRepository.existsByClubIdAndUserId(clubId, userId);
        if (!isMember) {
            throw new ResponseStatusException(FORBIDDEN, "not a club member");
        }
    }

    public void requireDirector(UUID clubId) {
        UUID userId = currentUserService.requireUserId();
        boolean isDirector = clubMemberRepository.existsByClubIdAndUserIdAndClubRole(clubId, userId, ClubRole.DIRECTOR);
        if (!isDirector) {
            throw new ResponseStatusException(FORBIDDEN, "director role required");
        }
    }
}

