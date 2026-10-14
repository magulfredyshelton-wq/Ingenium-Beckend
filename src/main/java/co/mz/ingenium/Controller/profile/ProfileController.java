package co.mz.ingenium.Controller.profile;

import co.mz.ingenium.dto.Feed.FeedItemDTO;
import co.mz.ingenium.dto.profile.ProfileDTO;
import co.mz.ingenium.dto.profile.UpdateProfileRequest;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.service.profile.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public ProfileDTO me(@AuthenticationPrincipal User user) {
        return service.get(user.getId());
    }

    /** Guarda "Sobre mim" e contactos do próprio utilizador. */
    @PutMapping("/me/profile")
    public ProfileDTO update(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateProfileRequest request
    ) {
        return service.update(user.getId(), request);
    }
    public record UpdateTagsRequest(List<String> slugs) {}

    /** Substitui as hashtags do próprio utilizador. */
    @PutMapping("/me/tags")
    public ProfileDTO updateTags(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateTagsRequest request
    ) {
        return service.updateTags(user.getId(), request.slugs());
    }
    @GetMapping("/{id}")
    public ProfileDTO byId(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return service.get(user.getId(), id);
    }

    @GetMapping("/{id}/posts")
    public List<FeedItemDTO> posts(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.posts(user.getId(), id, page, size);
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<Void> follow(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        service.follow(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/follow")
    public ResponseEntity<Void> unfollow(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        service.unfollow(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}