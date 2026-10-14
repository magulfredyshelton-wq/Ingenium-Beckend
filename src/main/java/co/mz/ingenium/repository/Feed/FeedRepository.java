package co.mz.ingenium.repository.Feed;

import co.mz.ingenium.dto.Feed.FeedCandidate;
import co.mz.ingenium.dto.Feed.PostType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class FeedRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public FeedRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // The query tested in psql. It does not compute the score, it brings the raw numbers.
    private static final String CANDIDATES_SQL = """
            WITH me AS (
                SELECT u.id, s.course_id, s.seeking
                FROM usuario u
                LEFT JOIN students s ON s.id = u.id
                WHERE u.id = CAST(:userId AS uuid)
            ),
            tag_scores AS (
                SELECT pt.post_id,
                       COALESCE(SUM(ct.weight), 0)                              AS course_weight,
                       COUNT(*) FILTER (WHERE ut.tag_id IS NOT NULL)            AS followed_tags,
                       COUNT(*) FILTER (WHERE utc.tag_category_id IS NOT NULL)  AS followed_categories
                FROM post_tag pt
                JOIN tag t ON t.id = pt.tag_id
                CROSS JOIN me
                LEFT JOIN course_tag ct         ON ct.tag_id = pt.tag_id AND ct.course_id = me.course_id
                LEFT JOIN user_tag ut           ON ut.tag_id = pt.tag_id AND ut.user_id = me.id
                LEFT JOIN user_tag_category utc ON utc.tag_category_id = t.tag_category_id
                                               AND utc.user_id = me.id
                GROUP BY pt.post_id
            )
            SELECT p.id,
                   p.post_type,
                   ROUND((EXTRACT(EPOCH FROM (now() - p.created_at)) / 3600.0)::numeric, 1) AS hours_since,
                   COALESCE(ts.course_weight, 0)       AS course_weight,
                   COALESCE(ts.followed_tags, 0)       AS followed_tags,
                   COALESCE(ts.followed_categories, 0) AS followed_categories,
                   EXISTS (SELECT 1 FROM follower f
                           WHERE f.follower_id = me.id AND f.followed_id = p.author_id) AS author_followed,
                   (j.application_end - CURRENT_DATE)  AS days_left
            FROM post p
            CROSS JOIN me
            LEFT JOIN tag_scores ts ON ts.post_id = p.id
            LEFT JOIN job j         ON j.post_id = p.id
            WHERE p.deleted_at IS NULL
              AND p.author_id <> me.id
              AND (p.post_type <> 'JOB'
                   OR (j.status = 'OPEN'
                       AND CURRENT_DATE BETWEEN j.application_start AND j.application_end
                       AND (me.seeking IS NULL
                            OR me.seeking = 'BOTH'
                            OR j.work_type = 'BOTH'
                            OR j.work_type = me.seeking)))
            ORDER BY p.created_at DESC
            LIMIT 200
            """;

    /** Candidate posts for one user, with the raw numbers the FeedScorer needs. */
    public List<FeedCandidate> findCandidates(UUID userId) {
        return jdbc.query(CANDIDATES_SQL, Map.of("userId", userId.toString()), (rs, rowNum) ->
                new FeedCandidate(
                        rs.getObject("id", UUID.class),
                        PostType.valueOf(rs.getString("post_type")),
                        rs.getDouble("hours_since"),
                        rs.getDouble("course_weight"),
                        rs.getInt("followed_tags"),
                        rs.getInt("followed_categories"),
                        rs.getBoolean("author_followed"),
                        rs.getObject("days_left", Integer.class)   // stays null for posts without a deadline
                ));
    }
}