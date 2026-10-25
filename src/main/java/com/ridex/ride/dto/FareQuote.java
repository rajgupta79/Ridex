package com.ridex.ride.dto;

/**
 * A transparent fare breakdown. Keeping the parts visible makes the surge
 * logic easy to explain and easy to show in a UI.
 */
public record FareQuote(
        double distanceKm,
        double baseFare,
        double distanceFare,
        double surgeMultiplier,
        int nearbyDrivers,
        double total
) {}
