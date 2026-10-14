package co.mz.ingenium.service.Feed;

import co.mz.ingenium.dto.Feed.CreatePostRequest;
import co.mz.ingenium.dto.Feed.FeedCommentDTO;
import co.mz.ingenium.dto.Feed.FeedTagDTO;
import co.mz.ingenium.dto.Feed.PostCreatedDTO;
import co.mz.ingenium.dto.Feed.PostType;
import co.mz.ingenium.entity.UserEntity.CompanyEntity.Company;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.repository.UserRepository.UserRepository;
import co.mz.ingenium.service.NotificationService.NotificationService;
import co.mz.ingenium.storage.FileVisibility;
import co.mz.ingenium.storage.StorageService;
import co.mz.ingenium.storage.StoredFile;
import co.mz.ingenium.storage.StoredFileRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FeedInteractionService {

    private static final long MAX_IMAGE_BYTES =
            5L * 1024 * 1024;

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final NamedParameterJdbcTemplate jdbc;
    private final UserRepository users;
    private final StoredFileRepository files;
    private final StorageService storage;
    private final NotificationService notifications;

    public FeedInteractionService(
            NamedParameterJdbcTemplate jdbc,
            UserRepository users,
            StoredFileRepository files,
            StorageService storage,
            NotificationService notifications
    ) {
        this.jdbc = jdbc;
        this.users = users;
        this.files = files;
        this.storage = storage;
        this.notifications = notifications;
    }

    @Transactional
    public PostCreatedDTO create(
            UUID authorId,
            CreatePostRequest request
    ) {
        if (
                request.type() == PostType.JOB &&
                        users.findById(authorId)
                                .filter(Company.class::isInstance)
                                .isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "Apenas empresas podem publicar vagas."
            );
        }

        if (
                request.type() != PostType.GENERAL &&
                        blank(request.title())
        ) {
            throw new IllegalArgumentException(
                    "O título é obrigatório."
            );
        }

        if (
                request.type() == PostType.JOB &&
                        request.applicationEnd() == null
        ) {
            throw new IllegalArgumentException(
                    "A data final da candidatura é obrigatória."
            );
        }

        UUID postId = UUID.randomUUID();

        jdbc.update(
                """
                INSERT INTO post (id, author_id, post_type)
                VALUES (:id, :authorId, :postType)
                """,
                new MapSqlParameterSource()
                        .addValue("id", postId)
                        .addValue("authorId", authorId)
                        .addValue(
                                "postType",
                                request.type().name()
                        )
        );

        switch (request.type()) {
            case GENERAL -> jdbc.update(
                    """
                    INSERT INTO general_post (post_id, content, title)
                    VALUES (:postId, :content, :title)
                    """,
                    new MapSqlParameterSource()
                            .addValue("postId", postId)
                            .addValue("content", request.content().trim())
                            .addValue("title",
                                    blank(request.title()) ? null : request.title().trim(),
                                    java.sql.Types.VARCHAR)
            );

            case PROJECT -> jdbc.update(
                    """
                    INSERT INTO project (
                        post_id,
                        title,
                        description,
                        repository_url
                    )
                    VALUES (
                        :postId,
                        :title,
                        :description,
                        :repositoryUrl
                    )
                    """,
                    new MapSqlParameterSource()
                            .addValue("postId", postId)
                            .addValue(
                                    "title",
                                    request.title().trim()
                            )
                            .addValue(
                                    "description",
                                    request.content().trim()
                            )
                            .addValue(
                                    "repositoryUrl",
                                    request.repositoryUrl()
                            )
            );

            case JOB -> createJob(
                    authorId,
                    postId,
                    request
            );
        }

        attachTags(postId, request.tags());

        notifications.notifyFollowersOfNewPost(
                authorId,
                displayName(authorId),
                postId
        );

        return new PostCreatedDTO(postId);
    }

    private void createJob(
            UUID companyId,
            UUID postId,
            CreatePostRequest request
    ) {
        LocalDate start =
                request.applicationStart() == null
                        ? LocalDate.now()
                        : request.applicationStart();

        String workType =
                normalizeWorkType(request.workType());

        if ("INTERNSHIP".equals(workType) && request.endDate() == null) {
            throw new IllegalArgumentException(
                    "Um estágio precisa de data de fim."
            );
        }

        if (request.applicationEnd().isBefore(start)) {
            throw new IllegalArgumentException(
                    "O fim das candidaturas não pode ser antes do início."
            );
        }

        if (
                request.startDate() != null &&
                        request.endDate() != null &&
                        request.endDate().isBefore(request.startDate())
        ) {
            throw new IllegalArgumentException(
                    "A data de fim não pode ser antes da data de início."
            );
        }

        jdbc.update(
                """
                INSERT INTO job (
                    post_id,
                    company_id,
                    title,
                    description,
                    work_type,
                    application_start,
                    application_end,
                    start_date,
                    end_date
                )
                VALUES (
                    :postId,
                    :companyId,
                    :title,
                    :description,
                    :workType,
                    :applicationStart,
                    :applicationEnd,
                    :startDate,
                    :endDate
                )
                """,
                new MapSqlParameterSource()
                        .addValue("postId", postId)
                        .addValue("companyId", companyId)
                        .addValue(
                                "title",
                                request.title().trim()
                        )
                        .addValue(
                                "description",
                                request.content().trim()
                        )
                        .addValue("workType", workType)
                        .addValue(
                                "applicationStart",
                                start
                        )
                        .addValue(
                                "applicationEnd",
                                request.applicationEnd()
                        )
                        .addValue(
                                "startDate",
                                request.startDate()
                        )
                        .addValue(
                                "endDate",
                                request.endDate()
                        )
        );
    }

    @Transactional
    public void uploadMedia(
            UUID authorId,
            UUID postId,
            MultipartFile upload
    ) {
        assertPostOwner(authorId, postId);

        if (upload == null || upload.isEmpty()) {
            throw new IllegalArgumentException(
                    "Envia uma imagem."
            );
        }

        if (upload.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException(
                    "A imagem deve ter no máximo 5 MB."
            );
        }

        String contentType =
                normalizeContentType(upload.getContentType());

        if (!IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Só são aceites imagens JPG, PNG ou WEBP."
            );
        }

        byte[] bytes;

        try {
            bytes = upload.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String extension =
                contentType.substring(
                        contentType.indexOf('/') + 1
                );

        String key =
                "posts/" +
                        postId +
                        "/" +
                        UUID.randomUUID() +
                        "." +
                        extension;

        storage.putPublic(
                key,
                bytes,
                contentType
        );

        StoredFile file = new StoredFile();
        file.setOwnerId(authorId);
        file.setStorageKey(key);
        file.setOriginalName(upload.getOriginalFilename());
        file.setContentType(contentType);
        file.setSizeBytes(bytes.length);
        file.setVisibility(FileVisibility.PUBLIC);

        StoredFile saved = files.saveAndFlush(file);

        jdbc.update(
                """
                INSERT INTO post_media (
                    post_id,
                    file_id,
                    position
                )
                SELECT
                    :postId,
                    :fileId,
                    COALESCE(MAX(position), -1) + 1
                FROM post_media
                WHERE post_id = :postId
                """,
                Map.of(
                        "postId",
                        postId,
                        "fileId",
                        saved.getId()
                )
        );
    }
    @Transactional
    public void delete(UUID userId, UUID postId) {
        int rows = jdbc.update(
                """
                UPDATE post
                SET deleted_at = now()
                WHERE id = :postId
                  AND author_id = :userId
                  AND deleted_at IS NULL
                """,
                Map.of("postId", postId, "userId", userId)
        );

        if (rows == 0) {
            throw new IllegalArgumentException("Publicação não encontrada.");
        }

        // os avisos que apontavam para este post deixam de fazer sentido
        jdbc.update(
                "DELETE FROM notification WHERE post_id = :postId",
                Map.of("postId", postId)
        );
    }

    @Transactional
    public void like(UUID userId, UUID postId) {
        assertPostExists(postId);

        int inserted = jdbc.update(
                """
                INSERT INTO post_like (
                    user_id,
                    post_id
                )
                VALUES (
                    :userId,
                    :postId
                )
                ON CONFLICT (
                    user_id,
                    post_id
                ) DO NOTHING
                """,
                Map.of(
                        "userId",
                        userId,
                        "postId",
                        postId
                )
        );

        if (inserted > 0) {
            notifications.notifyLike(
                    postAuthor(postId),
                    userId,
                    displayName(userId),
                    postId
            );
        }
    }

    @Transactional
    public void unlike(UUID userId, UUID postId) {
        jdbc.update(
                """
                DELETE FROM post_like
                WHERE user_id = :userId
                  AND post_id = :postId
                """,
                Map.of(
                        "userId",
                        userId,
                        "postId",
                        postId
                )
        );
    }

    @Transactional(readOnly = true)
    public List<FeedCommentDTO> comments(UUID postId) {
        assertPostExists(postId);

        return jdbc.query(
                """
                SELECT
                    cm.id,
                    cm.content,
                    cm.created_at,
                    cm.author_id,
                    COALESCE(
                        s.full_name,
                        c.company_name
                    ) AS author_name,
                    af.storage_key AS avatar_key
                FROM comment cm
                JOIN usuario u
                    ON u.id = cm.author_id
                LEFT JOIN students s
                    ON s.id = u.id
                LEFT JOIN company c
                    ON c.id = u.id
                LEFT JOIN file af
                    ON af.id = u.avatar_file_id
                WHERE cm.post_id = :postId
                ORDER BY cm.created_at ASC
                """,
                Map.of("postId", postId),
                (rs, rowNum) ->
                        new FeedCommentDTO(
                                rs.getObject(
                                        "id",
                                        UUID.class
                                ),
                                new FeedCommentDTO.Author(
                                        rs.getObject(
                                                "author_id",
                                                UUID.class
                                        ),
                                        rs.getString(
                                                "author_name"
                                        ),
                                        rs.getString(
                                                "avatar_key"
                                        )
                                ),
                                rs.getString("content"),
                                rs.getObject(
                                        "created_at",
                                        OffsetDateTime.class
                                )
                        )
        );
    }

    @Transactional
    public FeedCommentDTO addComment(
            UUID authorId,
            UUID postId,
            String content
    ) {
        assertPostExists(postId);

        if (
                blank(content) ||
                        content.trim().length() > 1000
        ) {
            throw new IllegalArgumentException(
                    "O comentário deve ter entre 1 e 1000 caracteres."
            );
        }

        UUID id = UUID.randomUUID();

        jdbc.update(
                """
                INSERT INTO comment (
                    id,
                    post_id,
                    author_id,
                    content
                )
                VALUES (
                    :id,
                    :postId,
                    :authorId,
                    :content
                )
                """,
                Map.of(
                        "id",
                        id,
                        "postId",
                        postId,
                        "authorId",
                        authorId,
                        "content",
                        content.trim()
                )
        );

        notifications.notifyComment(
                postAuthor(postId),
                authorId,
                displayName(authorId),
                postId,
                content.trim()
        );

        return comments(postId)
                .stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Comentário não encontrado após a criação."
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<FeedTagDTO> tags() {
        return jdbc.query(
                """
                SELECT slug, name
                FROM tag
                WHERE active = true
                ORDER BY name
                LIMIT 200
                """,
                Map.of(),
                (rs, rowNum) ->
                        new FeedTagDTO(
                                rs.getString("slug"),
                                rs.getString("name")
                        )
        );
    }

    private void attachTags(
            UUID postId,
            List<String> requested
    ) {
        List<String> tags = normalizeTags(requested);

        if (tags.isEmpty()) {
            return;
        }

        List<String> found = jdbc.query(
                """
                SELECT slug
                FROM tag
                WHERE active = true
                  AND slug IN (:tags)
                """,
                Map.of("tags", tags),
                (rs, rowNum) ->
                        rs.getString("slug")
        );

        if (found.size() != tags.size()) {
            throw new IllegalArgumentException(
                    "Uma ou mais tags selecionadas não existem."
            );
        }

        for (String tag : tags) {
            jdbc.update(
                    """
                    INSERT INTO post_tag (
                        post_id,
                        tag_id
                    )
                    SELECT
                        :postId,
                        id
                    FROM tag
                    WHERE slug = :slug
                    """,
                    Map.of(
                            "postId",
                            postId,
                            "slug",
                            tag
                    )
            );
        }
    }

    private void assertPostOwner(
            UUID authorId,
            UUID postId
    ) {
        UUID owner = jdbc.query(
                """
                SELECT author_id
                FROM post
                WHERE id = :postId
                  AND deleted_at IS NULL
                """,
                Map.of("postId", postId),
                rs -> rs.next()
                        ? rs.getObject(
                        "author_id",
                        UUID.class
                )
                        : null
        );

        if (!authorId.equals(owner)) {
            throw new IllegalArgumentException(
                    "Publicação não encontrada."
            );
        }
    }

    private UUID postAuthor(UUID postId) {
        return jdbc.query(
                """
                SELECT author_id
                FROM post
                WHERE id = :postId
                """,
                Map.of("postId", postId),
                rs -> rs.next()
                        ? rs.getObject("author_id", UUID.class)
                        : null
        );
    }

    private String displayName(UUID userId) {
        return users.findById(userId)
                .map(User::getDisplayName)
                .orElse("Alguém");
    }

    private void assertPostExists(UUID postId) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM post
                WHERE id = :postId
                  AND deleted_at IS NULL
                """,
                Map.of("postId", postId),
                Integer.class
        );

        if (count == null || count == 0) {
            throw new IllegalArgumentException(
                    "Publicação não encontrada."
            );
        }
    }

    private static List<String> normalizeTags(
            List<String> source
    ) {
        if (source == null) {
            return List.of();
        }

        LinkedHashSet<String> result =
                new LinkedHashSet<>();

        for (String value : source) {
            if (value == null || value.isBlank()) {
                continue;
            }

            result.add(slug(value));

            if (result.size() == 5) {
                break;
            }
        }

        return new ArrayList<>(result);
    }

    private static String slug(String value) {
        String noAccent =
                Normalizer.normalize(
                        value.trim().toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFD
                ).replaceAll("\\p{M}", "");

        return noAccent
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private static String normalizeWorkType(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return "BOTH";
        }

        String normalized =
                value.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "INTERNSHIP", "ESTAGIO", "ESTÁGIO" ->
                    "INTERNSHIP";
            case "JOB", "EMPREGO" ->
                    "JOB";
            case "BOTH", "AMBOS" ->
                    "BOTH";
            default ->
                    throw new IllegalArgumentException(
                            "Tipo de vaga inválido."
                    );
        };
    }

    private static String normalizeContentType(
            String value
    ) {
        if (value == null) {
            return "";
        }

        int separator = value.indexOf(';');

        return (
                separator >= 0
                        ? value.substring(0, separator)
                        : value
        )
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}