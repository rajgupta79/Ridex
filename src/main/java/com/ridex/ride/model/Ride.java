package com.ridex.ride.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A single ride request and its lifecycle. Pickup/dropoff are GeoJSON points.
 * The {@code status} field drives the state machine; the atomic accept in
 * RideService guards against two drivers grabbing the same REQUESTED ride.
 */
@Document(collection = "rides")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ride {

    @Id
    private String id;

    @Indexed
    private String riderId;

    /** Null until a driver accepts. */
    @Indexed
    private String driverId;

    @Indexed
    @Builder.Default
    private RideStatus status = RideStatus.REQUESTED;

    private GeoJsonPoint pickup;
    private GeoJsonPoint dropoff;

    /** Straight-line distance pickup→dropoff, km (filled at request time). */
    private double estimatedDistanceKm;

    /** Quoted fare at request time (Phase 6 fills this in). */
    private Double fare;

    /** Set when the ride is cancelled, for a short audit trail. */
    private String cancelReason;

    @Builder.Default
    private Instant requestedAt = Instant.now();
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
}
