package co.mz.ingenium.dto.Chat;

import co.mz.ingenium.entity.MessageEntity.MessageType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseMessageDTO(
        UUID id,
        UUID roomId,
        UUID senderId,
        LocalDateTime sentAt,
        String message,
        MessageType type,
        ChatAttachmentDTO attachment,
        ChatReplyDTO replyTo,
        LocalDateTime readAt,
        LocalDateTime deletedAt,
        boolean forwarded) {

    public ResponseMessageDTO(
            UUID id,
            UUID roomId,
            UUID senderId,
            LocalDateTime sentAt,
            String message,
            MessageType type,
            ChatAttachmentDTO attachment) {
        this(id, roomId, senderId, sentAt,
                message, type, attachment,
                null, null, null, false);
    }
}