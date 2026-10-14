package co.mz.ingenium.chatfile;

import co.mz.ingenium.entity.MessageEntity.MessageType;
import co.mz.ingenium.repository.UserRepository.UserRepository;
import co.mz.ingenium.storage.FileVisibility;
import co.mz.ingenium.storage.StorageService;
import co.mz.ingenium.storage.StoredFile;
import co.mz.ingenium.storage.StoredFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Recebe anexos do chat e guarda-os no bucket PRIVADO.
 * Só aceita os tipos da lista abaixo e confere os primeiros bytes do ficheiro,
 * porque o tipo que o cliente declara pode ser falso.
 */
@Service
public class ChatFileService {

    private static final Logger log = LoggerFactory.getLogger(ChatFileService.class);

    private static final long MB = 1024L * 1024;
    private static final int MAX_AUDIO_SECONDS = 300;

    /** Regra de um tipo aceite: categoria da mensagem, extensão, tamanho máximo e assinatura. */
    private record Rule(MessageType type, String extension, long maxBytes, Predicate<byte[]> signature) {
    }

    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private static final Map<String, Rule> RULES = Map.ofEntries(
            // imagens (5 MB)
            Map.entry("image/jpeg", new Rule(MessageType.IMAGE, "jpg", 5 * MB, ChatFileService::isJpeg)),
            Map.entry("image/png", new Rule(MessageType.IMAGE, "png", 5 * MB, ChatFileService::isPng)),
            Map.entry("image/webp", new Rule(MessageType.IMAGE, "webp", 5 * MB, ChatFileService::isWebp)),
            // áudio (10 MB)
            Map.entry("audio/mp4", new Rule(MessageType.AUDIO, "m4a", 10 * MB, ChatFileService::isMp4)),
            Map.entry("audio/x-m4a", new Rule(MessageType.AUDIO, "m4a", 10 * MB, ChatFileService::isMp4)),
            Map.entry("audio/aac", new Rule(MessageType.AUDIO, "aac", 10 * MB, ChatFileService::isMpegAudio)),
            Map.entry("audio/mpeg", new Rule(MessageType.AUDIO, "mp3", 10 * MB, ChatFileService::isMpegAudio)),
            Map.entry("audio/ogg", new Rule(MessageType.AUDIO, "ogg", 10 * MB, ChatFileService::isOgg)),
            Map.entry("audio/webm", new Rule(MessageType.AUDIO, "webm", 10 * MB, ChatFileService::isWebm)),
            // documentos (10 MB)
            Map.entry("application/pdf", new Rule(MessageType.FILE, "pdf", 10 * MB, ChatFileService::isPdf)),
            Map.entry(DOCX, new Rule(MessageType.FILE, "docx", 10 * MB, ChatFileService::isZip)),
            Map.entry(XLSX, new Rule(MessageType.FILE, "xlsx", 10 * MB, ChatFileService::isZip)),
            Map.entry(PPTX, new Rule(MessageType.FILE, "pptx", 10 * MB, ChatFileService::isZip)),
            Map.entry("text/plain", new Rule(MessageType.FILE, "txt", 10 * MB, ChatFileService::isText))
    );

    private final StorageService storage;
    private final StoredFileRepository files;
    private final UserRepository users;

    public ChatFileService(StorageService storage, StoredFileRepository files, UserRepository users) {
        this.storage = storage;
        this.files = files;
        this.users = users;
    }

    @Transactional
    public ChatFileResponse upload(UUID senderId, UUID recipientId, MultipartFile upload, Integer durationSeconds) {
        if (senderId.equals(recipientId)) {
            throw new IllegalArgumentException("Não podes enviar ficheiros a ti próprio!");
        }
        if (!users.existsById(recipientId)) {
            throw new IllegalArgumentException("Destinatário não encontrado!");
        }
        if (upload == null || upload.isEmpty()) {
            throw new IllegalArgumentException("Envia um ficheiro no campo 'file'.");
        }

        Rule rule = RULES.get(normalize(upload.getContentType()));
        if (rule == null) {
            throw new IllegalArgumentException("Tipo de ficheiro não permitido.");
        }
        if (upload.getSize() > rule.maxBytes()) {
            throw new IllegalArgumentException(
                    "Ficheiro demasiado grande (máximo " + (rule.maxBytes() / MB) + " MB para este tipo).");
        }

        byte[] bytes;
        try {
            bytes = upload.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!rule.signature().test(bytes)) {
            throw new IllegalArgumentException("O conteúdo do ficheiro não corresponde ao tipo indicado.");
        }

        Integer duration = null;
        if (rule.type() == MessageType.AUDIO && durationSeconds != null) {
            if (durationSeconds < 1 || durationSeconds > MAX_AUDIO_SECONDS) {
                throw new IllegalArgumentException("Duração do áudio inválida (máximo " + MAX_AUDIO_SECONDS + " s).");
            }
            duration = durationSeconds;
        }

        String contentType = normalize(upload.getContentType());
        String key = "chat/" + senderId + "/" + UUID.randomUUID() + "." + rule.extension();

        // 1) envia para o bucket privado
        storage.putPrivate(key, bytes, contentType);

        // Se a transação falhar, apaga o ficheiro do bucket (para não ficar órfão).
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        storage.deletePrivate(key);
                    } catch (Exception e) {
                        log.warn("Não foi possível apagar {} do bucket privado: {}", key, e.getMessage());
                    }
                }
            }
        });

        // 2) regista na base de dados
        StoredFile created = new StoredFile();
        created.setOwnerId(senderId);
        created.setStorageKey(key);
        created.setOriginalName(cleanName(upload.getOriginalFilename()));
        created.setContentType(contentType);
        created.setSizeBytes(bytes.length);
        created.setDurationSeconds(duration);
        created.setVisibility(FileVisibility.PRIVATE);
        files.save(created);

        return new ChatFileResponse(created.getId(), rule.type(), created.getOriginalName(),
                created.getContentType(), created.getSizeBytes(), created.getDurationSeconds());
    }

    // ---------- auxiliares ----------

    /** "Audio/MP4; codecs=x" vira "audio/mp4". */
    private static String normalize(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        String base = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return base.trim().toLowerCase(Locale.ROOT);
    }

    /** Só serve para mostrar no chat: tira o caminho e caracteres de controlo, limita a 255. */
    private static String cleanName(String name) {
        if (name == null) {
            return null;
        }
        String n = name.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (n.isEmpty()) {
            return null;
        }
        return n.length() > 255 ? n.substring(0, 255) : n;
    }

    // ---------- assinaturas (primeiros bytes) ----------

    private static boolean at(byte[] b, int offset, int... expected) {
        if (b.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((b[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isJpeg(byte[] b) {
        return at(b, 0, 0xFF, 0xD8, 0xFF);
    }

    private static boolean isPng(byte[] b) {
        return at(b, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
    }

    private static boolean isWebp(byte[] b) {
        return at(b, 0, 'R', 'I', 'F', 'F') && at(b, 8, 'W', 'E', 'B', 'P');
    }

    private static boolean isPdf(byte[] b) {
        return at(b, 0, '%', 'P', 'D', 'F');
    }

    /** docx, xlsx e pptx são ficheiros ZIP. */
    private static boolean isZip(byte[] b) {
        return at(b, 0, 0x50, 0x4B, 0x03, 0x04);
    }

    private static boolean isMp4(byte[] b) {
        return at(b, 4, 'f', 't', 'y', 'p');
    }

    /** mp3 (com etiqueta ID3 ou frame direto) e aac em ADTS. */
    private static boolean isMpegAudio(byte[] b) {
        return at(b, 0, 'I', 'D', '3')
                || (b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xE0) == 0xE0);
    }

    private static boolean isOgg(byte[] b) {
        return at(b, 0, 'O', 'g', 'g', 'S');
    }

    private static boolean isWebm(byte[] b) {
        return at(b, 0, 0x1A, 0x45, 0xDF, 0xA3);
    }

    /** Texto simples: sem bytes nulos nos primeiros 512 bytes. */
    private static boolean isText(byte[] b) {
        int limit = Math.min(b.length, 512);
        for (int i = 0; i < limit; i++) {
            if (b[i] == 0) {
                return false;
            }
        }
        return true;
    }
}
