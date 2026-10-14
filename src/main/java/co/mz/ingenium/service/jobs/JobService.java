package co.mz.ingenium.service.jobs;

import co.mz.ingenium.dto.jobs.JobDTO;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JobService {

    private final NamedParameterJdbcTemplate jdbc;

    public JobService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<JobDTO> search(
            UUID userId,
            String query,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        String term = query == null
                ? ""
                : query.trim().toLowerCase();

        return jdbc.query(
                """
                SELECT p.id AS post_id,
                       j.title,
                       j.description,
                       c.id AS company_id,
                       c.company_name,
                       j.work_type,
                       j.application_end,
                       (j.application_end - CURRENT_DATE) AS days_left,
                       EXISTS (
                           SELECT 1 FROM application ap
                           WHERE ap.job_post_id = p.id
                             AND ap.student_id = CAST(:userId AS uuid)
                       ) AS applied_by_me,
                       COALESCE(
                           string_agg(t.name, ',' ORDER BY t.name),
                           ''
                       ) AS tag_names
                FROM job j
                JOIN post p ON p.id = j.post_id
                JOIN company c ON c.id = j.company_id
                LEFT JOIN post_tag pt ON pt.post_id = p.id
                LEFT JOIN tag t ON t.id = pt.tag_id
                WHERE p.deleted_at IS NULL
                  AND j.status = 'OPEN'
                  AND CURRENT_DATE BETWEEN
                      j.application_start AND j.application_end
                  AND (
                      :query = ''
                      OR lower(j.title) LIKE :likeQuery
                      OR lower(j.description) LIKE :likeQuery
                      OR lower(c.company_name) LIKE :likeQuery
                      OR lower(COALESCE(t.name, ''))
                         LIKE :likeQuery
                  )
                GROUP BY p.id,
                         j.title,
                         j.description,
                         c.id,
                         c.company_name,
                         j.work_type,
                         j.application_end
                ORDER BY j.application_end ASC,
                         p.created_at DESC
                LIMIT :limit OFFSET :offset
                """,
                Map.of(
                        "userId", userId.toString(),
                        "query", term,
                        "likeQuery", "%" + term + "%",
                        "limit", safeSize,
                        "offset", safePage * safeSize
                ),
                (rs, rowNum) -> new JobDTO(
                        rs.getObject("post_id", UUID.class),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getObject("company_id", UUID.class),
                        rs.getString("company_name"),
                        rs.getString("work_type"),
                        rs.getObject("application_end", LocalDate.class),
                        rs.getObject("days_left", Integer.class),
                        rs.getBoolean("applied_by_me"),
                        splitTags(rs.getString("tag_names"))
                )
        );
    }

    private static List<String> splitTags(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return List.of(value.split(","));
    }
}