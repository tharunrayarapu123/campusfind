package com.campusfind.config;

import com.campusfind.security.WebSocketAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @org.springframework.beans.factory.annotation.Value("${app.cors.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = java.util.Arrays.stream(frontendUrl.split(","))
                .map(String::trim)
                .map(url -> url.replaceAll("/+$", ""))
                .filter(url -> !url.isEmpty())
                .toArray(String[]::new);

        String[] allowedPatterns = (origins.length > 0) ? origins : new String[]{"*"};

        // SockJS fallback endpoint (supports HTTPS behind proxies)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedPatterns)
                .withSockJS();

        // Direct WebSocket endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedPatterns);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthChannelInterceptor);
    }
}
