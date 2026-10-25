package com.ridex.driver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Per-driver operational state, separate from the auth {@link com.ridex.auth.model.User}.
 * Keyed by the user's id so there is exactly one profile per driver account.
 *
 * The {@code location} field is a GeoJSON Point with a 2dsphere index, which is
 * what makes "nearest drivers within X km" queries fast.
 */
@Document(collection = "driver_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverProfile {

    /** Same value as the owning User's id (one profile per driver). */
    @Id
    private String id;

    @Indexed
    private String userId;

    /**
     * Current position as GeoJSON [longitude, latitude].
     * The 2dsphere index powers $near / $geoNear queries on a sphere (real Earth).
     */
    @GeoSpatialIndexed(name = "location_2dsphere", type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    /**
     * Whether the driver is online and free to receive ride offers.
     * Combined with approval (on the User) to decide eligibility.
     */
    @Builder.Default
    private DriverStatus status = DriverStatus.OFFLINE;

    /** Simple 0-5 rating, used later for distance+rating matching. */
    @Builder.Default
    private double rating = 5.0;

    /** Id of the ride the driver is currently serving, if any. */
    private String currentRideId;

    private Instant lastLocationUpdate;
}
