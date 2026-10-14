package co.mz.ingenium.Controller.NotificationController;

import co.mz.ingenium.dto.Notification.NotificationDTO;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.service.NotificationService.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationDTO> list(@AuthenticationPrincipal User user,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return service.list(user.getId(), page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal User user) {
        return Map.of("count", service.unreadCount(user.getId()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        service.markRead(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal User user) {
        service.markAllRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    /** Chamado ao abrir uma conversa. */
    @PostMapping("/read-chat/{otherUserId}")
    public ResponseEntity<Void> markChatRead(@AuthenticationPrincipal User user, @PathVariable UUID otherUserId) {
        service.markChatRead(user.getId(), otherUserId);
        return ResponseEntity.noContent().build();
    }
}
