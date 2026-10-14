package co.mz.ingenium.config.videocall;

import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.repository.UserRepository.UserRepository;
import co.mz.ingenium.service.JWTService.JWTService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Só deixa abrir a ligação /call quem traz um token válido (?token=...).
 * O browser não consegue pôr cabeçalhos num WebSocket, por isso o token vai no endereço.
 * Guarda o id e o nome do utilizador, lidos da base de dados, na sessão.
 */
@Component
public class CallAuthInterceptor implements HandshakeInterceptor {

    private final JWTService jwtService;
    private final UserRepository userRepository;

    public CallAuthInterceptor(JWTService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");

        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            String email = jwtService.extrairEmail(token);
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return false;
            }
            attributes.put("email", email);
            attributes.put("userId", user.getId().toString());
            attributes.put("name", user.getDisplayName());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}