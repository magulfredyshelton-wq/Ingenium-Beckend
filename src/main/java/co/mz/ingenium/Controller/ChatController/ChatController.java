package co.mz.ingenium.Controller.ChatController;

import co.mz.ingenium.dto.Chat.ChatActivityDTO;
import co.mz.ingenium.dto.Chat.ChatMessageDTO;
import co.mz.ingenium.dto.Chat.ConversationDTO;
import co.mz.ingenium.dto.Chat.ResponseMessageDTO;
import co.mz.ingenium.dto.searchUser.UserSummaryDTO;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.repository.UserRepository.UserRepository;
import co.mz.ingenium.service.ChatService.MessagingService;
import co.mz.ingenium.service.NotificationService.NotificationService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
public class ChatController {

    private final MessagingService messagingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public ChatController(
            MessagingService messagingService,
            SimpMessagingTemplate messagingTemplate,
            NotificationService notificationService,
            UserRepository userRepository) {
        this.messagingService = messagingService;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @MessageMapping("/chat.send")
    public void send(
            @Payload @Valid ChatMessageDTO dto,
            Principal principal) {
        UUID senderId =
                UUID.fromString(principal.getName());

        ResponseMessageDTO response =
                messagingService.sendMessage(senderId, dto);

        messagingTemplate.convertAndSendToUser(
                dto.recipientId().toString(),
                "/queue/messages",
                response);

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/messages",
                response);

        try {
            String senderName = userRepository.findById(senderId)
                    .map(User::getDisplayName)
                    .orElse("Alguém");

            notificationService.notifyNewMessage(
                    dto.recipientId(),
                    senderId,
                    senderName,
                    response.roomId(),
                    previewOf(response));
        } catch (Exception e) {
            System.err.println(
                    "Notificação de mensagem falhou: "
                            + e.getMessage());
        }
    }

    @MessageMapping("/chat.activity")
    public void activity(
            @Payload @Valid ChatActivityDTO.Request request,
            Principal principal) {
        if (principal == null) return;

        UUID senderId =
                UUID.fromString(principal.getName());

        messagingService.activity(senderId, request)
                .ifPresent(event ->
                        messagingTemplate.convertAndSendToUser(
                                event.recipientId().toString(),
                                "/queue/chat.activity",
                                event));
    }

    private static String previewOf(ResponseMessageDTO message) {
        if (message.message() != null
                && !message.message().isBlank()) {
            return message.message();
        }

        if (message.type() == null) return "Mensagem";

        return switch (message.type()) {
            case IMAGE -> "Enviou uma imagem";
            case AUDIO -> "Enviou um áudio";
            case FILE -> "Enviou um ficheiro";
            default -> "Nova mensagem";
        };
    }

    @GetMapping("/chat/{otherUserId}/messages")
    @ResponseBody
    public List<ResponseMessageDTO> history(
            @PathVariable UUID otherUserId,
            @AuthenticationPrincipal User user) {
        return messagingService.getHistoryConversation(
                user.getId(),
                otherUserId);
    }

    @GetMapping("/chat/rooms")
    @ResponseBody
    public List<ConversationDTO> rooms(
            @AuthenticationPrincipal User user) {
        return messagingService.getConversations(
                user.getId());
    }

    @GetMapping("/chat/users")
    @ResponseBody
    public List<UserSummaryDTO> searchUsers(
            @RequestParam String q,
            @AuthenticationPrincipal User user) {
        return messagingService.searchUsers(
                q,
                user.getId());
    }
}