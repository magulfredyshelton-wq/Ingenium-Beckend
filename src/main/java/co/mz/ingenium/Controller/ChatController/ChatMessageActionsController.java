package co.mz.ingenium.Controller.ChatController;

import co.mz.ingenium.dto.Chat.ChatActionDTO;
import co.mz.ingenium.dto.Chat.ResponseMessageDTO;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.repository.UserRepository.UserRepository;
import co.mz.ingenium.service.ChatService.MessagingService;
import co.mz.ingenium.service.NotificationService.NotificationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class ChatMessageActionsController {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ChatMessageActionsController.class);

    private final MessagingService messaging;
    private final SimpMessagingTemplate socket;
    private final NotificationService notifications;
    private final UserRepository users;

    public ChatMessageActionsController(
            MessagingService messaging,
            SimpMessagingTemplate socket,
            NotificationService notifications,
            UserRepository users) {
        this.messaging = messaging;
        this.socket = socket;
        this.notifications = notifications;
        this.users = users;
    }

    @PostMapping("/{otherUserId}/read")
    public ChatActionDTO.ReadReceipt read(
            @PathVariable UUID otherUserId,
            @RequestBody @Valid ChatActionDTO.ReadRequest request,
            @AuthenticationPrincipal User user) {
        ChatActionDTO.ReadReceipt receipt =
                messaging.markRead(
                        user.getId(),
                        otherUserId,
                        request);

        if (!receipt.messageIds().isEmpty()) {
            ChatActionDTO.Event event =
                    new ChatActionDTO.Event(
                            "MESSAGES_READ",
                            receipt);

            eventToBoth(
                    user.getId(),
                    otherUserId,
                    event);
        }

        return receipt;
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseMessageDTO delete(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal User user) {
        ResponseMessageDTO message =
                messaging.deleteMessage(
                        user.getId(),
                        messageId);

        UUID otherId =
                messaging.otherParticipant(
                        user.getId(),
                        messageId);

        ChatActionDTO.Event event =
                new ChatActionDTO.Event(
                        "MESSAGE_DELETED",
                        new ChatActionDTO.DeletedMessage(message));

        eventToBoth(user.getId(), otherId, event);
        return message;
    }

    @GetMapping("/messages/{messageId}/info")
    public ChatActionDTO.MessageInfo info(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal User user) {
        return messaging.messageInfo(
                user.getId(),
                messageId);
    }

    @PostMapping("/messages/{messageId}/forward")
    public ResponseMessageDTO forward(
            @PathVariable UUID messageId,
            @RequestBody @Valid ChatActionDTO.ForwardRequest request,
            @AuthenticationPrincipal User user) {
        ResponseMessageDTO message =
                messaging.forwardMessage(
                        user.getId(),
                        messageId,
                        request.recipientId());

        socket.convertAndSendToUser(
                request.recipientId().toString(),
                "/queue/messages",
                message);

        socket.convertAndSendToUser(
                user.getId().toString(),
                "/queue/messages",
                message);

        try {
            String name = users.findById(user.getId())
                    .map(User::getDisplayName)
                    .orElse("Alguém");

            notifications.notifyNewMessage(
                    request.recipientId(),
                    user.getId(),
                    name,
                    message.roomId(),
                    preview(message));
        } catch (RuntimeException failure) {
            log.warn(
                    "Notificação de mensagem encaminhada falhou: {}",
                    failure.getMessage());
        }

        return message;
    }

    private void eventToBoth(
            UUID first,
            UUID second,
            ChatActionDTO.Event event) {
        socket.convertAndSendToUser(
                first.toString(),
                "/queue/chat.events",
                event);

        socket.convertAndSendToUser(
                second.toString(),
                "/queue/chat.events",
                event);
    }

    private static String preview(
            ResponseMessageDTO message) {
        if (message.message() != null
                && !message.message().isBlank()) {
            return message.message();
        }

        return switch (message.type()) {
            case IMAGE -> "Enviou uma imagem";
            case AUDIO -> "Enviou um áudio";
            case FILE -> "Enviou um ficheiro";
            default -> "Nova mensagem";
        };
    }
}