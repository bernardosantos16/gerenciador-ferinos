package com.bernardo.geradortimes.club.controller;

import com.bernardo.geradortimes.club.dto.request.JoinClubRequestDTO;
import com.bernardo.geradortimes.club.model.Club;
import com.bernardo.geradortimes.club.model.ClubMember;
import com.bernardo.geradortimes.club.model.ClubMembershipRequest;
import com.bernardo.geradortimes.notification.model.Notification;
import com.bernardo.geradortimes.notification.model.NotificationType;
import com.bernardo.geradortimes.shared.enums.ClubRole;
import com.bernardo.geradortimes.shared.enums.JoinPolicy;
import com.bernardo.geradortimes.shared.enums.MembershipRequestStatus;
import com.bernardo.geradortimes.support.IntegrationTestBase;
import com.bernardo.geradortimes.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link ClubMembershipController}.
 * <p>
 * Covers: solicitar ingresso (path/body), gerar token de convite, listar,
 * aprovar e recusar solicitacoes.
 */
@DisplayName("ClubMembershipController – Integration Tests")
class ClubMembershipControllerTest extends IntegrationTestBase {

    private record DirectorContext(User director, Club club) {}

    private DirectorContext setupDirector() {
        User director = createActiveUser("director@club.com", "director_nick");
        Club club = createClub("Clube Teste", "clube_teste");
        createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);
        return new DirectorContext(director, club);
    }

    // ── POST /api/clubs/{clubId}/invite (body) ─────────────────────────────────

    @Nested
    @DisplayName("POST /api/clubs/{clubId}/invite")
    class RequestJoinByBody {

        @Test
        @DisplayName("deve criar solicitacao e retornar 201 quando token valido")
        void joinWithValidToken() throws Exception {
            DirectorContext ctx = setupDirector();
            User applicant = createActiveUser("applicant@club.com", "applicant_nick");
            String token = createInviteToken(ctx.club().getId());

            mockMvc.perform(post("/api/clubs/{clubId}/invite", ctx.club().getId())
                            .header("Authorization", bearerToken(applicant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new JoinClubRequestDTO(token))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", not(emptyString())))
                    .andExpect(jsonPath("$.status", is("PENDING")))
                    .andExpect(jsonPath("$.name", is("Test User")))
                    .andExpect(jsonPath("$.nickname", is("applicant_nick")));
        }

        @Test
        @DisplayName("deve retornar 400 quando token e invalido")
        void joinWithInvalidToken() throws Exception {
            DirectorContext ctx = setupDirector();
            User applicant = createActiveUser("applicant2@club.com", "applicant2_nick");

            mockMvc.perform(post("/api/clubs/{clubId}/invite", ctx.club().getId())
                            .header("Authorization", bearerToken(applicant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new JoinClubRequestDTO("ZZZZZZ"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 404 quando clube nao existe")
        void joinClubNotFound() throws Exception {
            User applicant = createActiveUser("applicant3@club.com", "applicant3_nick");

            mockMvc.perform(post("/api/clubs/{clubId}/invite", UUID.randomUUID())
                            .header("Authorization", bearerToken(applicant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new JoinClubRequestDTO("AAAAAA"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 409 quando usuario ja e membro")
        void joinAlreadyMember() throws Exception {
            DirectorContext ctx = setupDirector();
            User applicant = createActiveUser("applicant4@club.com", "applicant4_nick");
            createClubMember(applicant.getId(), ctx.club().getId(), ClubRole.MEMBER);
            String token = createInviteToken(ctx.club().getId());

            mockMvc.perform(post("/api/clubs/{clubId}/invite", ctx.club().getId())
                            .header("Authorization", bearerToken(applicant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new JoinClubRequestDTO(token))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve retornar 409 quando ja existe solicitacao pendente")
        void joinAlreadyPending() throws Exception {
            DirectorContext ctx = setupDirector();
            User applicant = createActiveUser("applicant5@club.com", "applicant5_nick");
            createMembershipRequest(ctx.club().getId(), applicant.getId(), "Test User", "applicant5_nick");
            String token = createInviteToken(ctx.club().getId());

            mockMvc.perform(post("/api/clubs/{clubId}/invite", ctx.club().getId())
                            .header("Authorization", bearerToken(applicant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new JoinClubRequestDTO(token))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve notificar os diretores ao criar a solicitacao")
        void joinNotifiesDirectors() throws Exception {
            DirectorContext ctx = setupDirector();
            User applicant = createActiveUser("applicant6@club.com", "applicant6_nick");
            String token = createInviteToken(ctx.club().getId());

            mockMvc.perform(post("/api/clubs/{clubId}/invite", ctx.club().getId())
                            .header("Authorization", bearerToken(applicant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new JoinClubRequestDTO(token))))
                    .andExpect(status().isCreated());

            List<Notification> notifications = notificationRepository
                    .findByUserId(ctx.director().getId(), PageRequest.of(0, 10)).getContent();
            assertTrue(notifications.stream().anyMatch(n -> n.getType() == NotificationType.MEMBERSHIP_REQUEST));
        }

        @Test
        @DisplayName("deve retornar 401 quando nao autenticado")
        void joinUnauthorized() throws Exception {
            DirectorContext ctx = setupDirector();
            String token = createInviteToken(ctx.club().getId());

            mockMvc.perform(post("/api/clubs/{clubId}/invite", ctx.club().getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new JoinClubRequestDTO(token))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve criar solicitacao sem token quando politica e OPEN")
        void joinOpenPolicyWithoutToken() throws Exception {
            User director = createActiveUser("open_dir@club.com", "open_dir_nick");
            Club club = createClub("Clube Aberto", "clube_aberto");
            club.changeJoinPolicy(JoinPolicy.OPEN);
            clubRepository.save(club);
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            User applicant = createActiveUser("open_app@club.com", "open_app_nick");

            mockMvc.perform(post("/api/clubs/{clubId}/invite", club.getId())
                            .header("Authorization", bearerToken(applicant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new JoinClubRequestDTO(null))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status", is("PENDING")));
        }

        @Test
        @DisplayName("deve retornar 400 quando politica e INVITE_ONLY e token ausente")
        void joinInviteOnlyWithoutToken() throws Exception {
            DirectorContext ctx = setupDirector();
            User applicant = createActiveUser("invite_app@club.com", "invite_app_nick");

            mockMvc.perform(post("/api/clubs/{clubId}/invite", ctx.club().getId())
                            .header("Authorization", bearerToken(applicant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new JoinClubRequestDTO(null))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 409 quando usuario recusado re-solicita dentro do cooldown")
        void joinAfterRejectWithinCooldown() throws Exception {
            DirectorContext ctx = setupDirector();
            User applicant = createActiveUser("rej_app2@club.com", "rej_app2_nick");
            String token = createInviteToken(ctx.club().getId());

            ClubMembershipRequest rejected = createMembershipRequest(
                    ctx.club().getId(), applicant.getId(), "Test User", "rej_app2_nick");
            rejected.reject(ctx.director().getId());
            clubMembershipRequestRepository.save(rejected);

            mockMvc.perform(post("/api/clubs/{clubId}/invite", ctx.club().getId())
                            .header("Authorization", bearerToken(applicant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new JoinClubRequestDTO(token))))
                    .andExpect(status().isConflict());
        }
    }

    // ── GET/POST /api/clubs/{clubId}/invite-token ─────────────────────────────

    @Nested
    @DisplayName("GET /api/clubs/{clubId}/invite-token")
    class GetInviteToken {

        @Test
        @DisplayName("deve retornar o token (gerando automaticamente) e 200 para DIRECTOR")
        void getTokenAsDirector() throws Exception {
            DirectorContext ctx = setupDirector();

            mockMvc.perform(get("/api/clubs/{clubId}/invite-token", ctx.club().getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", matchesPattern("^[A-Z0-9]{6}$")))
                    .andExpect(jsonPath("$.expiresAt", not(emptyString())));
        }

        @Test
        @DisplayName("deve retornar 403 quando usuario e apenas MEMBER")
        void getTokenForbidden() throws Exception {
            User director = createActiveUser("gt2_dir@club.com", "gt2_dir_nick");
            Club club = createClub("Clube GT2", "clube_gt2");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            User member = createActiveUser("gt2_member@club.com", "gt2_member_nick");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);

            mockMvc.perform(get("/api/clubs/{clubId}/invite-token", club.getId())
                            .header("Authorization", bearerToken(member)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/clubs/{clubId}/invite-token")
    class RegenerateInviteToken {

        @Test
        @DisplayName("deve regenerar token e retornar 200 quando usuario e DIRECTOR")
        void regenerateTokenAsDirector() throws Exception {
            DirectorContext ctx = setupDirector();

            mockMvc.perform(post("/api/clubs/{clubId}/invite-token", ctx.club().getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", matchesPattern("^[A-Z0-9]{6}$")))
                    .andExpect(jsonPath("$.expiresAt", not(emptyString())));
        }

        @Test
        @DisplayName("deve retornar 403 quando usuario e apenas MEMBER")
        void regenerateTokenForbidden() throws Exception {
            User director = createActiveUser("gt_dir@club.com", "gt_dir_nick");
            Club club = createClub("Clube GT", "clube_gt");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            User member = createActiveUser("gt_member@club.com", "gt_member_nick");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);

            mockMvc.perform(post("/api/clubs/{clubId}/invite-token", club.getId())
                            .header("Authorization", bearerToken(member)))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /api/clubs/{clubId}/membership-requests ────────────────────────────

    @Nested
    @DisplayName("GET /api/clubs/{clubId}/membership-requests")
    class ListRequests {

        @Test
        @DisplayName("deve listar solicitacoes pendentes e retornar 200 para DIRECTOR")
        void listPendingAsDirector() throws Exception {
            DirectorContext ctx = setupDirector();
            User applicant = createActiveUser("list_req@club.com", "list_req_nick");
            createMembershipRequest(ctx.club().getId(), applicant.getId(), "Test User", "list_req_nick");

            mockMvc.perform(get("/api/clubs/{clubId}/membership-requests", ctx.club().getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].nickname", is("list_req_nick")));
        }

        @Test
        @DisplayName("deve retornar 403 quando usuario nao e DIRECTOR")
        void listForbidden() throws Exception {
            User director = createActiveUser("lr_dir@club.com", "lr_dir_nick");
            Club club = createClub("Clube LR", "clube_lr");
            createClubMember(director.getId(), club.getId(), ClubRole.DIRECTOR);

            User member = createActiveUser("lr_member@club.com", "lr_member_nick");
            createClubMember(member.getId(), club.getId(), ClubRole.MEMBER);

            mockMvc.perform(get("/api/clubs/{clubId}/membership-requests", club.getId())
                            .header("Authorization", bearerToken(member)))
                    .andExpect(status().isForbidden());
        }
    }

    // ── POST /api/clubs/{clubId}/membership-requests/{id}/approve ──────────────

    @Nested
    @DisplayName("POST /api/clubs/{clubId}/membership-requests/{id}/approve")
    class ApproveRequest {

        @Test
        @DisplayName("deve aprovar, criar membro MEMBER e retornar 200")
        void approveSuccess() throws Exception {
            DirectorContext ctx = setupDirector();
            User applicant = createActiveUser("appr_app@club.com", "appr_app_nick");
            ClubMembershipRequest request = createMembershipRequest(
                    ctx.club().getId(), applicant.getId(), "Test User", "appr_app_nick");

            mockMvc.perform(post("/api/clubs/{clubId}/membership-requests/{id}/approve",
                            ctx.club().getId(), request.getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("APPROVED")));

            assertTrue(clubMemberRepository.existsByClubIdAndUserId(ctx.club().getId(), applicant.getId()));
        }

        @Test
        @DisplayName("deve retornar 403 quando usuario nao e DIRECTOR")
        void approveForbidden() throws Exception {
            DirectorContext ctx = setupDirector();
            User member = createActiveUser("appr_mem@club.com", "appr_mem_nick");
            createClubMember(member.getId(), ctx.club().getId(), ClubRole.MEMBER);

            User applicant = createActiveUser("appr_app2@club.com", "appr_app2_nick");
            ClubMembershipRequest request = createMembershipRequest(
                    ctx.club().getId(), applicant.getId(), "Test User", "appr_app2_nick");

            mockMvc.perform(post("/api/clubs/{clubId}/membership-requests/{id}/approve",
                            ctx.club().getId(), request.getId())
                            .header("Authorization", bearerToken(member)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 404 quando solicitacao pertence a outro clube")
        void approveWrongClub() throws Exception {
            DirectorContext ctx = setupDirector();
            Club other = createClub("Outro Clube", "outro_clube");
            createClubMember(ctx.director().getId(), other.getId(), ClubRole.DIRECTOR);

            User applicant = createActiveUser("appr_app3@club.com", "appr_app3_nick");
            ClubMembershipRequest request = createMembershipRequest(
                    ctx.club().getId(), applicant.getId(), "Test User", "appr_app3_nick");

            mockMvc.perform(post("/api/clubs/{clubId}/membership-requests/{id}/approve",
                            other.getId(), request.getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 409 quando solicitacao ja foi decidida")
        void approveAlreadyDecided() throws Exception {
            DirectorContext ctx = setupDirector();
            User applicant = createActiveUser("appr_app4@club.com", "appr_app4_nick");
            ClubMembershipRequest request = createMembershipRequest(
                    ctx.club().getId(), applicant.getId(), "Test User", "appr_app4_nick");
            request.reject(ctx.director().getId());
            clubMembershipRequestRepository.save(request);

            mockMvc.perform(post("/api/clubs/{clubId}/membership-requests/{id}/approve",
                            ctx.club().getId(), request.getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve aprovar sem duplicar membro quando usuario ja e membro")
        void approveWhenAlreadyMember() throws Exception {
            DirectorContext ctx = setupDirector();
            User applicant = createActiveUser("appr_mem2@club.com", "appr_mem2_nick");
            createClubMember(applicant.getId(), ctx.club().getId(), ClubRole.MEMBER);
            ClubMembershipRequest request = createMembershipRequest(
                    ctx.club().getId(), applicant.getId(), "Test User", "appr_mem2_nick");

            mockMvc.perform(post("/api/clubs/{clubId}/membership-requests/{id}/approve",
                            ctx.club().getId(), request.getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("APPROVED")));

            long count = clubMemberRepository.findByClubIdAndClubRole(ctx.club().getId(), ClubRole.MEMBER)
                    .stream()
                    .filter(m -> applicant.getId().equals(m.getUserId()))
                    .count();
            assertEquals(1, count);
        }
    }

    // ── POST /api/clubs/{clubId}/membership-requests/{id}/reject ───────────────

    @Nested
    @DisplayName("POST /api/clubs/{clubId}/membership-requests/{id}/reject")
    class RejectRequest {

        @Test
        @DisplayName("deve recusar sem criar membro e retornar 200")
        void rejectSuccess() throws Exception {
            DirectorContext ctx = setupDirector();
            User applicant = createActiveUser("rej_app@club.com", "rej_app_nick");
            ClubMembershipRequest request = createMembershipRequest(
                    ctx.club().getId(), applicant.getId(), "Test User", "rej_app_nick");

            mockMvc.perform(post("/api/clubs/{clubId}/membership-requests/{id}/reject",
                            ctx.club().getId(), request.getId())
                            .header("Authorization", bearerToken(ctx.director())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("REJECTED")));

            assertFalse(clubMemberRepository.existsByClubIdAndUserId(ctx.club().getId(), applicant.getId()));
        }
    }
}
