package com.ridex.driver.dto;

import com.ridex.driver.model.DriverProfile;
import com.ridex.driver.model.DriverStatus;

import java.time.Instant;

/**
 * Safe projection of a driver profile for API responses.
 */
public record DriverView(
        String userId,
        DriverStatus status,
        Double longitude,
        Double latitude,
        double rating,
        String currentRideId,
        Instant lastLocationUpdate
) {
    public static DriverView from(DriverProfile p) {
        Double lng = p.getLocation() != null ? p.getLocation().getX() : null;
        Double lat = p.getLocation() != null ? p.getLocation().getY() : null;
        return new DriverView(
                p.getUserId(), p.getStatus(), lng, lat,
                p.getRating(), p.getCurrentRideId(), p.getLastLocationUpdate());
    }
}
