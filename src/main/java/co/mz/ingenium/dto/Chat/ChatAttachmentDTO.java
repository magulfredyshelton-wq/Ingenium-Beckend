package co.mz.ingenium.dto.Chat;

import java.util.UUID;

/** Anexo de uma mensagem. O url é temporário (expira em poucos minutos). */
public record ChatAttachmentDTO(UUID fileId,
                                String name,
                                String contentType,
                                long sizeBytes,
                                Integer durationSeconds,
                                String url) {}
