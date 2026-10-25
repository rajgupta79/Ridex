package com.ridex.matching.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables the scheduled offer-expiry sweep and async event handling for matching.
 */
@Configuration
@EnableScheduling
@EnableAsync
public class MatchingConfig {
}
