package co.mz.ingenium.service.Verification;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class VerificationCodeService {

    private static final int CODE_TTL_SECONDS = 600;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_CODES_PER_HOUR = 5;
    private static final int MAX_ATTEMPTS = 5;

    private static final String INVALID =
            "Código inválido ou expirado.";
    private static final String TOO_MANY =
            "Demasiadas tentativas. Peça um novo código.";

    private final SecureRandom random = new SecureRandom();
    private final NamedParameterJdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate attemptTransaction;

    public VerificationCodeService(
            NamedParameterJdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            PlatformTransactionManager transactionManager) {

        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.attemptTransaction =
                new TransactionTemplate(transactionManager);
        this.attemptTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private record ActiveCode(UUID id, String hash, int attempts) {}
    private record CheckResult(UUID id, String error) {}

    @Transactional
    public String issue(String email, VerificationPurpose purpose) {
        String key = normalize(email);
        Objects.requireNonNull(purpose, "Finalidade obrigatória.");
        MapSqlParameterSource parameters = params(key, purpose);

        jdbc.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))",
                new MapSqlParameterSource(
                        "key", key + ":" + purpose.name()),
                (rs, rowNum) -> Boolean.TRUE);

        if (countCodes(key, purpose, RESEND_COOLDOWN_SECONDS) > 0) {
            throw new IllegalArgumentException(
                    "Aguarde um minuto antes de pedir outro código.");
        }

        if (countCodes(key, purpose, 3600) >= MAX_CODES_PER_HOUR) {
            throw new IllegalArgumentException(
                    "Pediu códigos a mais. Tente de novo dentro de uma hora.");
        }

        jdbc.update("""
                UPDATE verification_code SET used_at = now()
                WHERE email = :email
                  AND purpose = :purpose
                  AND used_at IS NULL
                """, parameters);

        String code = String.format(
                Locale.ROOT, "%06d", random.nextInt(1_000_000));

        jdbc.update("""
                INSERT INTO verification_code
                    (id, email, purpose, code_hash, expires_at)
                VALUES
                    (:id, :email, :purpose, :hash,
                     now() + (:ttl * interval '1 second'))
                """, parameters
                .addValue("id", UUID.randomUUID())
                .addValue("hash", passwordEncoder.encode(code))
                .addValue("ttl", CODE_TTL_SECONDS));

        return code;
    }

    public void verify(
            String email,
            VerificationPurpose purpose,
            String code) {

        String key = normalize(email);
        Objects.requireNonNull(purpose, "Finalidade obrigatória.");

        CheckResult result = attemptTransaction.execute(status -> {
            List<ActiveCode> found = jdbc.query("""
                    SELECT id, code_hash, attempts
                    FROM verification_code
                    WHERE email = :email
                      AND purpose = :purpose
                      AND used_at IS NULL
                      AND expires_at > now()
                    ORDER BY created_at DESC
                    LIMIT 1
                    FOR UPDATE
                    """, params(key, purpose),
                    (rs, rowNum) -> new ActiveCode(
                            rs.getObject("id", UUID.class),
                            rs.getString("code_hash"),
                            rs.getInt("attempts")));

            if (found.isEmpty()) {
                return new CheckResult(null, INVALID);
            }

            ActiveCode active = found.getFirst();
            MapSqlParameterSource byId =
                    new MapSqlParameterSource("id", active.id());

            if (active.attempts() >= MAX_ATTEMPTS) {
                jdbc.update("""
                        UPDATE verification_code
                        SET used_at = now()
                        WHERE id = :id
                        """, byId);
                return new CheckResult(null, TOO_MANY);
            }

            if (code == null
                    || !code.trim().matches("[0-9]{6}")
                    || !passwordEncoder.matches(
                    code.trim(), active.hash())) {

                jdbc.update("""
                        UPDATE verification_code
                        SET attempts = attempts + 1,
                            used_at = CASE
                                WHEN attempts + 1 >= :max THEN now()
                                ELSE used_at
                            END
                        WHERE id = :id
                        """, byId.addValue("max", MAX_ATTEMPTS));

                return new CheckResult(
                        null,
                        active.attempts() + 1 >= MAX_ATTEMPTS
                                ? TOO_MANY : INVALID);
            }

            return new CheckResult(active.id(), null);
        });

        if (result == null || result.error() != null) {
            throw new IllegalArgumentException(
                    result == null ? INVALID : result.error());
        }

        int consumed = jdbc.update("""
                UPDATE verification_code
                SET used_at = now()
                WHERE id = :id
                  AND used_at IS NULL
                  AND expires_at > now()
                  AND attempts < :max
                """, new MapSqlParameterSource("id", result.id())
                .addValue("max", MAX_ATTEMPTS));

        if (consumed != 1) {
            throw new IllegalArgumentException(INVALID);
        }
    }

    private int countCodes(
            String email,
            VerificationPurpose purpose,
            int lastSeconds) {

        Integer count = jdbc.queryForObject("""
                SELECT count(*)
                FROM verification_code
                WHERE email = :email
                  AND purpose = :purpose
                  AND created_at >
                      now() - (:seconds * interval '1 second')
                """, params(email, purpose)
                .addValue("seconds", lastSeconds), Integer.class);

        return count == null ? 0 : count;
    }

    private static MapSqlParameterSource params(
            String email,
            VerificationPurpose purpose) {

        return new MapSqlParameterSource()
                .addValue("email", email)
                .addValue("purpose", purpose.name());
    }

    private static String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email obrigatório.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}