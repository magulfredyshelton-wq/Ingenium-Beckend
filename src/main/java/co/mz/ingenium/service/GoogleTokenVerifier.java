package co.mz.ingenium.service;

import co.mz.ingenium.dto.GoogleOAuth2.GoogleUserDTO;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Locale;

@Service
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(
            @Value("${google.client-id:}") String clientId) {

        if (clientId.isBlank()) {
            throw new IllegalStateException("Configure google.client-id.");
        }

        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId.trim()))
                .build();
    }

    public GoogleUserDTO verify(String credential) {
        if (credential == null || credential.isBlank()) {
            throw new BadCredentialsException(
                    "Token do Google obrigatório.");
        }

        GoogleIdToken token;
        try {
            token = verifier.verify(credential);
        } catch (GeneralSecurityException
                 | IOException
                 | IllegalArgumentException e) {
            throw new BadCredentialsException(
                    "Não foi possível validar o token do Google.", e);
        }

        if (token == null) {
            throw new BadCredentialsException(
                    "Token do Google inválido ou expirado.");
        }

        GoogleIdToken.Payload payload = token.getPayload();
        String email = payload.getEmail();

        if (email == null
                || email.isBlank()
                || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new BadCredentialsException(
                    "O email desta conta Google não está verificado.");
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);
        String hostedDomain = payload.getHostedDomain();

        if (!normalizedEmail.endsWith("@gmail.com")
                && (hostedDomain == null || hostedDomain.isBlank())) {
            throw new BadCredentialsException(
                    "Para este endereço, entre com email e palavra-passe. "
                            + "O acesso Google exige Gmail ou Google Workspace.");
        }

        return new GoogleUserDTO(
                normalizedEmail,
                (String) payload.get("name"));
    }
}