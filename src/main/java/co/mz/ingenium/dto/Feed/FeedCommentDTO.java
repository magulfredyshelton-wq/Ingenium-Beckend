package co.mz.ingenium.dto.Feed;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FeedCommentDTO(
        UUID id,
        Author author,
        String content,
        OffsetDateTime createdAt
) {
    public record Author(
            UUID id,
            String name,
            String avatarKey
    ) {}
}
