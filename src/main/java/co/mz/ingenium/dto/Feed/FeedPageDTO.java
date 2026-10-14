package co.mz.ingenium.dto.Feed;

import java.util.List;

/** One page of the feed. hasMore tells the app whether to ask for the next page. */
public record FeedPageDTO(
        List<FeedItemDTO> items,
        int page,
        int size,
        boolean hasMore
) {}