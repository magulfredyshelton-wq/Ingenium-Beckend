package co.mz.ingenium.dto.Feed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreatePostRequest(
        @NotNull PostType type,
        @Size(max = 200) String title,
        @NotBlank @Size(max = 2000) String content,
        @Size(max = 5)
        List<@Size(max = 80) String> tags,
        String workType,
        LocalDate applicationStart,
        LocalDate applicationEnd,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 255) String repositoryUrl
) {}