package com.ridex.driver.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * A driver's location ping. GeoJSON order is [longitude, latitude], so we name
 * the fields explicitly to avoid the classic lat/lng swap bug.
 */
public record LocationUpdateRequest(
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude
) {}
