package co.mz.ingenium.Controller.search;

import co.mz.ingenium.dto.Feed.FeedPageDTO;
import co.mz.ingenium.dto.search.SearchDtos;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.service.search.UserSearchService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
public class UserSearchController {

    private final UserSearchService service;

    public UserSearchController(UserSearchService service) {
        this.service = service;
    }

    @GetMapping("/users")
    public SearchDtos.AccountsPage users(
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.accounts(username, page, size);
    }

    @GetMapping("/posts")
    public FeedPageDTO posts(
            @AuthenticationPrincipal User viewer,
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.posts(
                viewer.getId(), username, page, size);
    }
}