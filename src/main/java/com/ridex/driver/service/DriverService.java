package com.ridex.driver.service;

import com.ridex.auth.model.Role;
import com.ridex.auth.model.User;
import com.ridex.auth.repository.UserRepository;
import com.ridex.common.ApiException;
import com.ridex.driver.config.MatchingProperties;
import com.ridex.driver.dto.DriverView;
import com.ridex.driver.dto.LocationUpdateRequest;
import com.ridex.driver.dto.NearbyDriver;
import com.ridex.driver.model.DriverProfile;
import com.ridex.driver.model.DriverStatus;
import com.ridex.driver.repository.DriverProfileRepository;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Driver operational logic: profile lookup, going online/offline, location
 * updates, and the "nearest available drivers" proximity search.
 */
@Service
public class DriverService {

    private final DriverProfileRepository profiles;
    private final UserRepository users;
    private final MatchingProperties matchingProps;
    private final MongoTemplate mongo;

    public DriverService(DriverProfileRepository profiles,
                         UserRepository users,
                         MatchingProperties matchingProps,
                         MongoTemplate mongo) {
        this.profiles = profiles;
        this.users = users;
        this.matchingProps = matchingProps;
        this.mongo = mongo;
    }

    /** Fetch (or lazily create) the profile for an approved driver. */
    public DriverProfile requireProfile(String userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (user.getRole() != Role.DRIVER) {
            throw ApiException.forbidden("Only drivers have a driver profile");
        }
        if (!user.isApproved()) {
            throw ApiException.forbidden("Driver is not approved yet");
        }
        return profiles.findByUserId(userId)
                .orElseGet(() -> profiles.save(DriverProfile.builder().userId(userId).build()));
    }

    public DriverView updateLocation(String userId, LocationUpdateRequest req) {
        DriverProfile profile = requireProfile(userId);
        profile.setLocation(new GeoJsonPoint(req.longitude(), req.latitude()));
        profile.setLastLocationUpdate(Instant.now());
        // First location ping also brings an OFFLINE driver online.
        if (profile.getStatus() == DriverStatus.OFFLINE) {
            profile.setStatus(DriverStatus.AVAILABLE);
        }
        return DriverView.from(profiles.save(profile));
    }

    public DriverView setStatus(String userId, DriverStatus status) {
        DriverProfile profile = requireProfile(userId);
        if (status == DriverStatus.AVAILABLE && profile.getLocation() == null) {
            throw ApiException.badRequest("Set your location before going online");
        }
        profile.setStatus(status);
        return DriverView.from(profiles.save(profile));
    }

    public DriverView getMyProfile(String userId) {
        return DriverView.from(requireProfile(userId));
    }

    /**
     * Nearest AVAILABLE drivers to a point. Uses the default configured radius
     * when {@code radiusKm} is null. Backed by the 2dsphere index via the
     * repository's geo-near derived query.
     */
    public List<NearbyDriver> findNearby(double longitude, double latitude, Double radiusKm, Integer limit) {
        double radius = radiusKm != null ? radiusKm : matchingProps.searchRadiusKm();

        // Use a geoNear aggregation (not a plain find-with-$near): $near needs to
        // be the sort-driving operation, which geoNear provides. This is served by
        // the 2dsphere index. The extra status filter is applied inside the query.
        NearQuery near = NearQuery.near(new Point(longitude, latitude), Metrics.KILOMETERS)
                .maxDistance(new Distance(radius, Metrics.KILOMETERS))
                .spherical(true)
                .query(Query.query(Criteria.where("status").is(DriverStatus.AVAILABLE.name())));
        if (limit != null && limit > 0) {
            near = near.limit(limit);
        }

        return mongo.geoNear(near, DriverProfile.class).getContent().stream()
                .map(this::toNearby)
                .toList();
    }

    private NearbyDriver toNearby(GeoResult<DriverProfile> r) {
        DriverProfile d = r.getContent();
        return new NearbyDriver(
                d.getUserId(),
                d.getLocation().getX(),
                d.getLocation().getY(),
                d.getRating(),
                // GeoResults reports distance in the metric we queried with (km).
                r.getDistance().getValue());
    }
}
