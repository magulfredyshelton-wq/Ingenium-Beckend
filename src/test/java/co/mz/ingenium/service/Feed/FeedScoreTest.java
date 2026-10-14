package co.mz.ingenium.service.Feed;

import co.mz.ingenium.config.Feed.FeedWeights;
import co.mz.ingenium.dto.Feed.FeedCandidate;
import co.mz.ingenium.dto.Feed.PostType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedScorerTest {

    // course 3, tag 2, category 1, author 2, recency 5, half-life 48h,
    // job x1.5, urgency 3, urgency window 7 days
    private final FeedWeights weights = new FeedWeights(3, 2, 1, 2, 5, 48, 1.5, 3, 7);
    private final FeedScorer scorer = new FeedScorer(weights);

    private FeedCandidate post(PostType type, double hours, double courseWeight,
                               int tags, int categories, boolean author, Integer daysLeft) {
        return new FeedCandidate(UUID.randomUUID(), type, hours, courseWeight,
                tags, categories, author, daysLeft);
    }

    // With 0 hours the recency bonus is 5 * e^0 = 5.

    @Test
    void courseWeightCounts() {
        var p = post(PostType.GENERAL, 0, 2.0, 0, 0, false, null);
        assertEquals(3 * 2.0 + 5, scorer.score(p), 1e-9);          // 11
    }

    @Test
    void followedTagCounts() {
        var p = post(PostType.GENERAL, 0, 0, 1, 0, false, null);
        assertEquals(2 + 5, scorer.score(p), 1e-9);                // 7
    }

    @Test
    void followedCategoryCounts() {
        var p = post(PostType.GENERAL, 0, 0, 0, 1, false, null);
        assertEquals(1 + 5, scorer.score(p), 1e-9);                // 6
    }

    @Test
    void irrelevantJobGetsNoMultiplierAndNoUrgency() {
        // no tag in common, closes tomorrow: only the recency bonus counts
        var job = post(PostType.JOB, 10, 0, 0, 0, false, 1);
        assertEquals(5 * Math.exp(-10.0 / 48), scorer.score(job), 1e-9);
    }

    @Test
    void relevantJobGetsMultiplierAndUrgency() {
        // course weight 2.0, author followed, 48h old, 5 days left
        var job = post(PostType.JOB, 48, 2.0, 0, 0, true, 5);
        double base = 3 * 2.0 + 2 + 5 * Math.exp(-1.0);
        double expected = base * 1.5 + 3 * (1 - 5.0 / 7);
        assertEquals(expected, scorer.score(job), 1e-9);           // about 15.6
    }

    @Test
    void relevantJobBeatsFriendsProject() {
        var job = post(PostType.JOB, 48, 2.0, 0, 0, true, 5);
        var friendProject = post(PostType.PROJECT, 24, 0, 1, 0, true, null);
        assertTrue(scorer.score(job) > scorer.score(friendProject));
    }

    @Test
    void rankOrdersFromBestToWorst() {
        var best = post(PostType.JOB, 48, 2.0, 0, 0, true, 5);
        var middle = post(PostType.PROJECT, 24, 0, 1, 0, true, null);
        var worst = post(PostType.GENERAL, 8, 0, 0, 0, false, null);

        var ranked = scorer.rank(List.of(worst, best, middle));

        assertEquals(best.postId(), ranked.get(0).candidate().postId());
        assertEquals(middle.postId(), ranked.get(1).candidate().postId());
        assertEquals(worst.postId(), ranked.get(2).candidate().postId());
    }
}