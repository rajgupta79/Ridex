package com.ridex.ride.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables pricing and pooling configuration properties.
 */
@Configuration
@EnableConfigurationProperties({PricingProperties.class, PoolingProperties.class})
public class RideConfig {
}
