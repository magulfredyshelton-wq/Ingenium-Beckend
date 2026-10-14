package co.mz.ingenium.dto.Chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class ChatActionDTO {

    private ChatActionDTO() {
    }

    public record ReadRequest(
            @NotNull @Size(max = 200)
            List<@NotNull UUID> messageIds) {
    }

    public record ReadReceipt(
            UUID roomId,
            List<UUID> messageIds,
            UUID readerId,
            LocalDateTime readAt) {
    }

    public record ForwardRequest(
            @NotNull UUID recipientId) {
    }

    public record MessageInfo(
            UUID id,
            UUID senderId,
            LocalDateTime sentAt,
            LocalDateTime readAt,
            LocalDateTime deletedAt,
            boolean forwarded) {
    }

    public record DeletedMessage(
            ResponseMessageDTO message) {
    }

    public record Event(
            String type,
            Object payload) {
    }
}