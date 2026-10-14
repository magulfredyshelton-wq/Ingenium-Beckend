package co.mz.ingenium.repository.Feed;

import co.mz.ingenium.dto.Feed.FeedItemDTO;
import co.mz.ingenium.dto.Feed.PostType;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Second query of the feed: runs only for the ~20 posts of the requested page
 * and brings what is needed to draw each card.
 */
@Repository
public class FeedItemRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public FeedItemRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Post + author + the fields of its subtype + counters.
    private static final String BASE_SQL = """
            SELECT p.id, p.post_type, p.created_at,
                   u.id AS author_id, u.user_type,
                   COALESCE(s.full_name, c.company_name)                AS author_name,
                   COALESCE(cr.name, aa.type_activity)                  AS author_subtitle,
                   af.storage_key                                       AS avatar_key,
                   COALESCE(j.title, pr.title, gp.title)                          AS title,
                   COALESCE(j.description, pr.description, gp.content)  AS content,
                   j.work_type,
                   j.application_end,
                   (j.application_end - CURRENT_DATE)                   AS days_left,
                   (SELECT count(*) FROM post_like l WHERE l.post_id = p.id)  AS likes_count,
                   (SELECT count(*) FROM comment cm WHERE cm.post_id = p.id)  AS comments_count,
                   EXISTS (SELECT 1 FROM post_like l2
                           WHERE l2.post_id = p.id
                                                          AND l2.user_id = CAST(:userId AS uuid))          AS liked_by_me,
                                                EXISTS (SELECT 1 FROM application ap
                                                        WHERE ap.job_post_id = p.id
                                                          AND ap.student_id = CAST(:userId AS uuid))       AS applied_by_me,
                                                CASE WHEN p.author_id = CAST(:userId AS uuid)
                                                     THEN (SELECT count(*) FROM application ap2 WHERE ap2.job_post_id = p.id)
                                                     ELSE 0 END                                            AS applications_count
            FROM post p
            JOIN usuario u             ON u.id = p.author_id
            LEFT JOIN students s       ON s.id = u.id
            LEFT JOIN course cr        ON cr.id = s.course_id
            LEFT JOIN company c        ON c.id = u.id
            LEFT JOIN activity_area aa ON aa.id = c.activity_area_id
            LEFT JOIN file af          ON af.id = u.avatar_file_id
            LEFT JOIN job j            ON j.post_id = p.id
            LEFT JOIN project pr       ON pr.post_id = p.id
            LEFT JOIN general_post gp  ON gp.post_id = p.id
            WHERE p.id IN (:ids)
              AND p.deleted_at IS NULL
            """;

    private static final String TAGS_SQL = """
            SELECT pt.post_id, t.name
            FROM post_tag pt
            JOIN tag t ON t.id = pt.tag_id
            WHERE pt.post_id IN (:ids)
            ORDER BY pt.post_id, t.name
            """;

    private static final String MEDIA_SQL = """
            SELECT pm.post_id, f.storage_key, f.content_type
            FROM post_media pm
            JOIN file f ON f.id = pm.file_id
            WHERE pm.post_id IN (:ids)
            ORDER BY pm.post_id, pm.position
            """;

    /** Cards for the given posts, by post id. The score is filled in later by the service. */
    public Map<UUID, FeedItemDTO> findItems(List<UUID> postIds, UUID userId) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<String>> tags = loadTags(postIds);
        Map<UUID, List<FeedItemDTO.Media>> media = loadMedia(postIds);

        Map<String, Object> params = Map.of("ids", postIds, "userId", userId.toString());

        List<FeedItemDTO> items = jdbc.query(BASE_SQL, params, (rs, rowNum) -> {
            UUID postId = rs.getObject("id", UUID.class);
            PostType type = PostType.valueOf(rs.getString("post_type"));

            var author = new FeedItemDTO.Author(
                    rs.getObject("author_id", UUID.class),
                    rs.getString("author_name"),
                    rs.getString("author_subtitle"),
                    rs.getString("avatar_key"),
                    rs.getString("user_type"));

            FeedItemDTO.JobInfo job = null;
            if (type == PostType.JOB) {
                job = new FeedItemDTO.JobInfo(
                        rs.getString("work_type"),
                        rs.getObject("application_end", LocalDate.class),
                        rs.getObject("days_left", Integer.class),
                        rs.getBoolean("applied_by_me"),
                        rs.getLong("applications_count"));
            }

            return new FeedItemDTO(
                    postId,
                    type,
                    rs.getObject("created_at", OffsetDateTime.class),
                    author,
                    rs.getString("title"),
                    rs.getString("content"),
                    tags.getOrDefault(postId, List.of()),
                    media.getOrDefault(postId, List.of()),
                    rs.getLong("likes_count"),
                    rs.getLong("comments_count"),
                    rs.getBoolean("liked_by_me"),
                    job,
                    0.0);
        });

        return items.stream().collect(Collectors.toMap(FeedItemDTO::postId, Function.identity()));
    }

    private Map<UUID, List<String>> loadTags(List<UUID> postIds) {
        Map<UUID, List<String>> result = new HashMap<>();
        jdbc.query(TAGS_SQL, Map.of("ids", postIds), (RowCallbackHandler) rs ->
                result.computeIfAbsent(rs.getObject("post_id", UUID.class), k -> new ArrayList<>())
                        .add(rs.getString("name")));
        return result;
    }

    private Map<UUID, List<FeedItemDTO.Media>> loadMedia(List<UUID> postIds) {
        Map<UUID, List<FeedItemDTO.Media>> result = new HashMap<>();
        jdbc.query(MEDIA_SQL, Map.of("ids", postIds), (RowCallbackHandler) rs ->
                result.computeIfAbsent(rs.getObject("post_id", UUID.class), k -> new ArrayList<>())
                        .add(new FeedItemDTO.Media(rs.getString("storage_key"), rs.getString("content_type"))));
        return result;
    }
}