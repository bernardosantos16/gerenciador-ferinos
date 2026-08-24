package com.bernardo.geradortimes.club.service;

import com.bernardo.geradortimes.auth.security.CurrentUserService;
import com.bernardo.geradortimes.club.dto.response.ClubMembershipRequestResponseDTO;
import com.bernardo.geradortimes.club.dto.response.InviteTokenResponseDTO;
import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.club.model.ClubMembershipRequest;
import com.bernardo.geradortimes.club.rabbitmq.ClubMembershipRequestedEvent;
import com.bernardo.geradortimes.club.rabbitmq.ClubMembershipRequestedProducer;
import com.bernardo.geradortimes.club.repository.ClubMemberRepository;
import com.bernardo.geradortimes.club.repository.ClubMembershipRequestRepository;
import com.bernardo.geradortimes.club.repository.ClubRepository;
import com.bernardo.geradortimes.notification.model.NotificationType;
import com.bernardo.geradortimes.notification.service.NotificationService;
import com.bernardo.geradortimes.shared.api.FieldValidationException;
import com.bernardo.geradortimes.shared.enums.ActivityStatus;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import com.bernardo.geradortimes.shared.enums.JoinPolicy;
import com.bernardo.geradortimes.shared.enums.MembershipRequestStatus;
import com.bernardo.geradortimes.user.model.User;
import com.bernardo.geradortimes.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Service
@Transactional
public class ClubMembershipService {

    private final ClubRepository clubRepository;
    private final ClubMembershipRequestRepository membershipRequestRepository;
    private final ClubInviteTokenService clubInviteTokenService;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubMemberService clubMemberService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ClubAuthorizationService clubAuthorizationService;
    private final NotificationService notificationService;
    private final ClubMembershipRequestedProducer membershipRequestedProducer;

    @Value("${app.membership-request.cooldown-hours:24}")
    private int cooldownHours;

    public ClubMembershipService(
            ClubRepository clubRepository,
            ClubMembershipRequestRepository membershipRequestRepository,
            ClubInviteTokenService clubInviteTokenService,
            ClubMemberRepository clubMemberRepository,
            ClubMemberService clubMemberService,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ClubAuthorizationService clubAuthorizationService,
            NotificationService notificationService,
            ClubMembershipRequestedProducer membershipRequestedProducer
    ) {
        this.clubRepository = clubRepository;
        this.membershipRequestRepository = membershipRequestRepository;
        this.clubInviteTokenService = clubInviteTokenService;
        this.clubMemberRepository = clubMemberRepository;
        this.clubMemberService = clubMemberService;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.clubAuthorizationService = clubAuthorizationService;
        this.notificationService = notificationService;
        this.membershipRequestedProducer = membershipRequestedProducer;
    }

    public InviteTokenResponseDTO getInviteToken(UUID clubId) {
        clubAuthorizationService.requireDirector(clubId);
        return clubInviteTokenService.getCurrent(clubId);
    }

    public InviteTokenResponseDTO regenerateInviteToken(UUID clubId) {
        clubAuthorizationService.requireDirector(clubId);
        return clubInviteTokenService.issue(clubId);
    }

    public ClubMembershipRequestResponseDTO requestJoin(UUID clubId, String rawToken) {
        UUID userId = currentUserService.requireUserId();

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "club not found"));

        if (club.getStatus() != ActivityStatus.ACTIVE) {
            log.warn("Solicitacao de ingresso rejeitada - clube inativo - clubId: {}, userId: {}", clubId, userId);
            throw new FieldValidationException(CONFLICT, "club", "club is not active");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "unauthorized"));

        if (clubMemberRepository.existsByClubIdAndUserId(clubId, userId)) {
            log.warn("Solicitacao de ingresso rejeitada - ja e membro - clubId: {}, userId: {}", clubId, userId);
            throw new FieldValidationException(CONFLICT, "membership", "already a member or pending");
        }

        if (membershipRequestRepository.existsByClubIdAndUserIdAndStatus(clubId, userId, MembershipRequestStatus.PENDING)) {
            log.warn("Solicitacao de ingresso rejeitada - ja possui solicitacao pendente - clubId: {}, userId: {}", clubId, userId);
            throw new FieldValidationException(CONFLICT, "membership", "already a member or pending");
        }

        if (isInCooldown(clubId, userId)) {
            log.warn("Solicitacao de ingresso rejeitada - cooldown ativo - clubId: {}, userId: {}", clubId, userId);
            throw new FieldValidationException(CONFLICT, "membership", "membership request already submitted recently");
        }

        if (club.getJoinPolicy() == JoinPolicy.INVITE_ONLY) {
            clubInviteTokenService.validate(clubId, rawToken);
        }

        ClubMembershipRequest request = ClubMembershipRequest.create(
                clubId,
                userId,
                user.getName(),
                user.getNickname().getValue()
        );
        try {
            request = membershipRequestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException e) {
            log.warn("Solicitacao de ingresso rejeitada - conflito concorrente - clubId: {}, userId: {}", clubId, userId);
            throw new FieldValidationException(CONFLICT, "membership", "already a member or pending");
        }

        notifyDirectors(club, user);
        log.info("Solicitacao de ingresso criada - requestId: {}, clubId: {}, userId: {}", request.getId(), clubId, userId);
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public Page<ClubMembershipRequestResponseDTO> listRequests(UUID clubId, MembershipRequestStatus status, Pageable pageable) {
        clubAuthorizationService.requireDirector(clubId);
        MembershipRequestStatus effectiveStatus = status != null ? status : MembershipRequestStatus.PENDING;
        return membershipRequestRepository.findByClubIdAndStatus(clubId, effectiveStatus, pageable)
                .map(this::toResponse);
    }

    public ClubMembershipRequestResponseDTO approve(UUID clubId, Long requestId) {
        clubAuthorizationService.requireDirector(clubId);
        UUID directorId = currentUserService.requireUserId();

        ClubMembershipRequest request = findRequestForDecision(clubId, requestId);
        request.approve(directorId);
        membershipRequestRepository.save(request);

        if (!clubMemberRepository.existsByClubIdAndUserId(clubId, request.getUserId())) {
            clubMemberService.createUserMember(request.getUserId(), clubId, request.getName());
        }

        Club club = requireClub(clubId);
        notificationService.create(
                request.getUserId(),
                NotificationType.MEMBERSHIP_APPROVED,
                "Solicitação aprovada",
                "Sua solicitação para entrar no clube " + club.getName() + " foi aprovada."
        );
        log.info("Solicitacao de ingresso aprovada - requestId: {}, clubId: {}, userId: {}", requestId, clubId, request.getUserId());
        return toResponse(request);
    }

    public ClubMembershipRequestResponseDTO reject(UUID clubId, Long requestId) {
        clubAuthorizationService.requireDirector(clubId);
        UUID directorId = currentUserService.requireUserId();

        ClubMembershipRequest request = findRequestForDecision(clubId, requestId);
        request.reject(directorId);
        membershipRequestRepository.save(request);

        Club club = requireClub(clubId);
        notificationService.create(
                request.getUserId(),
                NotificationType.MEMBERSHIP_REJECTED,
                "Solicitação recusada",
                "Sua solicitação para entrar no clube " + club.getName() + " foi recusada."
        );
        log.info("Solicitacao de ingresso recusada - requestId: {}, clubId: {}, userId: {}", requestId, clubId, request.getUserId());
        return toResponse(request);
    }

    private void notifyDirectors(Club club, User requester) {
        List<ClubMember> directors = clubMemberRepository.findByClubIdAndClubRole(club.getId(), ClubRole.DIRECTOR);
        for (ClubMember director : directors) {
            if (director.getUserId() == null) {
                continue;
            }
            notificationService.create(
                    director.getUserId(),
                    NotificationType.MEMBERSHIP_REQUEST,
                    "Nova solicitação de ingresso",
                    requester.getName() + " (" + requester.getNickname().getValue() + ") quer entrar no clube " + club.getName()
            );
            membershipRequestedProducer.publish(new ClubMembershipRequestedEvent(
                    club.getId(),
                    club.getName(),
                    director.getUserId(),
                    requester.getName(),
                    requester.getNickname().getValue()
            ));
        }
    }

    private boolean isInCooldown(UUID clubId, UUID userId) {
        Instant threshold = Instant.now().minus(cooldownHours, ChronoUnit.HOURS);
        return membershipRequestRepository.findFirstByClubIdAndUserIdOrderByRequestedAtDesc(clubId, userId)
                .filter(request -> request.getRequestedAt().isAfter(threshold))
                .map(request -> request.getStatus() != MembershipRequestStatus.PENDING)
                .orElse(false);
    }

    private ClubMembershipRequest findRequestForDecision(UUID clubId, Long requestId) {
        ClubMembershipRequest request = membershipRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "membership request not found"));
        if (!clubId.equals(request.getClubId())) {
            throw new ResponseStatusException(NOT_FOUND, "membership request not found");
        }
        if (!request.isPending()) {
            throw new FieldValidationException(CONFLICT, "membership", "membership request already decided");
        }
        return request;
    }

    private Club requireClub(UUID clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "club not found"));
    }

    private ClubMembershipRequestResponseDTO toResponse(ClubMembershipRequest request) {
        return new ClubMembershipRequestResponseDTO(
                request.getId(),
                request.getClubId(),
                request.getUserId(),
                request.getName(),
                request.getNickname(),
                request.getStatus(),
                request.getRequestedAt(),
                request.getReviewedAt()
        );
    }
}
