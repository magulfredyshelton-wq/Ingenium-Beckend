package co.mz.ingenium.entity.NotificationEntity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID userId;
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    private UUID actorId;
    private UUID postId;
    private UUID roomId;
    private String text;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
