package co.mz.ingenium.dto.Notification;

import co.mz.ingenium.entity.NotificationEntity.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

/** createdAt vai com fuso horário para o browser não adivinhar a hora. */
public record NotificationDTO(UUID id,
                              NotificationType type,
                              UUID actorId,
                              UUID postId,
                              UUID roomId,
                              String text,
                              boolean read,
                              OffsetDateTime createdAt) {}
