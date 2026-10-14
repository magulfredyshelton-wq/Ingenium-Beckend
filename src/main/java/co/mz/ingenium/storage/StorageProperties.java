package co.mz.ingenium.storage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração do armazenamento de ficheiros (protocolo S3).
 * Funciona com Backblaze B2, Supabase Storage, Cloudflare R2, etc.
 * Para mudar de provedor basta mudar o application.properties.
 */
@Validated
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        @NotBlank String endpoint,
        @NotBlank String region,
        @Valid @NotNull PublicBucket publicBucket,
        @Valid @NotNull PrivateBucket privateBucket
) {

    /** Bucket público: fotos de perfil, imagens dos posts. */
    public record PublicBucket(
            @NotBlank String name,
            @NotBlank String accessKey,
            @NotBlank String secretKey,
            /** Prefixo do endereço público, sem barra final. */
            @NotBlank String baseUrl
    ) {
    }

    /** Bucket privado: ficheiros do chat. */
    public record PrivateBucket(
            @NotBlank String name,
            @NotBlank String accessKey,
            @NotBlank String secretKey
    ) {
    }
}
