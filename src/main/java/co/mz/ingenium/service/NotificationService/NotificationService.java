package co.mz.ingenium.service.NotificationService;

import co.mz.ingenium.dto.Notification.NotificationDTO;
import co.mz.ingenium.entity.NotificationEntity.Notification;
import co.mz.ingenium.entity.NotificationEntity.NotificationType;
import co.mz.ingenium.repository.NotificationRepository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository repository,
                               SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    // Mensagem nova: uma notificação não lida por conversa (actualiza em vez de criar outra)
    @Transactional
    public void notifyNewMessage(UUID recipientId, UUID senderId, String senderName,
                                 UUID roomId, String preview) {
        Notification n = repository
                .findFirstByUserIdAndTypeAndRoomIdAndReadAtIsNull(recipientId, NotificationType.MESSAGE, roomId)
                .orElseGet(Notification::new);

        n.setUserId(recipientId);
        n.setType(NotificationType.MESSAGE);
        n.setActorId(senderId);
        n.setRoomId(roomId);
        n.setText(clip(senderName + ": " + shorten(preview)));
        n.setCreatedAt(LocalDateTime.now());

        push(repository.save(n));
    }

    // Post novo: avisa quem segue o autor
    @Transactional
    public void notifyFollowersOfNewPost(UUID authorId, String authorName, UUID postId) {
        List<UUID> followerIds = repository.findFollowerIds(authorId);
        if (followerIds.isEmpty()) return;

        List<Notification> list = followerIds.stream().map(followerId -> {
            Notification n = new Notification();
            n.setUserId(followerId);
            n.setType(NotificationType.POST);
            n.setActorId(authorId);
            n.setPostId(postId);
            n.setText(clip(authorName + " fez uma nova publicação"));
            n.setCreatedAt(LocalDateTime.now());
            return n;
        }).toList();

        repository.saveAll(list).forEach(this::push);
    }

    // Gosto no post: avisa o autor (nunca a si próprio)
    @Transactional
    public void notifyLike(UUID postAuthorId, UUID actorId, String actorName, UUID postId) {
        if (postAuthorId == null || postAuthorId.equals(actorId)) return;
        push(repository.save(build(postAuthorId, NotificationType.LIKE, actorId, postId,
                actorName + " gostou da tua publicação")));
    }

    // Comentário no post: avisa o autor (nunca a si próprio)
    @Transactional
    public void notifyComment(UUID postAuthorId, UUID actorId, String actorName, UUID postId, String comment) {
        if (postAuthorId == null || postAuthorId.equals(actorId)) return;
        push(repository.save(build(postAuthorId, NotificationType.COMMENT, actorId, postId,
                actorName + " comentou: " + shorten(comment))));
    }

    // Novo seguidor
    @Transactional
    public void notifyFollow(UUID followedId, UUID actorId, String actorName) {
        if (followedId.equals(actorId)) return;
        push(repository.save(build(followedId, NotificationType.FOLLOW, actorId, null,
                actorName + " começou a seguir-te")));
    }

    // Nova candidatura: avisa a empresa
    @Transactional
    public void notifyApplication(UUID companyId, UUID studentId, String studentName,
                                  UUID postId, String jobTitle) {
        push(repository.save(build(companyId, NotificationType.APPLICATION, studentId, postId,
                studentName + " candidatou-se a " + jobTitle)));
    }

    // A empresa mudou o estado da candidatura: avisa o estudante
    @Transactional
    public void notifyApplicationUpdate(UUID studentId, UUID companyId, String companyName,
                                        UUID postId, String jobTitle, String status) {
        String text = switch (status) {
            case "VIEWED" -> companyName + " viu a tua candidatura a " + jobTitle;
            case "SHORTLISTED" -> companyName + " pré-selecionou-te para " + jobTitle;
            case "REJECTED" -> companyName + " não avançou com a tua candidatura a " + jobTitle;
            default -> null;
        };
        if (text == null) return;

        push(repository.save(build(studentId, NotificationType.APPLICATION_UPDATE, companyId, postId, text)));
    }

    public List<NotificationDTO> list(UUID userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), safeSize))
                .stream().map(this::toDto).toList();
    }

    public long unreadCount(UUID userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        repository.findById(notificationId).ifPresent(n -> {
            // só o dono pode marcar como lida
            if (n.getUserId().equals(userId) && n.getReadAt() == null) {
                n.setReadAt(LocalDateTime.now());
            }
        });
    }

    @Transactional
    public void markAllRead(UUID userId) {
        repository.markAllRead(userId, LocalDateTime.now());
    }

    @Transactional
    public void markChatRead(UUID userId, UUID otherUserId) {
        repository.markChatRead(userId, otherUserId, LocalDateTime.now());
    }

    private Notification build(UUID userId, NotificationType type, UUID actorId, UUID postId, String text) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setActorId(actorId);
        n.setPostId(postId);
        n.setText(clip(text));
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }

    private void push(Notification n) {
        messagingTemplate.convertAndSendToUser(n.getUserId().toString(), "/queue/notifications", toDto(n));
    }

    private NotificationDTO toDto(Notification n) {
        return new NotificationDTO(n.getId(), n.getType(), n.getActorId(), n.getPostId(),
                n.getRoomId(), n.getText(), n.getReadAt() != null,
                n.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }

    private String shorten(String s) {
        if (s == null) return "";
        return s.length() > 80 ? s.substring(0, 80) + "…" : s;
    }

    /** A coluna text tem 200 caracteres. */
    private String clip(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 199) + "…" : s;
    }
}
