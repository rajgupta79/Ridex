package com.ridex.ride.dto;

import com.ridex.ride.model.Ride;
import com.ridex.ride.model.RideStatus;

import java.time.Instant;

/**
 * API projection of a ride.
 */
public record RideView(
        String id,
        String riderId,
        String driverId,
        RideStatus status,
        Double pickupLng,
        Double pickupLat,
        Double dropoffLng,
        Double dropoffLat,
        double estimatedDistanceKm,
        Double fare,
        String cancelReason,
        Instant requestedAt,
        Instant acceptedAt,
        Instant startedAt,
        Instant completedAt,
        Instant cancelledAt
) {
    public static RideView from(Ride r) {
        return new RideView(
                r.getId(), r.getRiderId(), r.getDriverId(), r.getStatus(),
                r.getPickup() != null ? r.getPickup().getX() : null,
                r.getPickup() != null ? r.getPickup().getY() : null,
                r.getDropoff() != null ? r.getDropoff().getX() : null,
                r.getDropoff() != null ? r.getDropoff().getY() : null,
                r.getEstimatedDistanceKm(), r.getFare(), r.getCancelReason(),
                r.getRequestedAt(), r.getAcceptedAt(), r.getStartedAt(),
                r.getCompletedAt(), r.getCancelledAt());
    }
}
