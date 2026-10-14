package co.mz.ingenium.service.application;

import co.mz.ingenium.dto.application.ApplicationDtos.Candidate;
import co.mz.ingenium.dto.application.ApplicationDtos.Mine;
import co.mz.ingenium.dto.application.ApplicationDtos.MyJob;
import co.mz.ingenium.entity.UserEntity.StudentEntity.Students;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.repository.UserRepository.UserRepository;
import co.mz.ingenium.service.NotificationService.NotificationService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ApplicationService {

    private static final Set<String> COMPANY_STATUSES = Set.of("VIEWED", "SHORTLISTED", "REJECTED");

    private final NamedParameterJdbcTemplate jdbc;
    private final UserRepository users;
    private final NotificationService notifications;

    public ApplicationService(NamedParameterJdbcTemplate jdbc,
                              UserRepository users,
                              NotificationService notifications) {
        this.jdbc = jdbc;
        this.users = users;
        this.notifications = notifications;
    }

    private record JobRow(UUID companyId, String title, boolean open) {}

    // ---------- estudante ----------

    @Transactional
    public void apply(UUID studentId, UUID postId, String message) {
        User user = users.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));

        if (!(user instanceof Students student)) {
            throw new IllegalArgumentException("Só estudantes se podem candidatar a vagas.");
        }

        JobRow job = findJob(postId);
        if (!job.open()) {
            throw new IllegalArgumentException("Esta vaga já não aceita candidaturas.");
        }

        String note = message == null ? null : message.strip();
        if (note != null && note.isEmpty()) {
            note = null;
        }
        if (note != null && note.length() > 1000) {
            throw new IllegalArgumentException("A mensagem deve ter no máximo 1000 caracteres.");
        }

        int inserted = jdbc.update(
                """
                INSERT INTO application (job_post_id, student_id, message)
                VALUES (:post, :student, :msg)
                ON CONFLICT (job_post_id, student_id) DO NOTHING
                """,
                new MapSqlParameterSource()
                        .addValue("post", postId)
                        .addValue("student", studentId)
                        .addValue("msg", note, Types.VARCHAR)
        );

        if (inserted == 0) {
            throw new IllegalArgumentException("Já te candidataste a esta vaga.");
        }

        notifications.notifyApplication(
                job.companyId(), studentId, student.getFullName(), postId, job.title());
    }

    @Transactional
    public void withdraw(UUID studentId, UUID postId) {
        jdbc.update(
                "DELETE FROM application WHERE job_post_id = :post AND student_id = :student",
                Map.of("post", postId, "student", studentId)
        );
    }

    @Transactional(readOnly = true)
    public List<Mine> mine(UUID studentId) {
        return jdbc.query(
                """
                SELECT a.id, a.job_post_id, j.title, j.company_id, c.company_name, j.work_type,
                       (j.status = 'OPEN'
                        AND CURRENT_DATE BETWEEN j.application_start AND j.application_end) AS job_open,
                       a.status, a.created_at
                FROM application a
                JOIN job j     ON j.post_id = a.job_post_id
                JOIN post p    ON p.id = j.post_id
                JOIN company c ON c.id = j.company_id
                WHERE a.student_id = :student
                  AND p.deleted_at IS NULL
                ORDER BY a.created_at DESC
                """,
                Map.of("student", studentId),
                (rs, n) -> new Mine(
                        rs.getObject("id", UUID.class),
                        rs.getObject("job_post_id", UUID.class),
                        rs.getString("title"),
                        rs.getObject("company_id", UUID.class),
                        rs.getString("company_name"),
                        rs.getString("work_type"),
                        rs.getBoolean("job_open"),
                        rs.getString("status"),
                        rs.getObject("created_at", OffsetDateTime.class)
                )
        );
    }

    // ---------- empresa ----------

    @Transactional(readOnly = true)
    public List<MyJob> myJobs(UUID companyId) {
        return jdbc.query(
                """
                SELECT j.post_id, j.title, j.work_type, j.application_end,
                       (j.status = 'OPEN'
                        AND CURRENT_DATE BETWEEN j.application_start AND j.application_end) AS open,
                       (SELECT count(*) FROM application a
                        WHERE a.job_post_id = j.post_id) AS total,
                       (SELECT count(*) FROM application a
                        WHERE a.job_post_id = j.post_id AND a.status = 'SUBMITTED') AS news
                FROM job j
                JOIN post p ON p.id = j.post_id
                WHERE j.company_id = :company
                  AND p.deleted_at IS NULL
                ORDER BY p.created_at DESC
                """,
                Map.of("company", companyId),
                (rs, n) -> new MyJob(
                        rs.getObject("post_id", UUID.class),
                        rs.getString("title"),
                        rs.getString("work_type"),
                        rs.getObject("application_end", LocalDate.class),
                        rs.getBoolean("open"),
                        rs.getLong("total"),
                        rs.getLong("news")
                )
        );
    }

    @Transactional(readOnly = true)
    public List<Candidate> candidates(UUID companyId, UUID postId) {
        assertJobOwner(companyId, postId);

        return jdbc.query(
                """
                SELECT a.id, a.student_id, s.full_name, u.username,
                       af.storage_key AS avatar_key, cr.name AS course_name,
                       a.message, a.status, a.created_at
                FROM application a
                JOIN students s ON s.id = a.student_id
                JOIN usuario u  ON u.id = a.student_id
                LEFT JOIN course cr ON cr.id = s.course_id
                LEFT JOIN file af   ON af.id = u.avatar_file_id
                WHERE a.job_post_id = :post
                ORDER BY a.created_at DESC
                """,
                Map.of("post", postId),
                (rs, n) -> new Candidate(
                        rs.getObject("id", UUID.class),
                        rs.getObject("student_id", UUID.class),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("avatar_key"),
                        rs.getString("course_name"),
                        rs.getString("message"),
                        rs.getString("status"),
                        rs.getObject("created_at", OffsetDateTime.class)
                )
        );
    }

    private record AppRow(UUID studentId, UUID postId, String status, String title, UUID companyId) {}

    @Transactional
    public void setStatus(UUID companyId, UUID applicationId, String status) {
        if (status == null || !COMPANY_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Estado inválido.");
        }

        List<AppRow> rows = jdbc.query(
                """
                SELECT a.student_id, a.job_post_id, a.status, j.title, j.company_id
                FROM application a
                JOIN job j ON j.post_id = a.job_post_id
                WHERE a.id = :id
                """,
                Map.of("id", applicationId),
                (rs, n) -> new AppRow(
                        rs.getObject("student_id", UUID.class),
                        rs.getObject("job_post_id", UUID.class),
                        rs.getString("status"),
                        rs.getString("title"),
                        rs.getObject("company_id", UUID.class)
                )
        );

        if (rows.isEmpty() || !companyId.equals(rows.get(0).companyId())) {
            throw new IllegalArgumentException("Candidatura não encontrada.");
        }

        AppRow row = rows.get(0);
        if (row.status().equals(status)) {
            return;
        }

        jdbc.update(
                "UPDATE application SET status = :status, updated_at = now() WHERE id = :id",
                Map.of("status", status, "id", applicationId)
        );

        String companyName = users.findById(companyId)
                .map(User::getDisplayName)
                .orElse("A empresa");

        notifications.notifyApplicationUpdate(
                row.studentId(), companyId, companyName, row.postId(), row.title(), status);
    }

    // ---------- auxiliares ----------

    private JobRow findJob(UUID postId) {
        List<JobRow> rows = jdbc.query(
                """
                SELECT j.company_id, j.title,
                       (j.status = 'OPEN'
                        AND CURRENT_DATE BETWEEN j.application_start AND j.application_end) AS open
                FROM job j
                JOIN post p ON p.id = j.post_id
                WHERE j.post_id = :post
                  AND p.deleted_at IS NULL
                """,
                Map.of("post", postId),
                (rs, n) -> new JobRow(
                        rs.getObject("company_id", UUID.class),
                        rs.getString("title"),
                        rs.getBoolean("open")
                )
        );

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Vaga não encontrada.");
        }
        return rows.get(0);
    }

    private void assertJobOwner(UUID companyId, UUID postId) {
        if (!companyId.equals(findJob(postId).companyId())) {
            throw new IllegalArgumentException("Vaga não encontrada.");
        }
    }
}