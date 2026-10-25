package com.ridex.ride.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * A rider's request for a ride: pickup and dropoff as [lng, lat].
 */
public record RideRequest(
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double pickupLng,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double pickupLat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double dropoffLng,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double dropoffLat
) {}
