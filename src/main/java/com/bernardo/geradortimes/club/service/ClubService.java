package com.bernardo.geradortimes.club.service;

import com.bernardo.geradortimes.auth.security.CurrentUserService;
import com.bernardo.geradortimes.club.dto.request.CreateClubRequestDTO;
import com.bernardo.geradortimes.club.dto.request.UpdateClubRequestDTO;
import com.bernardo.geradortimes.club.dto.response.ClubResponseDTO;
import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.club.repository.ClubMemberRepository;
import com.bernardo.geradortimes.club.repository.ClubRepository;
import com.bernardo.geradortimes.match.service.MatchService;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import com.bernardo.geradortimes.shared.value_object.Nickname;
import com.bernardo.geradortimes.user.model.User;
import com.bernardo.geradortimes.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Service
@Transactional
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberService clubMemberService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ClubAuthorizationService clubAuthorizationService;
    private final ClubMemberRepository clubMemberRepository;


    public ClubService(
            ClubRepository clubRepository,
            ClubMemberService clubMemberService,
            MatchService matchService,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ClubAuthorizationService clubAuthorizationService,
            ClubMemberRepository clubMemberRepository
    ) {
        this.clubRepository = clubRepository;
        this.clubMemberService = clubMemberService;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.clubAuthorizationService = clubAuthorizationService;
        this.clubMemberRepository = clubMemberRepository;
    }

    public ClubResponseDTO createClub(CreateClubRequestDTO requestDTO) {
        UUID userId = currentUserService.requireUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "unauthorized"));

        String nicknameValue = requestDTO.nickname() == null || requestDTO.nickname().isBlank()
                ? requestDTO.name()
                : requestDTO.nickname();

        Club club = Club.create(
                requestDTO.name(),
                Nickname.of(nicknameValue)
        );
        Club saved = clubRepository.save(club);

        // The creator is always a DIRECTOR in the club.
        clubMemberService.createDirectorMember(userId, saved.getId(), user.getName());
        log.info("Clube criado - clubId: {}, userId: {}", saved.getId(), userId);
        return new ClubResponseDTO(saved.getId(), saved.getName(), saved.getNickname().getValue());
    }


    public ClubResponseDTO getClub(UUID id) {
        Club club = clubRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "club not found"));

        return new ClubResponseDTO(club.getId(), club.getName(), club.getNickname().getValue());
    }

    public ClubResponseDTO getClubByNickname(String nickname) {
        Club club = clubRepository.findByNicknameValue(nickname)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "club not found"));

        return new ClubResponseDTO(club.getId(), club.getName(), club.getNickname().getValue());
    }

    @Transactional
    public ClubResponseDTO update(UUID clubId, UpdateClubRequestDTO request) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "club not found"));

        clubAuthorizationService.requireDirector(clubId);

        if (request.name() != null && !request.name().isBlank()) {
            club.changeName(request.name());
        }
        if (request.nickname() != null && !request.nickname().isBlank()) {
            club.changeNickname(Nickname.of(request.nickname()));
        }

        Club saved = clubRepository.save(club);
        log.info("Clube atualizado - clubId: {}", saved.getId());
        return new ClubResponseDTO(saved.getId(), saved.getName(), saved.getNickname().getValue());
    }

    @Transactional
    public ClubResponseDTO softDelete(UUID clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "club not found"));

        clubAuthorizationService.requireDirector(clubId);

        club.deactivate();
        clubRepository.save(club);
        log.info("Clube desativado (soft-delete) - clubId: {}", club.getId());
        return new ClubResponseDTO(club.getId(), club.getName(), club.getNickname().getValue());
    }

    public List<ClubResponseDTO> listUserClubs(ClubRole clubRole) {
        UUID userId = currentUserService.requireUserId();
        List<Club> clubs = clubMemberRepository.findByUserIdAndClubRole(userId, clubRole);
        return clubs.stream()
                .map(club -> new ClubResponseDTO(club.getId(), club.getName(), club.getNickname().getValue()))
                .toList();
    }
}
