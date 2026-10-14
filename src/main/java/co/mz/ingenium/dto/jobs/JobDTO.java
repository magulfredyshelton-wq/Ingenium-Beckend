package co.mz.ingenium.dto.jobs;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record JobDTO(
        UUID postId,
        String title,
        String description,
        UUID companyId,
        String companyName,
        String workType,
        LocalDate applicationEnd,
        Integer daysLeft,
        boolean appliedByMe,
        List<String> tags
) {}