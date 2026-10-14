package co.mz.ingenium.dto.Feed;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * One card of the feed, with everything the app needs to draw it
 * (author, title, text, tags, media, counters).
 */
public record FeedItemDTO(
        UUID postId,
        PostType type,
        OffsetDateTime createdAt,      // the app turns this into "2h" or "2 days ago"
        Author author,
        String title,                  // jobs and projects; null for general posts
        String content,                // description (job/project) or text (general post)
        List<String> tags,             // names without '#', the app adds it
        List<Media> media,
        long likesCount,
        long commentsCount,
        boolean likedByMe,
        JobInfo job,                   // only filled when type is JOB
        double score                   // for tuning the weights; remove later
) {

    /** Student: subtitle is the course. Company: subtitle is the activity area. */
    public record Author(UUID id, String name, String subtitle, String avatarKey, String userType) {}

    /** key is the storage key; the public URL is built from it later. */
    public record Media(String key, String contentType) {}

    public record JobInfo(String workType, LocalDate applicationEnd, Integer daysLeft,
                          boolean appliedByMe, long applicationsCount) {}

    public FeedItemDTO withScore(double newScore) {
        return new FeedItemDTO(postId, type, createdAt, author, title, content, tags, media,
                likesCount, commentsCount, likedByMe, job, newScore);
    }
}