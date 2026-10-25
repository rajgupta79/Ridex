package com.ridex.driver.dto;

/**
 * A driver returned by a proximity search, together with the straight-line
 * distance (in km) from the query point.
 */
public record NearbyDriver(
        String userId,
        double longitude,
        double latitude,
        double rating,
        double distanceKm
) {}
