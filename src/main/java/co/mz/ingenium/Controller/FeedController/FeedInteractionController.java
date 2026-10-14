package co.mz.ingenium.Controller.FeedController;

import co.mz.ingenium.dto.Feed.CreatePostRequest;
import co.mz.ingenium.dto.Feed.FeedCommentDTO;
import co.mz.ingenium.dto.Feed.FeedTagDTO;
import co.mz.ingenium.dto.Feed.PostCreatedDTO;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.service.Feed.FeedInteractionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/feed")
public class FeedInteractionController {

    private final FeedInteractionService service;

    public FeedInteractionController(
            FeedInteractionService service
    ) {
        this.service = service;
    }

    @GetMapping("/tags")
    public List<FeedTagDTO> tags() {
        return service.tags();
    }

    @PostMapping("/posts")
    public PostCreatedDTO create(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid CreatePostRequest request
    ) {
        return service.create(user.getId(), request);
    }

    @PostMapping(
            path = "/posts/{postId}/media",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> uploadMedia(
            @AuthenticationPrincipal User user,
            @PathVariable UUID postId,
            @RequestPart("file") MultipartFile file
    ) {
        service.uploadMedia(user.getId(), postId, file);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Void> like(
            @AuthenticationPrincipal User user,
            @PathVariable UUID postId
    ) {
        service.like(user.getId(), postId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/posts/{postId}/like")
    public ResponseEntity<Void> unlike(
            @AuthenticationPrincipal User user,
            @PathVariable UUID postId
    ) {
        service.unlike(user.getId(), postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/{postId}/comments")
    public List<FeedCommentDTO> comments(
            @PathVariable UUID postId
    ) {
        return service.comments(postId);
    }

    @PostMapping("/posts/{postId}/comments")
    public FeedCommentDTO comment(
            @AuthenticationPrincipal User user,
            @PathVariable UUID postId,
            @RequestBody Map<String, String> body
    ) {
        return service.addComment(
                user.getId(),
                postId,
                body.get("content")
        );
    }
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID postId
    ) {
        service.delete(user.getId(), postId);
        return ResponseEntity.noContent().build();
    }
}
