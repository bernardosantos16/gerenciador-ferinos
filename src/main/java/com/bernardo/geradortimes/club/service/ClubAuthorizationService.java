package com.bernardo.geradortimes.club.service;

import com.bernardo.geradortimes.auth.security.CurrentUserService;
import com.bernardo.geradortimes.club.repository.ClubMemberRepository;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Slf4j
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
            log.warn("Autorizacao negada - usuario nao e membro do clube - userId: {}, clubId: {}, requiredRole: MEMBER",
                    userId, clubId);
            throw new ResponseStatusException(FORBIDDEN, "not a club member");
        }
    }

    public void requireDirector(UUID clubId) {
        UUID userId = currentUserService.requireUserId();
        if (!isDirector(clubId, userId)) {
            log.warn("Autorizacao negada - usuario nao e diretor do clube - userId: {}, clubId: {}, requiredRole: DIRECTOR",
                    userId, clubId);
            throw new ResponseStatusException(FORBIDDEN, "director role required");
        }
    }

    public boolean isDirector(UUID clubId) {
        return isDirector(clubId, currentUserService.requireUserId());
    }

    private boolean isDirector(UUID clubId, UUID userId) {
        return clubMemberRepository.existsByClubIdAndUserIdAndClubRole(clubId, userId, ClubRole.DIRECTOR);
    }
}

