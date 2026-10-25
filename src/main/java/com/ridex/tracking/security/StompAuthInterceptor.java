package com.ridex.tracking.security;

import com.ridex.auth.model.Role;
import com.ridex.auth.model.User;
import com.ridex.auth.security.JwtService;
import com.ridex.auth.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Authenticates the STOMP CONNECT frame. The client sends the JWT in a
 * "Authorization: Bearer <token>" STOMP header on CONNECT; we validate it and
 * attach a UserPrincipal to the session, so later SEND/SUBSCRIBE frames know
 * who the user is.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public StompAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String auth = accessor.getFirstNativeHeader("Authorization");
            if (auth == null || !auth.startsWith(PREFIX)) {
                throw new IllegalArgumentException("Missing bearer token on STOMP CONNECT");
            }
            Claims claims = jwtService.parse(auth.substring(PREFIX.length()));
            User user = User.builder()
                    .id(jwtService.getUserId(claims))
                    .email(claims.get("email", String.class))
                    .role(Role.valueOf(jwtService.getRole(claims)))
                    .approved(Boolean.TRUE.equals(claims.get("approved", Boolean.class)))
                    .build();
            UserPrincipal principal = new UserPrincipal(user);
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            accessor.setUser(authentication);
        }
        return message;
    }
}
