package com.ridex.matching.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A shared trip: one cab carrying multiple rider legs. A new rider can join an
 * OPEN group if there's capacity, their pickup is near the group's anchor, and
 * the added detour stays under the cap.
 */
@Document(collection = "pool_groups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolGroup {

    @Id
    private String id;

    /** Anchor pickup — the first rider's pickup, used for proximity checks. */
    private GeoJsonPoint anchorPickup;

    @Indexed
    @Builder.Default
    private boolean open = true;

    @Builder.Default
    private List<PoolLeg> legs = new ArrayList<>();

    /** Sum of each leg's solo distance, for reference. */
    @Builder.Default
    private double totalSoloDistanceKm = 0.0;

    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * One rider's segment within a shared trip.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoolLeg {
        private String rideId;
        private String riderId;
        private GeoJsonPoint pickup;
        private GeoJsonPoint dropoff;
        private double soloDistanceKm;
        /** Discounted fare for sharing. */
        private double pooledFare;
    }
}
