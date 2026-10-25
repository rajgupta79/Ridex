package com.ridex.ride.service;

import com.ridex.common.GeoUtils;
import com.ridex.driver.service.DriverService;
import com.ridex.ride.config.PricingProperties;
import org.springframework.stereotype.Service;

/**
 * Computes fares. Formula:
 *   total = (baseFare + perKm * distanceKm) * surgeMultiplier
 *
 * Surge kicks in when supply is scarce: if the number of available drivers near
 * the pickup is below a threshold, a multiplier is applied. This is a simple,
 * explainable version of real surge pricing (which also considers demand).
 */
@Service
public class PricingService {

    private final PricingProperties props;
    private final DriverService driverService;

    public PricingService(PricingProperties props, DriverService driverService) {
        this.props = props;
        this.driverService = driverService;
    }

    /**
     * Full quote for a pickup→dropoff trip, including surge based on how many
     * drivers are currently available near the pickup.
     */
    public FareQuoteResult quote(double pickupLng, double pickupLat,
                                 double dropoffLng, double dropoffLat) {
        double distanceKm = GeoUtils.haversineKm(pickupLng, pickupLat, dropoffLng, dropoffLat);
        int nearby = driverService.findNearby(pickupLng, pickupLat, null, null).size();
        double surge = nearby < props.surgeThresholdDrivers() ? props.surgeMultiplier() : 1.0;
        return compute(distanceKm, nearby, surge);
    }

    /** Fare for a known distance, without surge (e.g. a fixed per-leg charge). */
    public double flatFare(double distanceKm) {
        return round2(props.baseFare() + props.perKm() * distanceKm);
    }

    private FareQuoteResult compute(double distanceKm, int nearby, double surge) {
        double distanceFare = props.perKm() * distanceKm;
        double total = (props.baseFare() + distanceFare) * surge;
        return new FareQuoteResult(
                round2(distanceKm), round2(props.baseFare()), round2(distanceFare),
                surge, nearby, round2(total));
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Internal carrier so the service doesn't depend on the DTO package shape. */
    public record FareQuoteResult(
            double distanceKm, double baseFare, double distanceFare,
            double surgeMultiplier, int nearbyDrivers, double total) {}
}
