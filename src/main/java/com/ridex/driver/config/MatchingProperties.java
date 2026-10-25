package com.ridex.driver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code ridex.matching.*}. Lives in the driver package because the
 * nearby-search radius is used here first; the matching package reuses it in Phase 4.
 */
@ConfigurationProperties(prefix = "ridex.matching")
public record MatchingProperties(
        double searchRadiusKm,
        int offerTimeoutSeconds
) {}
