package com.ridex.tracking.config;

import com.ridex.tracking.security.StompAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket setup for live ride tracking.
 *
 * - Clients connect to the "/ws" endpoint.
 * - Messages the client sends are prefixed "/app" and routed to @MessageMapping methods.
 * - The server broadcasts on "/topic/..." which subscribed clients receive.
 *
 * Auth is enforced on the STOMP CONNECT frame by {@link StompAuthInterceptor}
 * (the HTTP handshake itself is permitted in SecurityConfig).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;

    public WebSocketConfig(StompAuthInterceptor stompAuthInterceptor) {
        this.stompAuthInterceptor = stompAuthInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Raw WebSocket endpoint. SockJS fallback added for browser clients.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple in-memory broker for /topic destinations.
        registry.enableSimpleBroker("/topic");
        // Client -> server messages are prefixed with /app.
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Authenticate the STOMP CONNECT frame via JWT.
        registration.interceptors(stompAuthInterceptor);
    }
}
