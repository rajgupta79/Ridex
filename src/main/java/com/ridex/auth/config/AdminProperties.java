package com.ridex.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code ridex.admin.*} — the bootstrap admin credentials.
 */
@ConfigurationProperties(prefix = "ridex.admin")
public record AdminProperties(
        String bootstrapEmail,
        String bootstrapPassword
) {}
