package com.ridex.matching.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A pending offer of a ride to a single driver. Matching creates one at a time:
 * offer to the best driver, wait up to the timeout, and if it's not accepted,
 * expire it and offer to the next driver.
 */
@Document(collection = "ride_offers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideOffer {

    @Id
    private String id;

    @Indexed
    private String rideId;

    @Indexed
    private String driverId;

    @Indexed
    private OfferStatus status;

    /** Drivers already offered this ride (so we don't re-offer to the same one). */
    @Builder.Default
    private java.util.Set<String> triedDriverIds = new java.util.HashSet<>();

    private double distanceKm;
    private double score;

    @Builder.Default
    private Instant offeredAt = Instant.now();

    /** When this offer auto-expires if the driver hasn't responded. */
    @Indexed
    private Instant expiresAt;

    private Instant respondedAt;
}
