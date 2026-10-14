package co.mz.ingenium.avatar;

import co.mz.ingenium.entity.UserEntity.User;
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
import java.util.UUID;

@Service
public class AvatarService {

    private static final Logger log = LoggerFactory.getLogger(AvatarService.class);

    /** Tamanho máximo da foto: 5 MB. */
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final StorageService storage;
    private final StoredFileRepository files;
    private final UserRepository users;

    public AvatarService(StorageService storage, StoredFileRepository files, UserRepository users) {
        this.storage = storage;
        this.files = files;
        this.users = users;
    }

    /** Foto atual do utilizador (avatarUrl = null se não tiver). */
    @Transactional(readOnly = true)
    public AvatarResponse current(UUID userId) {
        User user = findUser(userId);
        if (user.getAvatarFileId() == null) {
            return new AvatarResponse(null);
        }
        return files.findById(user.getAvatarFileId())
                .map(f -> new AvatarResponse(storage.publicUrl(f.getStorageKey())))
                .orElse(new AvatarResponse(null));
    }

    /** Guarda uma nova foto e apaga a anterior. */
    @Transactional
    public AvatarResponse replace(UUID userId, MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw new IllegalArgumentException("Envia uma imagem no campo 'file'.");
        }
        if (upload.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Imagem demasiado grande (máximo 5 MB).");
        }

        byte[] bytes;
        try {
            bytes = upload.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Não confiamos no tipo que o cliente diz: lemos os primeiros bytes do ficheiro.
        ImageType type = detectType(bytes);

        User user = findUser(userId);
        StoredFile old = user.getAvatarFileId() == null
                ? null
                : files.findById(user.getAvatarFileId()).orElse(null);
        String oldKey = old == null ? null : old.getStorageKey();

        String newKey = "avatars/" + userId + "/" + UUID.randomUUID() + "." + type.extension();

        // 1) envia para o bucket
        storage.putPublic(newKey, bytes, type.contentType());

        // Se a transação falhar, apaga o ficheiro novo; se correr bem, apaga o antigo.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    if (oldKey != null) {
                        deleteQuietly(oldKey);
                    }
                } else {
                    deleteQuietly(newKey);
                }
            }
        });

        // 2) regista na base de dados
        StoredFile created = new StoredFile();
        created.setOwnerId(userId);
        created.setStorageKey(newKey);
        created.setContentType(type.contentType());
        created.setSizeBytes(bytes.length);
        created.setVisibility(FileVisibility.PUBLIC);
        files.save(created);

        user.setAvatarFileId(created.getId());
        users.save(user);

        if (old != null) {
            files.delete(old);
        }

        return new AvatarResponse(storage.publicUrl(newKey));
    }

    /** Remove a foto do utilizador (não faz nada se não tiver). */
    @Transactional
    public void remove(UUID userId) {
        User user = findUser(userId);
        UUID fileId = user.getAvatarFileId();
        if (fileId == null) {
            return;
        }

        StoredFile old = files.findById(fileId).orElse(null);
        user.setAvatarFileId(null);
        users.save(user);

        if (old != null) {
            String oldKey = old.getStorageKey();
            files.delete(old);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) {
                        deleteQuietly(oldKey);
                    }
                }
            });
        }
    }

    // ---------- auxiliares ----------

    private User findUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));
    }

    private void deleteQuietly(String key) {
        try {
            storage.deletePublic(key);
        } catch (Exception e) {
            // Não é grave: fica um ficheiro órfão no bucket, mas a aplicação continua.
            log.warn("Não foi possível apagar {} do armazenamento: {}", key, e.getMessage());
        }
    }

    private record ImageType(String contentType, String extension) {
    }

    private static ImageType detectType(byte[] b) {
        // JPEG: FF D8 FF
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return new ImageType("image/jpeg", "jpg");
        }
        // PNG: 89 'P' 'N' 'G' 0D 0A 1A 0A
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return new ImageType("image/png", "png");
        }
        // WebP: 'RIFF' .... 'WEBP'
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return new ImageType("image/webp", "webp");
        }
        throw new IllegalArgumentException("Formato inválido. Usa JPEG, PNG ou WebP.");
    }
}
