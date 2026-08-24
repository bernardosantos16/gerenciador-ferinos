package com.bernardo.geradortimes.notification.service;

import com.bernardo.geradortimes.auth.security.CurrentUserService;
import com.bernardo.geradortimes.notification.dto.response.NotificationResponseDTO;
import com.bernardo.geradortimes.notification.model.Notification;
import com.bernardo.geradortimes.notification.model.NotificationType;
import com.bernardo.geradortimes.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(NotificationRepository notificationRepository, CurrentUserService currentUserService) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    public void create(UUID userId, NotificationType type, String title, String message) {
        notificationRepository.save(Notification.create(userId, type, title, message));
        log.info("Notificacao criada - userId: {}, type: {}", userId, type);
    }

    public Page<NotificationResponseDTO> list(Boolean unreadOnly, Pageable pageable) {
        UUID userId = currentUserService.requireUserId();
        Page<Notification> page = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findByUserIdAndRead(userId, false, pageable)
                : notificationRepository.findByUserId(userId, pageable);
        return page.map(this::toResponse);
    }

    public void markRead(Long id) {
        UUID userId = currentUserService.requireUserId();
        Notification notification = notificationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "notification not found"));
        if (!userId.equals(notification.getUserId())) {
            throw new ResponseStatusException(NOT_FOUND, "notification not found");
        }
        notification.markRead();
        notificationRepository.save(notification);
        log.info("Notificacao marcada como lida - notificationId: {}, userId: {}", id, userId);
    }

    private NotificationResponseDTO toResponse(Notification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
