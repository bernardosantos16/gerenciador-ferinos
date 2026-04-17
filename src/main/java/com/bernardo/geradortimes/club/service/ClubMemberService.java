package com.bernardo.geradortimes.club.service;

import com.bernardo.geradortimes.club.dto.request.AddClubMemberRequestDTO;
import com.bernardo.geradortimes.club.dto.request.UpdateClubMemberRequestDTO;
import com.bernardo.geradortimes.club.dto.response.ClubMemberResponseDTO;
import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.club.repository.ClubMemberRepository;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class ClubMemberService {

    private final ClubMemberRepository clubMemberRepository;
    private final ClubAuthorizationService clubAuthorizationService;

    public ClubMemberService(
            ClubMemberRepository clubMemberRepository,
            ClubAuthorizationService clubAuthorizationService
    ) {
        this.clubMemberRepository = clubMemberRepository;
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
    }


    public void addNonUserClubMember(UUID clubId, AddClubMemberRequestDTO requestDTO) {
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
    }

    public Page<ClubMemberResponseDTO> listMembers(UUID clubId, Pageable pageable) {
        clubAuthorizationService.requireMember(clubId);
        Page<ClubMember> members = clubMemberRepository.findByClubId(clubId, pageable);
        return members
                .map(member -> new ClubMemberResponseDTO(
                        member.getId(),
                        member.getUserId(),
                        member.getName(),
                        member.getRating(),
                        member.getTimesMvp(),
                        member.getTimesChampion(),
                        member.getTeamId(),
                        member.getClubRole()
                ));
    }

    @Transactional
    public ClubMemberResponseDTO updateMember(UUID clubId, Long memberId, UpdateClubMemberRequestDTO request) {
        clubAuthorizationService.requireDirector(clubId);
        ClubMember member = clubMemberRepository.findById(memberId)
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
        if (request.clubRole() != null) {
            member.changeRole(request.clubRole());
        }

        ClubMember saved = clubMemberRepository.save(member);
        return toResponse(saved);
    }

    public void removeMember(UUID clubId, Long memberId) {
        clubAuthorizationService.requireDirector(clubId);
        ClubMember member = clubMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "member not found"));
        if (!clubId.equals(member.getClubId())) {
            throw new ResponseStatusException(NOT_FOUND, "member not found");
        }
        clubMemberRepository.delete(member);
    }

    private static ClubMemberResponseDTO toResponse(ClubMember member) {
        return new ClubMemberResponseDTO(
                member.getId(),
                member.getUserId(),
                member.getName(),
                member.getRating(),
                member.getTimesMvp(),
                member.getTimesChampion(),
                member.getTeamId(),
                member.getClubRole()
        );
    }

}
