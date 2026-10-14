package co.mz.ingenium.dto.Feed;

import java.util.UUID;

public record FeedCandidate(
        UUID postId,
        PostType postType,
        double hoursSince,
        double courseWeight,
        int followedTags,
        int followedCategories,
        boolean authorFollowed,
        Integer daysLeft

) {
}