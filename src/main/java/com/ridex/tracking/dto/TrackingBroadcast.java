package com.ridex.tracking.dto;

/**
 * What subscribers (the rider) receive on a ride's topic. Echoes the driver's
 * position plus timing fields so a test client can compute latency.
 */
public record TrackingBroadcast(
        String rideId,
        String driverId,
        double longitude,
        double latitude,
        long clientSentAt,
        long serverReceivedAt
) {}
