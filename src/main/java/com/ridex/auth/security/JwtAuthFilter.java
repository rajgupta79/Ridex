package com.ridex.auth.security;

import com.ridex.auth.model.Role;
import com.ridex.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request. If a valid "Authorization: Bearer <jwt>" header is
 * present, it builds an Authentication from the token's claims and stores it
 * in the SecurityContext. Invalid/absent tokens are simply ignored here —
 * authorization rules downstream decide whether that's allowed.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(PREFIX.length());
            try {
                Claims claims = jwtService.parse(token);
                UserPrincipal principal = principalFromClaims(claims);

                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                // Bad/expired token: leave the context unauthenticated.
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    /** Rebuild a lightweight principal from token claims (no DB round-trip). */
    private UserPrincipal principalFromClaims(Claims claims) {
        User user = User.builder()
                .id(jwtService.getUserId(claims))
                .email(claims.get("email", String.class))
                .role(Role.valueOf(jwtService.getRole(claims)))
                .approved(Boolean.TRUE.equals(claims.get("approved", Boolean.class)))
                .build();
        return new UserPrincipal(user);
    }
}
