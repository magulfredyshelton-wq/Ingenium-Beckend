package co.mz.ingenium.chatfile;

import co.mz.ingenium.entity.MessageEntity.MessageType;

import java.util.UUID;

/** Resposta do upload: o cliente guarda o fileId e usa-o ao enviar a mensagem. */
public record ChatFileResponse(UUID fileId,
                               MessageType type,
                               String originalName,
                               String contentType,
                               long sizeBytes,
                               Integer durationSeconds) {
}
