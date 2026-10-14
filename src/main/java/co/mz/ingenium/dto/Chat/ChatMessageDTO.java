package co.mz.ingenium.dto.Chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatMessageDTO(
        @NotNull UUID recipientId,
        @Size(max = 1000) String message,
        UUID fileId,
        UUID replyToMessageId) {

    public ChatMessageDTO(
            UUID recipientId,
            String message,
            UUID fileId) {
        this(recipientId, message, fileId, null);
    }
}