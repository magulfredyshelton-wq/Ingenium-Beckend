package co.mz.ingenium.service.Feed;

import co.mz.ingenium.dto.Feed.FeedItemDTO;
import co.mz.ingenium.dto.Feed.FeedPageDTO;
import co.mz.ingenium.repository.Feed.FeedItemRepository;
import co.mz.ingenium.repository.Feed.FeedRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedItemRepository itemRepository;
    private final FeedScorer scorer;

    public FeedService(FeedRepository feedRepository,
                       FeedItemRepository itemRepository,
                       FeedScorer scorer) {
        this.feedRepository = feedRepository;
        this.itemRepository = itemRepository;
        this.scorer = scorer;
    }

    @Transactional(readOnly = true)
    public FeedPageDTO getFeed(UUID userId, int page, int size) {
        // 1. candidates (SQL) and their order (Java)
        List<FeedScorer.Scored> ranked = scorer.rank(feedRepository.findCandidates(userId));

        // 2. cut the requested page (never go past the end of the list)
        int from = Math.min(page * size, ranked.size());
        int to = Math.min(from + size, ranked.size());
        List<FeedScorer.Scored> slice = ranked.subList(from, to);

        // 3. card details only for the posts of this page
        List<UUID> ids = slice.stream().map(s -> s.candidate().postId()).toList();
        Map<UUID, FeedItemDTO> details = itemRepository.findItems(ids, userId);

        // 4. keep the ranking order and attach the score to each card
        List<FeedItemDTO> items = slice.stream()
                .filter(s -> details.containsKey(s.candidate().postId()))
                .map(s -> details.get(s.candidate().postId()).withScore(s.score()))
                .toList();

        return new FeedPageDTO(items, page, size, to < ranked.size());
    }

    /** Um único post (usado pelo clique numa notificação). */
    @Transactional(readOnly = true)
    public FeedItemDTO getPost(UUID userId, UUID postId) {
        FeedItemDTO item = itemRepository.findItems(List.of(postId), userId).get(postId);
        if (item == null) {
            throw new IllegalArgumentException("Publicação não encontrada.");
        }
        return item;
    }
}
