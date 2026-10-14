package co.mz.ingenium.config.videocall;

import co.mz.ingenium.Controller.videocall.CallHandler;
import co.mz.ingenium.call.CallLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class CallWebSocketConfig implements WebSocketConfigurer {

    @Value("${videocall.max-participants:6}")
    private int maxParticipants;

    @Value("${videocall.allowed-origins:*}")
    private String[] allowedOrigins;

    private final CallAuthInterceptor authInterceptor;
    private final CallAccessService accessService;
    private final CallLogService callLogService;

    public CallWebSocketConfig(CallAuthInterceptor authInterceptor, CallAccessService accessService,
                               CallLogService callLogService) {
        this.authInterceptor = authInterceptor;
        this.accessService = accessService;
        this.callLogService = callLogService;
    }

    @Bean
    public CallHandler callHandler() {
        return new CallHandler(maxParticipants, accessService, callLogService);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(callHandler(), "/call")
                .addInterceptors(authInterceptor)
                .setAllowedOriginPatterns(allowedOrigins);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(64 * 1024);
        container.setMaxBinaryMessageBufferSize(64 * 1024);
        container.setMaxSessionIdleTimeout(5 * 60 * 1000L);
        return container;
    }
}
