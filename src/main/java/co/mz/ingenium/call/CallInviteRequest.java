package co.mz.ingenium.call;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Pedido para ligar: roomId é o id da conversa; video=false é uma chamada só de voz. */
public record CallInviteRequest(@NotNull UUID roomId, boolean video) {
}
