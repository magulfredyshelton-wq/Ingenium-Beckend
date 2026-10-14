package co.mz.ingenium.storage;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

/**
 * Operações de ficheiros nos dois buckets.
 * A base de dados guarda só a chave (key) do ficheiro, nunca o ficheiro.
 */
@Service
public class StorageService {

    private final StorageProperties props;
    private final S3Client publicClient;
    private final S3Client privateClient;
    private final S3Presigner privatePresigner;

    public StorageService(StorageProperties props,
                          @Qualifier("publicS3Client") S3Client publicClient,
                          @Qualifier("privateS3Client") S3Client privateClient,
                          @Qualifier("privateS3Presigner") S3Presigner privatePresigner) {
        this.props = props;
        this.publicClient = publicClient;
        this.privateClient = privateClient;
        this.privatePresigner = privatePresigner;
    }

    // ---------- bucket público ----------

    /** Envia para o bucket público e devolve o endereço público do ficheiro. */
    public String putPublic(String key, byte[] data, String contentType) {
        publicClient.putObject(
                PutObjectRequest.builder()
                        .bucket(props.publicBucket().name())
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(data));
        return publicUrl(key);
    }

    public void deletePublic(String key) {
        publicClient.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(props.publicBucket().name())
                        .key(key)
                        .build());
    }

    /** Endereço público de uma chave (as chaves geradas por nós não têm espaços). */
    public String publicUrl(String key) {
        String base = props.publicBucket().baseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + key;
    }

    // ---------- bucket privado ----------

    public void putPrivate(String key, byte[] data, String contentType) {
        privateClient.putObject(
                PutObjectRequest.builder()
                        .bucket(props.privateBucket().name())
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(data));
    }

    public void deletePrivate(String key) {
        privateClient.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(props.privateBucket().name())
                        .key(key)
                        .build());
    }

    /** Link temporário para descarregar um ficheiro privado. */
    public String presignedGetPrivate(String key, Duration validity) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(validity)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(props.privateBucket().name())
                        .key(key)
                        .build())
                .build();
        return privatePresigner.presignGetObject(request).url().toString();
    }
}
