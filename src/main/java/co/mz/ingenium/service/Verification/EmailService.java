package co.mz.ingenium.service.Verification;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Envio dos códigos de verificação.
 *
 * Ordem de preferência:
 *  1. API HTTP do Brevo (porta 443) quando app.mail.brevo.api-key está definida.
 *     É a opção que funciona no Render grátis, que bloqueia SMTP.
 *  2. SMTP (JavaMailSender) quando não há chave do Brevo.
 *  3. Apenas log, se app.mail.log-codes=true (só em desenvolvimento).
 */
@Service
public class EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);

    private static final String BREVO_URL =
            "https://api.brevo.com/v3/smtp/email";

    private final ApplicationEventPublisher events;
    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;
    private final String brevoApiKey;
    private final boolean logCodes;
    private final RestClient brevoClient;

    public EmailService(
            ApplicationEventPublisher events,
            ObjectProvider<JavaMailSender> senders,
            @Value("${app.mail.from:${spring.mail.username:}}") String from,
            @Value("${app.mail.from-name:Ingenium}") String fromName,
            @Value("${app.mail.brevo.api-key:}") String brevoApiKey,
            @Value("${app.mail.log-codes:false}") boolean logCodes) {

        this.events = events;
        this.mailSender = senders.getIfAvailable();
        this.from = from;
        this.fromName = fromName;
        this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
        this.logCodes = logCodes;

        boolean usaBrevo = !this.brevoApiKey.isBlank();

        if (!logCodes && !usaBrevo && (mailSender == null || from.isBlank())) {
            throw new IllegalStateException(
                    "Configure app.mail.brevo.api-key (recomendado) ou "
                            + "spring.mail.host e app.mail.from para enviar os códigos. "
                            + "Apenas em desenvolvimento, pode usar app.mail.log-codes=true.");
        }

        if (usaBrevo && from.isBlank()) {
            throw new IllegalStateException(
                    "Defina app.mail.from com o remetente validado no Brevo.");
        }

        if (usaBrevo) {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            JdkClientHttpRequestFactory factory =
                    new JdkClientHttpRequestFactory(httpClient);
            factory.setReadTimeout(Duration.ofSeconds(15));
            this.brevoClient = RestClient.builder()
                    .requestFactory(factory)
                    .build();
            log.info("Envio de email: API do Brevo (HTTPS).");
        } else {
            this.brevoClient = null;
            log.info("Envio de email: {}",
                    logCodes ? "apenas log (desenvolvimento)" : "SMTP");
        }
    }

    public record CodeMail(
            String to,
            String code,
            VerificationPurpose purpose) {}

    public void sendRegistrationCode(String to, String code) {
        events.publishEvent(
                new CodeMail(to, code, VerificationPurpose.REGISTER));
    }

    public void sendPasswordResetCode(String to, String code) {
        events.publishEvent(
                new CodeMail(to, code, VerificationPurpose.PASSWORD_RESET));
    }

    @Async("mailExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void sendEmail(CodeMail event) {
        String subject;
        String text;

        if (event.purpose() == VerificationPurpose.REGISTER) {
            subject = "Ingenium: confirme o seu email";
            text = "O seu código de verificação é " + event.code() + ".\n\n"
                    + "Vale 10 minutos. Se não criou uma conta no Ingenium, "
                    + "ignore esta mensagem.";
        } else {
            subject = "Ingenium: recuperar a palavra-passe";
            text = "O seu código para criar uma nova palavra-passe é "
                    + event.code() + ".\n\n"
                    + "Vale 10 minutos. Se não pediu esta alteração, "
                    + "ignore esta mensagem: a sua palavra-passe não muda.";
        }

        if (logCodes) {
            log.warn(
                    "APENAS DESENVOLVIMENTO: email para {}: {}",
                    event.to(), text);
            return;
        }

        if (brevoClient != null) {
            enviarPorBrevo(event.to(), subject, text);
        } else {
            enviarPorSmtp(event.to(), subject, text);
        }
    }

    private void enviarPorBrevo(String to, String subject, String text) {
        try {
            Map<String, Object> body = Map.of(
                    "sender", Map.of("name", fromName, "email", from),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "textContent", text);

            brevoClient.post()
                    .uri(BREVO_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("api-key", brevoApiKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Email enviado para {} (Brevo).", to);
        } catch (RestClientException e) {
            log.error("Não foi possível enviar o email para {} (Brevo)", to, e);
        }
    }

    private void enviarPorSmtp(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email enviado para {} (SMTP).", to);
        } catch (MailException e) {
            log.error("Não foi possível enviar o email para {} (SMTP)", to, e);
        }
    }
}