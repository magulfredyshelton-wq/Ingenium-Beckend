package co.mz.ingenium.dto.Chat;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class ChatActivityDTO {

    private ChatActivityDTO() {
    }

    public enum State {
        TYPING,
        RECORDING,
        IDLE
    }

    public record Request(
            @NotNull UUID recipientId,
            @NotNull State state) {
    }

    public record Event(
            UUID roomId,
            UUID senderId,
            UUID recipientId,
            State state,
            Instant sentAt,
            long expiresInMs) {
    }
}