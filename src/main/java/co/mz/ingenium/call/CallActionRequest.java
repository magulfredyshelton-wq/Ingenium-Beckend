package co.mz.ingenium.call;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Aceitar, recusar ou cancelar uma chamada de uma conversa. */
public record CallActionRequest(@NotNull UUID roomId) {
}
