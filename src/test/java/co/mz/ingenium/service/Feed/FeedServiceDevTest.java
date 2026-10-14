package co.mz.ingenium.service.Feed;

import co.mz.ingenium.dto.Feed.FeedItemDTO;
import co.mz.ingenium.dto.Feed.FeedPageDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DEV ONLY. Starts the whole application, reads the real dev database (read only)
 * and prints the feed of each test user, so we can see the order with our own eyes.
 * Delete it (or add @Disabled) before anyone runs the tests on another database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeedServiceDevTest {

    @Autowired
    private FeedService feedService;

    @Test
    void printFeedForTestUsers() {
        Map<String, UUID> users = new LinkedHashMap<>();
        users.put("Ana (Redes, procura estagio)", UUID.fromString("a0000000-0000-0000-0000-000000000001"));
        users.put("Bruno (Informatica, ambos)",   UUID.fromString("a0000000-0000-0000-0000-000000000002"));
        users.put("Carla (Contabilidade, emprego)", UUID.fromString("a0000000-0000-0000-0000-000000000003"));
        users.put("Daniel (sem curso)",           UUID.fromString("a0000000-0000-0000-0000-000000000004"));

        users.forEach((label, userId) -> {
            FeedPageDTO page = feedService.getFeed(userId, 0, 20);

            System.out.println();
            System.out.println("=== Feed de " + label + ": " + page.items().size()
                    + " posts, hasMore=" + page.hasMore() + " ===");

            double previous = Double.MAX_VALUE;
            for (FeedItemDTO item : page.items()) {
                String text = item.title() != null ? item.title() : item.content();
                System.out.printf("%7.2f  %-8s  %-24s  %s%n",
                        item.score(), item.type(), shorten(item.author().name(), 24), shorten(text, 45));

                // the feed must be ordered from best to worst, and never show the user's own posts
                assertTrue(item.score() <= previous, "feed out of order");
                assertNotEquals(userId, item.author().id(), "user sees own post");
                previous = item.score();
            }
        });
    }

    private static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ');
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max - 1) + "…";
    }
}