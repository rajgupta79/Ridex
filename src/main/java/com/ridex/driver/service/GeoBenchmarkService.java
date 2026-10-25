package com.ridex.driver.service;

import com.ridex.common.GeoUtils;
import com.ridex.driver.dto.BenchmarkResult;
import com.ridex.driver.model.DriverProfile;
import com.ridex.driver.model.DriverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Measures the nearest-driver query time WITH the 2dsphere index versus
 * WITHOUT it, so we get an honest, self-measured number.
 *
 * - WITH index: a geoNear aggregation ($near on a sphere), which the index serves.
 * - WITHOUT index: drop the geo index, then run an equivalent query that Mongo
 *   can only answer with a full collection scan + per-document distance filter.
 *   (A $near query itself REQUIRES a geo index, so the honest "no index"
 *   baseline is the scan you'd be forced into without one.)
 *
 * The index is always recreated afterwards.
 */
@Service
public class GeoBenchmarkService {

    private static final Logger log = LoggerFactory.getLogger(GeoBenchmarkService.class);
    /** The field the 2dsphere index is built on. */
    private static final String GEO_FIELD = "location";
    /** Name we use when (re)creating the index. */
    private static final String GEO_INDEX_NAME = "location_2dsphere";

    private final MongoTemplate mongo;

    public GeoBenchmarkService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public BenchmarkResult run(double lng, double lat, double radiusKm, int runs) {
        long total = mongo.count(new Query(), DriverProfile.class);
        if (runs < 1) runs = 1;

        // --- WITH INDEX ---
        ensureGeoIndex();
        int withMatches = 0;
        long withNanos = 0;
        for (int i = 0; i < runs; i++) {
            long t0 = System.nanoTime();
            List<DriverProfile> res = nearWithIndex(lng, lat, radiusKm);
            withNanos += System.nanoTime() - t0;
            withMatches = res.size();
        }

        // --- WITHOUT INDEX (full scan + haversine filter) ---
        dropGeoIndex();
        int withoutMatches = 0;
        long withoutNanos = 0;
        try {
            for (int i = 0; i < runs; i++) {
                long t0 = System.nanoTime();
                int c = scanWithoutIndex(lng, lat, radiusKm);
                withoutNanos += System.nanoTime() - t0;
                withoutMatches = c;
            }
        } finally {
            // Always restore the index, even if the scan throws.
            ensureGeoIndex();
        }

        double withAvgMs = (withNanos / (double) runs) / 1_000_000.0;
        double withoutAvgMs = (withoutNanos / (double) runs) / 1_000_000.0;
        double speedup = withAvgMs > 0 ? withoutAvgMs / withAvgMs : 0;

        log.info("Geo benchmark: {} drivers, radius {} km, {} runs -> indexed {} ms, scan {} ms ({}x)",
                total, radiusKm, runs, String.format("%.2f", withAvgMs),
                String.format("%.2f", withoutAvgMs), String.format("%.1f", speedup));

        return new BenchmarkResult(total, radiusKm, runs,
                withMatches, round2(withAvgMs),
                withoutMatches, round2(withoutAvgMs),
                round2(speedup));
    }

    /** Index-served proximity query (geoNear on a sphere). */
    private List<DriverProfile> nearWithIndex(double lng, double lat, double radiusKm) {
        NearQuery near = NearQuery.near(new Point(lng, lat), Metrics.KILOMETERS)
                .maxDistance(new Distance(radiusKm, Metrics.KILOMETERS))
                .spherical(true);
        near.query(Query.query(Criteria.where("status").is(DriverStatus.AVAILABLE.name())));
        return mongo.geoNear(near, DriverProfile.class).getContent().stream()
                .map(r -> r.getContent())
                .toList();
    }

    /**
     * No-index baseline: Mongo must scan every document. We stream all available
     * drivers and filter by haversine distance in the app, which is the cost you
     * pay when there is no geospatial index to prune the search.
     */
    private int scanWithoutIndex(double lng, double lat, double radiusKm) {
        List<DriverProfile> all = mongo.find(
                Query.query(Criteria.where("status").is(DriverStatus.AVAILABLE.name())),
                DriverProfile.class);
        int matches = 0;
        for (DriverProfile d : all) {
            if (d.getLocation() == null) continue;
            double km = GeoUtils.haversineKm(lng, lat, d.getLocation().getX(), d.getLocation().getY());
            if (km <= radiusKm) matches++;
        }
        return matches;
    }

    /** Name of the existing 2dsphere index on the location field, if any. */
    private java.util.Optional<String> existingGeoIndexName() {
        return mongo.indexOps(DriverProfile.class).getIndexInfo().stream()
                .filter(ix -> ix.getIndexFields().stream()
                        .anyMatch(f -> GEO_FIELD.equals(f.getKey())))
                .map(ix -> ix.getName())
                .findFirst();
    }

    private void ensureGeoIndex() {
        if (existingGeoIndexName().isEmpty()) {
            mongo.indexOps(DriverProfile.class)
                    .createIndex(new GeospatialIndex(GEO_FIELD)
                            .typed(org.springframework.data.mongodb.core.index.GeoSpatialIndexType.GEO_2DSPHERE)
                            .named(GEO_INDEX_NAME));
            log.info("Created geo index on '{}'", GEO_FIELD);
        }
    }

    private void dropGeoIndex() {
        existingGeoIndexName().ifPresent(name -> {
            mongo.indexOps(DriverProfile.class).dropIndex(name);
            log.info("Dropped geo index '{}'", name);
        });
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
