package com.ridex.driver.dto;

/**
 * Timing comparison for the nearest-driver query with vs. without the
 * 2dsphere index. Times are averaged over several runs.
 */
public record BenchmarkResult(
        long totalDrivers,
        double radiusKm,
        int runs,
        int withIndexMatches,
        double withIndexAvgMs,
        int withoutIndexMatches,
        double withoutIndexAvgMs,
        double speedup
) {}
