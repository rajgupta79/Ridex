package com.ridex.driver.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the driver/matching configuration properties.
 */
@Configuration
@EnableConfigurationProperties(MatchingProperties.class)
public class DriverConfig {
}
