package com.ridex.driver.service;

import com.ridex.driver.model.DriverProfile;
import com.ridex.driver.model.DriverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Seeds fake AVAILABLE drivers for benchmarking the proximity query.
 * Uses unordered bulk inserts for speed (50k single saves would be very slow).
 *
 * These are synthetic rows (userId prefixed "seed-") so they can be wiped
 * without touching real driver accounts.
 */
@Service
public class DriverSeeder {

    private static final Logger log = LoggerFactory.getLogger(DriverSeeder.class);
    public static final String SEED_PREFIX = "seed-";
    private static final int BATCH = 5_000;

    private final MongoTemplate mongo;

    public DriverSeeder(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /**
     * Insert {@code count} drivers scattered uniformly in a bounding box around
     * (centerLng, centerLat). spanDeg is the half-width of the box in degrees
     * (~111 km per degree of latitude), so 0.5 ≈ a ~110 km-wide metro area.
     */
    public long seed(int count, double centerLng, double centerLat, double spanDeg) {
        long start = System.currentTimeMillis();
        List<DriverProfile> batch = new ArrayList<>(BATCH);
        long inserted = 0;

        for (int i = 0; i < count; i++) {
            double lng = centerLng + ThreadLocalRandom.current().nextDouble(-spanDeg, spanDeg);
            double lat = centerLat + ThreadLocalRandom.current().nextDouble(-spanDeg, spanDeg);
            DriverProfile p = DriverProfile.builder()
                    .userId(SEED_PREFIX + i)
                    .location(new GeoJsonPoint(lng, lat))
                    .status(DriverStatus.AVAILABLE)
                    .rating(3.5 + ThreadLocalRandom.current().nextDouble(0, 1.5))
                    .lastLocationUpdate(Instant.now())
                    .build();
            batch.add(p);

            if (batch.size() == BATCH) {
                mongo.insert(batch, DriverProfile.class);
                inserted += batch.size();
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            mongo.insert(batch, DriverProfile.class);
            inserted += batch.size();
        }

        log.info("Seeded {} drivers in {} ms", inserted, System.currentTimeMillis() - start);
        return inserted;
    }

    /** Remove only the synthetic seed drivers. */
    public long clearSeedData() {
        var query = org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("userId").regex("^" + SEED_PREFIX));
        long before = mongo.count(query, DriverProfile.class);
        mongo.remove(query, DriverProfile.class);
        return before;
    }
}
