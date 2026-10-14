package co.mz.ingenium.avatar;

import co.mz.ingenium.entity.UserEntity.User;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users/me/avatar")
public class AvatarController {

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    /** Foto atual de quem está autenticado. */
    @GetMapping
    public AvatarResponse get(@AuthenticationPrincipal User user) {
        return avatarService.current(user.getId());
    }

    /** Envia (ou troca) a foto. Corpo: multipart/form-data, campo "file". */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AvatarResponse upload(@AuthenticationPrincipal User user,
                                 @RequestParam("file") MultipartFile file) {
        return avatarService.replace(user.getId(), file);
    }

    /** Remove a foto. */
    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user) {
        avatarService.remove(user.getId());
        return ResponseEntity.noContent().build();
    }
}
