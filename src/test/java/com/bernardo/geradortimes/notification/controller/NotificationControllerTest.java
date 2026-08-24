package com.bernardo.geradortimes.notification.controller;

import com.bernardo.geradortimes.notification.model.Notification;
import com.bernardo.geradortimes.notification.model.NotificationType;
import com.bernardo.geradortimes.support.IntegrationTestBase;
import com.bernardo.geradortimes.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link NotificationController}.
 * <p>
 * Covers: listar notificacoes (todas/nao lidas) e marcar como lida.
 */
@DisplayName("NotificationController – Integration Tests")
class NotificationControllerTest extends IntegrationTestBase {

    private Notification persistNotification(User user, String title) {
        return notificationRepository.save(
                Notification.create(user.getId(), NotificationType.MEMBERSHIP_REQUEST, title, "message")
        );
    }

    // ── GET /api/notifications ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/notifications")
    class ListNotifications {

        @Test
        @DisplayName("deve listar notificacoes do usuario autenticado")
        void listSuccess() throws Exception {
            User user = createActiveUser("notif_user@club.com", "notif_user");
            persistNotification(user, "titulo");

            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", bearerToken(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].title", is("titulo")));
        }

        @Test
        @DisplayName("deve retornar apenas nao lidas quando unread=true")
        void listUnreadOnly() throws Exception {
            User user = createActiveUser("notif_unread@club.com", "notif_unread");
            Notification read = persistNotification(user, "lida");
            read.markRead();
            notificationRepository.save(read);
            persistNotification(user, "nao lida");

            mockMvc.perform(get("/api/notifications")
                            .param("unread", "true")
                            .header("Authorization", bearerToken(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].title", is("nao lida")));
        }

        @Test
        @DisplayName("deve retornar 401 quando nao autenticado")
        void listUnauthorized() throws Exception {
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── PATCH /api/notifications/{id}/read ─────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/notifications/{id}/read")
    class MarkRead {

        @Test
        @DisplayName("deve marcar como lida e retornar 204")
        void markReadSuccess() throws Exception {
            User user = createActiveUser("notif_read@club.com", "notif_read");
            Notification notification = persistNotification(user, "titulo");

            mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                            .header("Authorization", bearerToken(user)))
                    .andExpect(status().isNoContent());

            assertTrue(notificationRepository.findById(notification.getId()).orElseThrow().isRead());
        }

        @Test
        @DisplayName("deve retornar 404 quando notificacao pertence a outro usuario")
        void markReadOtherUser() throws Exception {
            User owner = createActiveUser("notif_owner@club.com", "notif_owner");
            User other = createActiveUser("notif_other@club.com", "notif_other");
            Notification notification = persistNotification(owner, "titulo");

            mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                            .header("Authorization", bearerToken(other)))
                    .andExpect(status().isNotFound());
        }
    }
}
