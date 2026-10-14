package co.mz.ingenium.Controller.sharing;

import co.mz.ingenium.dto.sharing.ShareDtos;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.service.sharing.ShareService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/sharing")
public class ShareController {

    private final ShareService service;

    public ShareController(ShareService service) {
        this.service = service;
    }

    @GetMapping("/following")
    public ShareDtos.PeoplePage following(
            @AuthenticationPrincipal User me,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page) {
        return service.following(me.getId(), q, page);
    }

    @PostMapping("/posts/{postId}")
    public ShareDtos.Result share(
            @AuthenticationPrincipal User me,
            @PathVariable UUID postId,
            @RequestBody @Valid ShareDtos.Request request) {
        return service.share(me.getId(), postId, request);
    }
}