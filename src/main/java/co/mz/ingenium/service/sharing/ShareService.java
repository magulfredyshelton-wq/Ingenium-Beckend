package co.mz.ingenium.service.sharing;

import co.mz.ingenium.dto.Notification.NotificationDTO;
import co.mz.ingenium.dto.sharing.ShareDtos;
import co.mz.ingenium.entity.NotificationEntity.Notification;
import co.mz.ingenium.entity.NotificationEntity.NotificationType;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.repository.NotificationRepository.NotificationRepository;
import co.mz.ingenium.repository.UserRepository.UserRepository;
import co.mz.ingenium.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ShareService {

    private static final Logger log =
            LoggerFactory.getLogger(ShareService.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final UserRepository users;
    private final NotificationRepository notifications;
    private final SimpMessagingTemplate socket;
    private final StorageService storage;

    public ShareService(
            NamedParameterJdbcTemplate jdbc,
            UserRepository users,
            NotificationRepository notifications,
            SimpMessagingTemplate socket,
            StorageService storage) {
        this.jdbc = jdbc;
        this.users = users;
        this.notifications = notifications;
        this.socket = socket;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public ShareDtos.PeoplePage following(
            UUID me,
            String query,
            int page) {

        int safePage = Math.min(Math.max(page, 0), 100_000);
        String term = query == null ? "" : query.strip();

        if (term.length() > 100) {
            throw new IllegalArgumentException(
                    "Pesquisa demasiado longa.");
        }

        List<ShareDtos.Person> found = jdbc.query("""
                SELECT u.id, u.username,
                       coalesce(s.full_name, c.company_name, u.username) AS name,
                       f.storage_key
                FROM follower following
                JOIN usuario u ON u.id = following.followed_id
                LEFT JOIN students s ON s.id = u.id
                LEFT JOIN company c ON c.id = u.id
                LEFT JOIN file f
                    ON f.id = u.avatar_file_id
                   AND f.visibility = 'PUBLIC'
                WHERE following.follower_id = :me
                  AND (
                      :q = ''
                      OR position(lower(:q) IN lower(
                          coalesce(s.full_name, c.company_name, '')
                          || ' ' || u.username
                      )) > 0
                  )
                ORDER BY name, u.id
                LIMIT 21 OFFSET :offset
                """, Map.of(
                        "me", me,
                        "q", term,
                        "offset", safePage * 20),
                (rs, row) -> new ShareDtos.Person(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("storage_key") == null
                                ? null
                                : storage.publicUrl(
                                rs.getString("storage_key"))));

        return new ShareDtos.PeoplePage(
                found.stream().limit(20).toList(),
                found.size() > 20);
    }

    @Transactional
    public ShareDtos.Result share(
            UUID me,
            UUID postId,
            ShareDtos.Request request) {

        List<UUID> recipients = request.recipientIds()
                .stream().distinct().toList();

        if (recipients.isEmpty()
                || recipients.size() > 20
                || recipients.contains(me)) {
            throw new IllegalArgumentException(
                    "Escolhe entre 1 e 20 pessoas que segues.");
        }

        List<UUID> posts = jdbc.query("""
                SELECT id
                FROM post
                WHERE id = :post AND deleted_at IS NULL
                FOR SHARE
                """, Map.of("post", postId),
                (rs, row) -> rs.getObject("id", UUID.class));

        if (posts.isEmpty()) {
            throw new IllegalArgumentException(
                    "Publicação não encontrada.");
        }

        List<UUID> allowed = jdbc.query("""
                SELECT followed_id
                FROM follower
                WHERE follower_id = :me
                  AND followed_id IN (:ids)
                FOR SHARE
                """, Map.of("me", me, "ids", recipients),
                (rs, row) -> rs.getObject(
                        "followed_id", UUID.class));

        if (allowed.size() != recipients.size()) {
            throw new IllegalArgumentException(
                    "Só podes partilhar com pessoas que segues.");
        }

        String name = users.findById(me)
                .map(User::getDisplayName)
                .orElse("Alguém");

        String text = name + " partilhou uma publicação contigo";
        if (text.length() > 200) {
            text = text.substring(0, 199) + "…";
        }

        List<NotificationDTO> events = new ArrayList<>();

        for (UUID recipient : recipients) {
            jdbc.update("""
                    INSERT INTO share
                        (sender_id, recipient_id, post_id, channel)
                    VALUES (:me, :recipient, :post, 'DIRECT')
                    """, Map.of(
                    "me", me,
                    "recipient", recipient,
                    "post", postId));

            Notification n = new Notification();
            n.setUserId(recipient);
            n.setActorId(me);
            n.setPostId(postId);
            n.setType(NotificationType.POST);
            n.setText(text);
            n.setCreatedAt(LocalDateTime.now());
            notifications.save(n);

            events.add(new NotificationDTO(
                    n.getId(),
                    n.getType(),
                    me,
                    postId,
                    null,
                    text,
                    false,
                    n.getCreatedAt()
                            .atZone(ZoneId.systemDefault())
                            .toOffsetDateTime()));
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (int i = 0; i < recipients.size(); i++) {
                            try {
                                socket.convertAndSendToUser(
                                        recipients.get(i).toString(),
                                        "/queue/notifications",
                                        events.get(i));
                            } catch (RuntimeException failure) {
                                log.warn(
                                        "Partilha gravada; aviso em tempo real "
                                                + "indisponível.", failure);
                            }
                        }
                    }
                });

        return new ShareDtos.Result(recipients.size());
    }
}