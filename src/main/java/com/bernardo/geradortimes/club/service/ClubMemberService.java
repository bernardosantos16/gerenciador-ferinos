package com.bernardo.geradortimes.club.service;

import com.bernardo.geradortimes.club.dto.request.AddClubMemberRequestDTO;
import com.bernardo.geradortimes.club.dto.request.UpdateClubMemberRequestDTO;
import com.bernardo.geradortimes.club.dto.response.ClubMemberResponseDTO;
import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.club.repository.ClubMemberRepository;
import com.bernardo.geradortimes.club.repository.ClubRepository;
import com.bernardo.geradortimes.shared.api.FieldValidationException;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@Transactional
public class ClubMemberService {

    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;
    private final ClubAuthorizationService clubAuthorizationService;

    public ClubMemberService(
            ClubMemberRepository clubMemberRepository,
            ClubRepository clubRepository,
            ClubAuthorizationService clubAuthorizationService
    ) {
        this.clubMemberRepository = clubMemberRepository;
        this.clubRepository = clubRepository;
        this.clubAuthorizationService = clubAuthorizationService;
    }

    public void createDirectorMember(UUID userId, UUID clubId, String name) {
        ClubMember clubMember = ClubMember.create(
                userId,
                clubId,
                name,
                3, // default rating
                0,
                0,
                null,
                ClubRole.DIRECTOR
        );
        clubMemberRepository.save(clubMember);
        log.info("Membro diretor criado - clubId: {}, userId: {}", clubId, userId);
    }

    public void createUserMember(UUID userId, UUID clubId, String name) {
        ClubMember clubMember = ClubMember.create(
                userId,
                clubId,
                name,
                3,
                0,
                0,
                null,
                ClubRole.MEMBER
        );
        clubMemberRepository.save(clubMember);
        log.info("Membro usuario criado - clubId: {}, userId: {}", clubId, userId);
    }


    public ClubMemberResponseDTO addNonUserClubMember(UUID clubId, AddClubMemberRequestDTO requestDTO) {
        clubAuthorizationService.requireDirector(clubId);
        ClubMember clubMember = ClubMember.create(
                null,
                clubId,
                requestDTO.name(),
                requestDTO.rating(),
                0,
                0,
                null,
                ClubRole.MEMBER
        );
        clubMemberRepository.save(clubMember);
        log.info("Membro sem usuario adicionado ao clube - memberId: {}, clubId: {}", clubMember.getId(), clubId);
        return toResponse(clubMember, true, null);
    }

    public Page<ClubMemberResponseDTO> paginateMembers(UUID clubId, Pageable pageable) {
        clubAuthorizationService.requireMember(clubId);
        boolean isDirector = clubAuthorizationService.isDirector(clubId);
        UUID ownerUserId = clubRepository.findById(clubId).map(Club::getOwnerUserId).orElse(null);
        Page<ClubMember> members = clubMemberRepository.findByClubId(clubId, pageable);
        return members.map(member -> toResponse(member, isDirector, ownerUserId));
    }

    @Transactional
    public ClubMemberResponseDTO getMember(UUID clubId, Long memberId) {
        clubAuthorizationService.requireDirector(clubId);
        ClubMember member = clubMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "member not found"));
        UUID ownerUserId = clubRepository.findById(clubId).map(Club::getOwnerUserId).orElse(null);

        return toResponse(member, true, ownerUserId);
    }

    @Transactional
    public ClubMemberResponseDTO updateMember(UUID clubId, Long memberId, UpdateClubMemberRequestDTO request) {
        clubAuthorizationService.requireDirector(clubId);
        ClubMember member = clubMemberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "member not found"));
        if (!clubId.equals(member.getClubId())) {
            throw new ResponseStatusException(NOT_FOUND, "member not found");
        }

        if (request.name() != null && !request.name().isBlank()) {
            member.changeName(request.name());
        }
        if (request.rating() != null) {
            member.changeRating(request.rating());
        }
        if (request.timesChampion() != null) {
            member.changeTimesChampion(request.timesChampion());
        }
        if (request.timesMvp() != null) {
            member.changeTimesMvp(request.timesMvp());
        }

        ClubMember saved = clubMemberRepository.save(member);
        UUID ownerUserId = clubRepository.findById(clubId).map(Club::getOwnerUserId).orElse(null);
        log.info("Membro atualizado - memberId: {}, clubId: {}, ratingChanged: {}",
                memberId, clubId, request.rating() != null);
        return toResponse(saved, true, ownerUserId);
    }

    public void promoteMemberToDirector(UUID clubId, Long memberId) {
        clubAuthorizationService.requireDirector(clubId);
        ClubMember member = clubMemberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "member not found"));
        if (!clubId.equals(member.getClubId())) {
            throw new ResponseStatusException(NOT_FOUND, "member not found");
        }
        if (member.getUserId() == null) {
            throw new FieldValidationException(CONFLICT, "member", "member has no linked user account");
        }
        if (member.getClubRole() == ClubRole.DIRECTOR) {
            throw new FieldValidationException(CONFLICT, "clubRole", "member is already a director");
        }
        member.changeRole(ClubRole.DIRECTOR);
        clubMemberRepository.save(member);
        log.info("Membro promovido a diretor - memberId: {}, clubId: {}", memberId, clubId);
    }

    public void demoteDirectorToMember(UUID clubId, Long memberId) {
        clubAuthorizationService.requireDirector(clubId);
        ClubMember member = clubMemberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "member not found"));
        if (!clubId.equals(member.getClubId())) {
            throw new ResponseStatusException(NOT_FOUND, "member not found");
        }
        if (member.getUserId() == null) {
            throw new FieldValidationException(CONFLICT, "member", "member has no linked user account");
        }
        if (member.getClubRole() == ClubRole.MEMBER) {
            throw new FieldValidationException(CONFLICT, "clubRole", "member is already a member");
        }
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "club not found"));
        if (member.getUserId().equals(club.getOwnerUserId())) {
            throw new FieldValidationException(CONFLICT, "clubRole", "cannot demote the club owner");
        }
        member.changeRole(ClubRole.MEMBER);
        clubMemberRepository.save(member);
        log.info("Membro rebaixado a membro - memberId: {}, clubId: {}", memberId, clubId);
    }

    public void removeMember(UUID clubId, Long memberId) {
        clubAuthorizationService.requireDirector(clubId);
        ClubMember member = clubMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "member not found"));
        if (!clubId.equals(member.getClubId())) {
            throw new ResponseStatusException(NOT_FOUND, "member not found");
        }
        clubMemberRepository.delete(member);
        log.info("Membro removido do clube - memberId: {}, clubId: {}", memberId, clubId);
    }

    private static ClubMemberResponseDTO toResponse(ClubMember member, boolean includeSensitive, UUID ownerUserId) {
        return new ClubMemberResponseDTO(
                member.getId(),
                member.getUserId(),
                member.getName(),
                includeSensitive ? member.getRating() : null,
                includeSensitive ? member.getTimesMvp() : null,
                includeSensitive ? member.getTimesChampion() : null,
                member.getTeamId(),
                member.getClubRole(),
                ownerUserId != null && ownerUserId.equals(member.getUserId())
        );
    }

}
