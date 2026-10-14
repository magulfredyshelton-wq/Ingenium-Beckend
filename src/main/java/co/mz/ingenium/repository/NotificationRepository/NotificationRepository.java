package co.mz.ingenium.repository.NotificationRepository;

import co.mz.ingenium.entity.NotificationEntity.Notification;
import co.mz.ingenium.entity.NotificationEntity.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    Optional<Notification> findFirstByUserIdAndTypeAndRoomIdAndReadAtIsNull(
            UUID userId, NotificationType type, UUID roomId);

    @Query(value = "select follower_id from follower where followed_id = :authorId", nativeQuery = true)
    List<UUID> findFollowerIds(@Param("authorId") UUID authorId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /** Ao abrir uma conversa, as notificações de mensagem dessa pessoa ficam lidas. */
    @Modifying
    @Query("update Notification n set n.readAt = :now "
            + "where n.userId = :userId and n.actorId = :actorId "
            + "and n.type = co.mz.ingenium.entity.NotificationEntity.NotificationType.MESSAGE "
            + "and n.readAt is null")
    int markChatRead(@Param("userId") UUID userId,
                     @Param("actorId") UUID actorId,
                     @Param("now") LocalDateTime now);
}
