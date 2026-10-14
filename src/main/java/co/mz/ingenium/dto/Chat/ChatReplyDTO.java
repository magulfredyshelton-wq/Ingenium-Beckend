package co.mz.ingenium.dto.Chat;

import co.mz.ingenium.entity.MessageEntity.MessageType;

import java.util.UUID;

/** Resumo criado pelo servidor a partir da mensagem original. */
public record ChatReplyDTO(
        UUID id,
        UUID senderId,
        String message,
        MessageType type,
        String attachmentName,
        boolean deleted) {
}