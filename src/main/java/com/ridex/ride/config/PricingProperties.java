package com.ridex.ride.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code ridex.pricing.*}.
 */
@ConfigurationProperties(prefix = "ridex.pricing")
public record PricingProperties(
        double baseFare,
        double perKm,
        int surgeThresholdDrivers,
        double surgeMultiplier
) {}
