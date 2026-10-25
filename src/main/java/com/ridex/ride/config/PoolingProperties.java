package com.ridex.ride.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code ridex.pooling.*}.
 */
@ConfigurationProperties(prefix = "ridex.pooling")
public record PoolingProperties(
        int maxRidersPerCab,
        double maxDetourKm
) {}
