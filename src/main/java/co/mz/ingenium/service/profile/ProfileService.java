package co.mz.ingenium.service.profile;

import co.mz.ingenium.dto.Feed.FeedItemDTO;
import co.mz.ingenium.dto.profile.ContactDTO;
import co.mz.ingenium.dto.profile.ProfileDTO;
import co.mz.ingenium.dto.profile.UpdateProfileRequest;
import co.mz.ingenium.entity.UserEntity.CompanyEntity.Company;
import co.mz.ingenium.entity.UserEntity.StudentEntity.Students;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.repository.Feed.FeedItemRepository;
import co.mz.ingenium.repository.UserRepository.UserRepository;
import co.mz.ingenium.service.NotificationService.NotificationService;
import co.mz.ingenium.storage.StoredFile;
import co.mz.ingenium.storage.StoredFileRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.mz.ingenium.dto.Feed.FeedTagDTO;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ProfileService {

    private static final Pattern PHONE = Pattern.compile("^[+0-9 ()\\-]{6,20}$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern URL = Pattern.compile(
            "^https?://[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(:\\d+)?(/\\S*)?$",
            Pattern.CASE_INSENSITIVE
    );

    private final UserRepository users;
    private final StoredFileRepository files;
    private final NamedParameterJdbcTemplate jdbc;
    private final FeedItemRepository items;
    private final NotificationService notifications;

    public ProfileService(
            UserRepository users,
            StoredFileRepository files,
            NamedParameterJdbcTemplate jdbc,
            FeedItemRepository items,
            NotificationService notifications
    ) {
        this.users = users;
        this.files = files;
        this.jdbc = jdbc;
        this.items = items;
        this.notifications = notifications;
    }

    // ---------- ler ----------

    @Transactional(readOnly = true)
    public ProfileDTO get(UUID userId) {
        return get(userId, userId);
    }

    @Transactional(readOnly = true)
    public ProfileDTO get(UUID viewerId, UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));

        String type;
        String name;
        String subtitle = null;
        UUID courseId = null;
        String courseName = null;
        String fieldStudyName = null;
        String activityAreaName = null;

        if (user instanceof Students student) {
            type = "STUDENT";
            name = student.getFullName();
            if (student.getCourse() != null) {
                courseId = student.getCourse().getId();
                courseName = student.getCourse().getName();
                if (student.getCourse().getFieldStudy() != null) {
                    fieldStudyName = student.getCourse().getFieldStudy().getName();
                }
                subtitle = courseName;
            }
        } else if (user instanceof Company company) {
            type = "COMPANY";
            name = company.getCompanyName();
            if (company.getActivityArea() != null) {
                activityAreaName = company.getActivityArea().getTypeActivity();
                subtitle = activityAreaName;
            }
        } else {
            type = "USER";
            name = user.getEmail();
        }

        String avatarKey = null;
        if (user.getAvatarFileId() != null) {
            avatarKey = files.findById(user.getAvatarFileId())
                    .map(StoredFile::getStorageKey)
                    .orElse(null);
        }

        // "Sobre mim" e contactos
        String bio = null;
        ContactDTO contact = new ContactDTO(null, null, null, null, null, null);
        var extra = jdbc.query(
                """
                SELECT bio, contact_phone, contact_email, contact_website,
                       contact_linkedin, contact_github, contact_location
                FROM usuario
                WHERE id = :id
                """,
                Map.of("id", userId),
                (rs, rowNum) -> new Object[]{
                        rs.getString("bio"),
                        new ContactDTO(
                                rs.getString("contact_phone"),
                                rs.getString("contact_email"),
                                rs.getString("contact_website"),
                                rs.getString("contact_linkedin"),
                                rs.getString("contact_github"),
                                rs.getString("contact_location")
                        )
                }
        );
        if (!extra.isEmpty()) {
            bio = (String) extra.get(0)[0];
            contact = (ContactDTO) extra.get(0)[1];
        }

        boolean self = viewerId.equals(userId);

        return new ProfileDTO(
                user.getId(),
                user.getUsername(),
                type,
                // o email de login só o próprio vê
                self ? user.getEmail() : null,
                name,
                subtitle,
                avatarKey,
                courseId,
                courseName,
                fieldStudyName,
                activityAreaName,
                bio,
                contact,
                loadTags(userId),
                count("SELECT COUNT(*) FROM post WHERE author_id = :id AND deleted_at IS NULL", userId),
                count("SELECT COUNT(*) FROM follower WHERE followed_id = :id", userId),
                count("SELECT COUNT(*) FROM follower WHERE follower_id = :id", userId),
                !self && isFollowing(viewerId, userId)
        );
    }

    // ---------- editar "Sobre mim" e contactos ----------

    @Transactional
    public ProfileDTO update(UUID userId, UpdateProfileRequest req) {
        String bio = clean(req.bio(), 500, "O texto \"Sobre mim\"");
        String phone = cleanPhone(req.phone());
        String email = cleanEmail(req.contactEmail());
        String website = cleanUrl(req.website(), "O site");
        String linkedin = cleanUrl(req.linkedin(), "O LinkedIn");
        String github = cleanUrl(req.github(), "O GitHub");
        String location = clean(req.location(), 100, "A localização");

        MapSqlParameterSource params = new MapSqlParameterSource("id", userId)
                .addValue("bio", bio, Types.VARCHAR)
                .addValue("phone", phone, Types.VARCHAR)
                .addValue("email", email, Types.VARCHAR)
                .addValue("website", website, Types.VARCHAR)
                .addValue("linkedin", linkedin, Types.VARCHAR)
                .addValue("github", github, Types.VARCHAR)
                .addValue("location", location, Types.VARCHAR);

        jdbc.update(
                """
                UPDATE usuario
                SET bio = :bio,
                    contact_phone = :phone,
                    contact_email = :email,
                    contact_website = :website,
                    contact_linkedin = :linkedin,
                    contact_github = :github,
                    contact_location = :location
                WHERE id = :id
                """,
                params
        );

        return get(userId);
    }

    private static String clean(String value, int max, String label) {
        if (value == null) {
            return null;
        }
        String text = value.strip();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > max) {
            throw new IllegalArgumentException(label + " é demasiado longo (máximo " + max + " caracteres).");
        }
        return text;
    }

    private static String cleanPhone(String value) {
        String text = clean(value, 20, "O telefone");
        if (text != null && !PHONE.matcher(text).matches()) {
            throw new IllegalArgumentException("O telefone não é válido. Usa só números, espaços, + e -.");
        }
        return text;
    }

    private static String cleanEmail(String value) {
        String text = clean(value, 120, "O email de contacto");
        if (text != null && !EMAIL.matcher(text).matches()) {
            throw new IllegalArgumentException("O email de contacto não é válido.");
        }
        return text;
    }

    private static String cleanUrl(String value, String label) {
        String text = clean(value, 200, label);
        if (text == null) {
            return null;
        }
        if (!text.regionMatches(true, 0, "http://", 0, 7)
                && !text.regionMatches(true, 0, "https://", 0, 8)) {
            text = "https://" + text;
        }
        if (text.length() > 200 || !URL.matcher(text).matches()) {
            throw new IllegalArgumentException(label + " não é um endereço válido.");
        }
        return text;
    }
    private List<FeedTagDTO> loadTags(UUID userId) {
        return jdbc.query(
                """
                SELECT t.slug, t.name
                FROM user_tag ut
                JOIN tag t ON t.id = ut.tag_id
                WHERE ut.user_id = :id
                ORDER BY t.name
                """,
                Map.of("id", userId),
                (rs, n) -> new FeedTagDTO(rs.getString("slug"), rs.getString("name"))
        );
    }

    @Transactional
    public ProfileDTO updateTags(UUID userId, List<String> slugs) {
        List<String> clean = slugs == null ? List.of() : slugs.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (clean.size() > 10) {
            throw new IllegalArgumentException("Podes ter no máximo 10 hashtags.");
        }

        jdbc.update("DELETE FROM user_tag WHERE user_id = :id", Map.of("id", userId));

        if (!clean.isEmpty()) {
            jdbc.update(
                    """
                    INSERT INTO user_tag (user_id, tag_id)
                    SELECT :id, t.id FROM tag t
                    WHERE t.slug IN (:slugs) AND t.active = true
                    """,
                    Map.of("id", userId, "slugs", clean)
            );
        }

        return get(userId);
    }
    // ---------- seguir ----------

    @Transactional
    public void follow(UUID viewerId, UUID targetId) {
        if (viewerId.equals(targetId)) {
            throw new IllegalArgumentException("Não podes seguir-te a ti próprio.");
        }

        User target = users.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));

        int inserted = jdbc.update(
                """
                INSERT INTO follower (follower_id, followed_id)
                VALUES (:viewer, :target)
                ON CONFLICT (follower_id, followed_id) DO NOTHING
                """,
                Map.of("viewer", viewerId, "target", targetId)
        );

        if (inserted > 0) {
            String name = users.findById(viewerId)
                    .map(User::getDisplayName)
                    .orElse("Alguém");
            notifications.notifyFollow(target.getId(), viewerId, name);
        }
    }

    @Transactional
    public void unfollow(UUID viewerId, UUID targetId) {
        jdbc.update(
                """
                DELETE FROM follower
                WHERE follower_id = :viewer
                  AND followed_id = :target
                """,
                Map.of("viewer", viewerId, "target", targetId)
        );
    }

    // ---------- publicações da pessoa ----------

    @Transactional(readOnly = true)
    public List<FeedItemDTO> posts(UUID viewerId, UUID authorId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        List<UUID> ids = jdbc.query(
                """
                SELECT id
                FROM post
                WHERE author_id = :author
                  AND deleted_at IS NULL
                ORDER BY created_at DESC
                LIMIT :limit OFFSET :offset
                """,
                Map.of(
                        "author", authorId,
                        "limit", safeSize,
                        "offset", safePage * safeSize
                ),
                (rs, rowNum) -> rs.getObject("id", UUID.class)
        );

        Map<UUID, FeedItemDTO> details = items.findItems(ids, viewerId);

        return ids.stream()
                .map(details::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    // ---------- auxiliares ----------

    private boolean isFollowing(UUID viewerId, UUID targetId) {
        return count(
                """
                SELECT COUNT(*)
                FROM follower
                WHERE follower_id = :viewer
                  AND followed_id = :target
                """,
                Map.of("viewer", viewerId, "target", targetId)
        ) > 0;
    }

    private long count(String sql, Map<String, Object> params) {
        Number value = jdbc.queryForObject(sql, params, Number.class);
        return value == null ? 0 : value.longValue();
    }

    private long count(String sql, UUID id) {
        Number value = jdbc.queryForObject(sql, Map.of("id", id), Number.class);
        return value == null ? 0 : value.longValue();
    }
}