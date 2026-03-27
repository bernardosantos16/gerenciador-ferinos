package com.bernardo.geradortimes.club.service;

import com.bernardo.geradortimes.auth.security.CurrentUserService;
import com.bernardo.geradortimes.club.dto.request.CreateClubRequestDTO;
import com.bernardo.geradortimes.club.dto.response.ClubResponseDTO;
import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.club.repository.ClubRepository;
import com.bernardo.geradortimes.shared.value_object.Nickname;
import com.bernardo.geradortimes.user.model.User;
import com.bernardo.geradortimes.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@Transactional
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberService clubMemberService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;


    public ClubService(
            ClubRepository clubRepository,
            ClubMemberService clubMemberService,
            UserRepository userRepository,
            CurrentUserService currentUserService
    ) {
        this.clubRepository = clubRepository;
        this.clubMemberService = clubMemberService;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
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
        return new ClubResponseDTO(saved.getId(), saved.getName(), saved.getNickname().getValue());
    }


    public ClubResponseDTO getClub(UUID id) {
        Club club = clubRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "club not found"));

        return new ClubResponseDTO(club.getId(), club.getName(), club.getNickname().getValue());
    }
}
