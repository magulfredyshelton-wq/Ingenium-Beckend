package co.mz.ingenium.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Cria um cliente e um presigner para cada bucket, porque cada bucket
 * tem a sua própria chave de acesso.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    // ---------- bucket público ----------

    @Bean(destroyMethod = "close")
    public S3Client publicS3Client(StorageProperties p) {
        return client(p, p.publicBucket().accessKey(), p.publicBucket().secretKey());
    }

    @Bean(destroyMethod = "close")
    public S3Presigner publicS3Presigner(StorageProperties p) {
        return presigner(p, p.publicBucket().accessKey(), p.publicBucket().secretKey());
    }

    // ---------- bucket privado ----------

    @Bean(destroyMethod = "close")
    public S3Client privateS3Client(StorageProperties p) {
        return client(p, p.privateBucket().accessKey(), p.privateBucket().secretKey());
    }

    @Bean(destroyMethod = "close")
    public S3Presigner privateS3Presigner(StorageProperties p) {
        return presigner(p, p.privateBucket().accessKey(), p.privateBucket().secretKey());
    }

    // ---------- auxiliares ----------

    private S3Client client(StorageProperties p, String accessKey, String secretKey) {
        return S3Client.builder()
                .endpointOverride(URI.create(p.endpoint()))
                .region(Region.of(p.region()))
                .credentialsProvider(credentials(accessKey, secretKey))
                // path-style: o nome do bucket vai no caminho, não no domínio
                // (evita problemas com maiúsculas no nome do bucket)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                // as versões recentes do SDK enviam checksums extra que alguns
                // provedores S3-compatíveis rejeitam
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    private S3Presigner presigner(StorageProperties p, String accessKey, String secretKey) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(p.endpoint()))
                .region(Region.of(p.region()))
                .credentialsProvider(credentials(accessKey, secretKey))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private StaticCredentialsProvider credentials(String accessKey, String secretKey) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
}
