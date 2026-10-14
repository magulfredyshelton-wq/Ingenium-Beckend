package co.mz.ingenium.service.Feed;

import co.mz.ingenium.config.Feed.FeedWeights;
import co.mz.ingenium.dto.Feed.FeedCandidate;
import co.mz.ingenium.dto.Feed.PostType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@EnableConfigurationProperties(FeedWeights.class)
public class FeedScorer {

    private final FeedWeights weights;

    public FeedScorer (FeedWeights weights) {
        this.weights = weights;
    }

    public double score(FeedCandidate candidate) {
        double relevance = weights.course() * candidate.courseWeight() + weights.tag() * candidate.followedTags() +
                candidate.followedCategories();

        double social = candidate.authorFollowed() ? weights.author() : 0;

        double recency = weights.recency() * Math.exp(-candidate.hoursSince() / weights.halfLifeHours());

        double base = relevance + social + recency;

        boolean relevantJob = candidate.postType() == PostType.JOB && relevance > 0;
        double multiplier = relevantJob ? weights.jobMultiplier() : 1.0;

        double urgency = 0;

        if (relevantJob && candidate.daysLeft() != null && candidate.daysLeft() <= weights.urgencyWindowDays()) {
            double days = Math.max(candidate.daysLeft(), 0);
            urgency = weights.urgency() * (1 - days / weights.urgencyWindowDays());
        }

        return base * multiplier + urgency;
    }

    public record Scored(FeedCandidate candidate, double score) {}

    public List<Scored> rank(List<FeedCandidate> candidates) {
        return candidates.stream()
                .map(c -> new Scored(c, score(c)))
                .sorted(Comparator.<Scored>comparingDouble(Scored::score).reversed()
                        .thenComparingDouble(s -> s.candidate().hoursSince()))
                .toList();
    }

}
