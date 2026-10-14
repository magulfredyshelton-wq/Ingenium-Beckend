package co.mz.ingenium.dto.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class ApplicationDtos {

    private ApplicationDtos() {}

    /** Corpo de POST /jobs/{id}/apply (a mensagem é opcional). */
    public record ApplyRequest(String message) {}

    /** Corpo de POST /applications/{id}/status. */
    public record StatusRequest(String status) {}

    /** Um candidato, visto pela empresa. */
    public record Candidate(
            UUID id,
            UUID studentId,
            String name,
            String username,
            String avatarKey,
            String courseName,
            String message,
            String status,
            OffsetDateTime createdAt
    ) {}

    /** Uma candidatura, vista pelo estudante. */
    public record Mine(
            UUID id,
            UUID postId,
            String jobTitle,
            UUID companyId,
            String companyName,
            String workType,
            boolean jobOpen,
            String status,
            OffsetDateTime createdAt
    ) {}

    /** Uma vaga da empresa, com o número de candidatos. */
    public record MyJob(
            UUID postId,
            String title,
            String workType,
            LocalDate applicationEnd,
            boolean open,
            long applicationsCount,
            long newCount
    ) {}
}