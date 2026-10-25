package com.ridex.matching.dto;

/**
 * Outcome of trying to pool a ride.
 */
public record PoolResult(
        String groupId,
        boolean joinedExisting,
        int ridersInGroup,
        double detourKm,
        double soloFare,
        double pooledFare
) {}
