package com.ridex.matching.service;

import com.ridex.common.GeoUtils;
import com.ridex.matching.dto.PoolResult;
import com.ridex.matching.model.PoolGroup;
import com.ridex.matching.repository.PoolGroupRepository;
import com.ridex.ride.config.PoolingProperties;
import com.ridex.ride.model.Ride;
import com.ridex.ride.service.PricingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Basic cab sharing. When a rider opts into pooling, we try to attach them to
 * an existing open group whose cab has capacity and whose route the new rider
 * only mildly lengthens (detour under the configured cap). Otherwise we open a
 * new group.
 *
 * Detour measure (triangle inequality): how much farther the cab travels by
 * picking up the new rider on the way, versus not:
 *   detour = d(anchorPickup, newPickup) + d(newPickup, newDropoff) - d(anchorPickup, newDropoff)
 *
 * Shared riders get a discount since they're splitting the cab.
 */
@Service
public class PoolingService {

    private static final Logger log = LoggerFactory.getLogger(PoolingService.class);

    /** Fraction off the solo fare for agreeing to share. */
    private static final double POOL_DISCOUNT = 0.25;

    private final PoolGroupRepository groups;
    private final PoolingProperties props;
    private final PricingService pricingService;

    public PoolingService(PoolGroupRepository groups,
                          PoolingProperties props,
                          PricingService pricingService) {
        this.groups = groups;
        this.props = props;
        this.pricingService = pricingService;
    }

    /**
     * Try to pool the given ride. Joins the best compatible open group, or
     * creates a new group if none fits.
     */
    public PoolResult pool(Ride ride) {
        double soloFare = pricingService.flatFare(ride.getEstimatedDistanceKm());
        double pooledFare = round2(soloFare * (1 - POOL_DISCOUNT));
        GeoJsonPoint pickup = ride.getPickup();
        GeoJsonPoint dropoff = ride.getDropoff();

        // Find the best open group this rider can join (smallest detour).
        PoolGroup bestGroup = null;
        double bestDetour = Double.MAX_VALUE;

        for (PoolGroup g : groups.findByOpenTrue()) {
            if (g.getLegs().size() >= props.maxRidersPerCab()) {
                continue; // cab full
            }
            // Gate 1: the new pickup must be near the group's anchor pickup.
            // Riders are only pooled if they're starting from roughly the same area.
            double pickupGap = GeoUtils.haversineKm(
                    g.getAnchorPickup().getX(), g.getAnchorPickup().getY(),
                    pickup.getX(), pickup.getY());
            if (pickupGap > props.maxDetourKm()) {
                continue;
            }
            // Gate 2: the added detour to insert this leg stays under the cap.
            double detour = detourKm(g.getAnchorPickup(), pickup, dropoff);
            if (detour <= props.maxDetourKm() && detour < bestDetour) {
                bestDetour = detour;
                bestGroup = g;
            }
        }

        if (bestGroup != null) {
            bestGroup.getLegs().add(buildLeg(ride, pooledFare));
            bestGroup.setTotalSoloDistanceKm(
                    bestGroup.getTotalSoloDistanceKm() + ride.getEstimatedDistanceKm());
            if (bestGroup.getLegs().size() >= props.maxRidersPerCab()) {
                bestGroup.setOpen(false); // full now
            }
            groups.save(bestGroup);
            log.info("Ride {} joined pool {} (detour {} km, {} riders)",
                    ride.getId(), bestGroup.getId(), String.format("%.2f", bestDetour),
                    bestGroup.getLegs().size());
            return new PoolResult(bestGroup.getId(), true, bestGroup.getLegs().size(),
                    round2(bestDetour), soloFare, pooledFare);
        }

        // No compatible group: open a new one anchored at this pickup.
        PoolGroup fresh = PoolGroup.builder()
                .anchorPickup(pickup)
                .open(true)
                .totalSoloDistanceKm(ride.getEstimatedDistanceKm())
                .build();
        fresh.getLegs().add(buildLeg(ride, pooledFare));
        fresh = groups.save(fresh);
        log.info("Ride {} opened new pool {}", ride.getId(), fresh.getId());
        return new PoolResult(fresh.getId(), false, 1, 0.0, soloFare, pooledFare);
    }

    public Optional<PoolGroup> getGroup(String groupId) {
        return groups.findById(groupId);
    }

    private PoolGroup.PoolLeg buildLeg(Ride ride, double pooledFare) {
        return PoolGroup.PoolLeg.builder()
                .rideId(ride.getId())
                .riderId(ride.getRiderId())
                .pickup(ride.getPickup())
                .dropoff(ride.getDropoff())
                .soloDistanceKm(ride.getEstimatedDistanceKm())
                .pooledFare(pooledFare)
                .build();
    }

    /** Extra distance the cab travels to insert this rider's leg. */
    private double detourKm(GeoJsonPoint anchor, GeoJsonPoint pickup, GeoJsonPoint dropoff) {
        double a2p = GeoUtils.haversineKm(anchor.getX(), anchor.getY(), pickup.getX(), pickup.getY());
        double p2d = GeoUtils.haversineKm(pickup.getX(), pickup.getY(), dropoff.getX(), dropoff.getY());
        double a2d = GeoUtils.haversineKm(anchor.getX(), anchor.getY(), dropoff.getX(), dropoff.getY());
        return Math.max(0.0, a2p + p2d - a2d);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
