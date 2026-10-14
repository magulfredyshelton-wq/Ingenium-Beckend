package co.mz.ingenium.Controller.FeedController;

import co.mz.ingenium.dto.Feed.FeedItemDTO;
import co.mz.ingenium.dto.Feed.FeedPageDTO;
import co.mz.ingenium.service.Feed.FeedService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import co.mz.ingenium.entity.UserEntity.User;

import java.util.UUID;

@RestController
@RequestMapping("/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public FeedPageDTO feed(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            Authentication authentication) {
        // protect against absurd values
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        return feedService.getFeed(currentUserId(authentication), safePage, safeSize);
    }

    @GetMapping("/posts/{postId}")
    public FeedItemDTO post(@PathVariable UUID postId, Authentication authentication) {
        return feedService.getPost(currentUserId(authentication), postId);
    }

    // ADJUST: this assumes the token's subject (authentication.getName()) is the user's UUID.
    // Check what JWTService puts in the token.
    private UUID currentUserId(Authentication authentication) {
        return ((User) authentication.getPrincipal()).getId();
    }
}