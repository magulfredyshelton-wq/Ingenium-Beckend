package co.mz.ingenium.config.Feed;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "feed.weights")
public record FeedWeights(
        double course,
        double tag,
        double category,
        double author,
        double recency,
        double halfLifeHours,
        double jobMultiplier,
        double urgency,
        int urgencyWindowDays
) {
}
