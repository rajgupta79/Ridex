package com.ridex.matching.dto;

import com.ridex.matching.model.OfferStatus;
import com.ridex.matching.model.RideOffer;

import java.time.Instant;

/**
 * What a driver sees for a pending offer.
 */
public record OfferView(
        String offerId,
        String rideId,
        String driverId,
        OfferStatus status,
        double distanceKm,
        double score,
        Instant offeredAt,
        Instant expiresAt
) {
    public static OfferView from(RideOffer o) {
        return new OfferView(o.getId(), o.getRideId(), o.getDriverId(), o.getStatus(),
                o.getDistanceKm(), o.getScore(), o.getOfferedAt(), o.getExpiresAt());
    }
}
