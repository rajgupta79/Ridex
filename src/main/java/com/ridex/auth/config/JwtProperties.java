package com.ridex.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code ridex.jwt.*} block from application.yaml.
 */
@ConfigurationProperties(prefix = "ridex.jwt")
public record JwtProperties(
        String secret,
        long expirationMs,
        String issuer
) {}
