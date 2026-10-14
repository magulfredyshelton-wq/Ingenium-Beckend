package co.mz.ingenium.service.Verification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);

    private final ApplicationEventPublisher events;
    private final JavaMailSender mailSender;
    private final String from;
    private final boolean logCodes;

    public EmailService(
            ApplicationEventPublisher events,
            ObjectProvider<JavaMailSender> senders,
            @Value("${app.mail.from:${spring.mail.username:}}") String from,
            @Value("${app.mail.log-codes:false}") boolean logCodes) {

        this.events = events;
        this.mailSender = senders.getIfAvailable();
        this.from = from;
        this.logCodes = logCodes;

        if (!logCodes && (mailSender == null || from.isBlank())) {
            throw new IllegalStateException(
                    "Configure spring.mail.host e app.mail.from para enviar os códigos. "
                            + "Apenas em desenvolvimento, pode usar app.mail.log-codes=true.");
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

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(event.to());
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (MailException e) {
            log.error(
                    "Não foi possível enviar o email para {}",
                    event.to(), e);
        }
    }
}