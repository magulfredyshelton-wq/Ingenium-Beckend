package co.mz.ingenium.config.chatConfig;

import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.repository.UserRepository.UserRepository;
import co.mz.ingenium.service.JWTService.JWTService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.*;

import java.security.Principal;
import java.util.Set;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private static final Logger log =
            LoggerFactory.getLogger(WebSocketConfig.class);

    private static final Set<String> SEND_DESTINATIONS = Set.of(
            "/app/chat.send",
            "/app/chat.activity",
            "/app/call.invite",
            "/app/call.accept",
            "/app/call.reject",
            "/app/call.cancel");

    private static final Set<String> SUBSCRIBE_DESTINATIONS = Set.of(
            "/user/queue/messages",
            "/user/queue/notifications",
            "/user/queue/calls",
            "/user/queue/chat.activity",
            "/user/queue/chat.events");

    private final JWTService jwtService;
    private final UserRepository userRepository;

    public WebSocketConfig(
            JWTService jwtService,
            UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(
                    Message<?> message,
                    MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(
                                message,
                                StompHeaderAccessor.class);

                if (accessor == null) return message;

                if (StompCommand.CONNECT.equals(
                        accessor.getCommand())) {
                    authenticate(message, accessor);
                } else if (
                        StompCommand.SEND.equals(
                                accessor.getCommand())
                                || StompCommand.SUBSCRIBE.equals(
                                accessor.getCommand())) {
                    if (accessor.getUser() == null) {
                        throw reject(
                                message,
                                "AUTH_MISSING",
                                "Entra para usar mensagens e notificações.");
                    }

                    String destination =
                            accessor.getDestination();

                    Set<String> allowed =
                            StompCommand.SEND.equals(
                                    accessor.getCommand())
                                    ? SEND_DESTINATIONS
                                    : SUBSCRIBE_DESTINATIONS;

                    if (destination == null
                            || !allowed.contains(destination)) {
                        throw reject(
                                message,
                                "DESTINATION_FORBIDDEN",
                                "Destino de mensagens não permitido.");
                    }
                }

                return message;
            }
        });
    }

    private void authenticate(
            Message<?> message,
            StompHeaderAccessor accessor) {
        String authHeader =
                accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null
                || !authHeader.regionMatches(
                true, 0, "Bearer ", 0, 7)) {
            throw reject(
                    message,
                    "AUTH_MISSING",
                    "Token de acesso em falta. Entra novamente.");
        }

        String token =
                authHeader.substring(7).trim();

        if (token.isEmpty()) {
            throw reject(
                    message,
                    "AUTH_MISSING",
                    "Token de acesso em falta. Entra novamente.");
        }

        String email;

        try {
            email = jwtService.extrairEmail(token);
        } catch (ExpiredJwtException e) {
            throw reject(
                    message,
                    "AUTH_EXPIRED",
                    "A tua sessão expirou. Entra novamente.");
        } catch (WeakKeyException e) {
            log.error(
                    "A chave JWT do servidor não cumpre "
                            + "os requisitos de assinatura",
                    e);

            throw reject(
                    message,
                    "SERVER_AUTH_ERROR",
                    "O servidor não conseguiu validar a sessão.");
        } catch (JwtException | IllegalArgumentException e) {
            throw reject(
                    message,
                    "AUTH_INVALID",
                    "Token de acesso inválido. Entra novamente.");
        } catch (RuntimeException e) {
            log.error(
                    "Falha ao verificar o JWT da sessão STOMP {}",
                    accessor.getSessionId(),
                    e);

            throw reject(
                    message,
                    "SERVER_AUTH_ERROR",
                    "Não foi possível validar a sessão. Tenta novamente.");
        }

        if (email == null || email.isBlank()) {
            throw reject(
                    message,
                    "AUTH_INVALID",
                    "Token de acesso sem utilizador. Entra novamente.");
        }

        User user;

        try {
            user = userRepository.findByEmail(email)
                    .orElse(null);
        } catch (RuntimeException e) {
            log.error(
                    "Falha ao consultar o utilizador da sessão STOMP {}",
                    accessor.getSessionId(),
                    e);

            throw reject(
                    message,
                    "SERVER_AUTH_ERROR",
                    "Não foi possível consultar a sessão. Tenta novamente.");
        }

        if (user == null || user.getId() == null) {
            throw reject(
                    message,
                    "AUTH_USER_NOT_FOUND",
                    "Utilizador não encontrado. Entra novamente.");
        }

        Principal principal =
                () -> user.getId().toString();

        accessor.setUser(principal);
    }

    private static MessageDeliveryException reject(
            Message<?> message,
            String code,
            String detail) {
        return new MessageDeliveryException(
                message,
                code + ": " + detail);
    }
}