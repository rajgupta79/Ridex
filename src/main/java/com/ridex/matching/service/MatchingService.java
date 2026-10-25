package com.ridex.matching.service;

import com.ridex.common.ApiException;
import com.ridex.driver.config.MatchingProperties;
import com.ridex.driver.dto.NearbyDriver;
import com.ridex.driver.service.DriverService;
import com.ridex.matching.model.OfferStatus;
import com.ridex.matching.model.RideOffer;
import com.ridex.matching.repository.RideOfferRepository;
import com.ridex.ride.model.Ride;
import com.ridex.ride.model.RideStatus;
import com.ridex.ride.service.RideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Offers a ride to the best nearby driver, one at a time. If the driver rejects
 * or lets the offer expire, matching automatically moves on to the next driver.
 *
 * Scoring: lower is better. score = distanceKm - (ratingWeight * rating).
 * So a slightly-farther but much-higher-rated driver can win — a simple
 * "distance plus rating" blend.
 */
@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    /** How many km of extra distance we're willing to trade for +1.0 rating. */
    private static final double RATING_WEIGHT_KM_PER_STAR = 0.5;

    private final RideService rideService;
    private final DriverService driverService;
    private final RideOfferRepository offers;
    private final MatchingProperties props;

    public MatchingService(RideService rideService,
                           DriverService driverService,
                           RideOfferRepository offers,
                           MatchingProperties props) {
        this.rideService = rideService;
        this.driverService = driverService;
        this.offers = offers;
        this.props = props;
    }

    /** Kicks off matching when a ride is requested (decoupled via event). */
    @Async
    @EventListener
    public void onRideRequested(com.ridex.ride.event.RideRequestedEvent event) {
        try {
            startMatching(event.rideId());
        } catch (Exception ex) {
            log.warn("Matching failed to start for ride {}: {}", event.rideId(), ex.getMessage());
        }
    }

    public Optional<RideOffer> startMatching(String rideId) {
        Ride ride = rideService.get(rideId);
        if (ride.getStatus() != RideStatus.REQUESTED) {
            return Optional.empty();
        }
        return offerNext(ride, new HashSet<>());
    }

    /**
     * Pick the best untried available driver within radius and create a PENDING
     * offer. Returns empty if no eligible driver remains (ride stays REQUESTED).
     */
    public Optional<RideOffer> offerNext(Ride ride, Set<String> alreadyTried) {
        double pickupLng = ride.getPickup().getX();
        double pickupLat = ride.getPickup().getY();

        List<NearbyDriver> candidates = driverService.findNearby(
                pickupLng, pickupLat, props.searchRadiusKm(), 25);

        Optional<NearbyDriver> best = candidates.stream()
                .filter(d -> !alreadyTried.contains(d.userId()))
                .min(Comparator.comparingDouble(this::score));

        if (best.isEmpty()) {
            log.info("No eligible driver for ride {} (tried {})", ride.getId(), alreadyTried.size());
            return Optional.empty();
        }

        NearbyDriver chosen = best.get();
        Set<String> tried = new HashSet<>(alreadyTried);
        tried.add(chosen.userId());

        RideOffer offer = RideOffer.builder()
                .rideId(ride.getId())
                .driverId(chosen.userId())
                .status(OfferStatus.PENDING)
                .triedDriverIds(tried)
                .distanceKm(chosen.distanceKm())
                .score(score(chosen))
                .offeredAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(props.offerTimeoutSeconds()))
                .build();
        offer = offers.save(offer);
        log.info("Offered ride {} to driver {} (distance {} km, score {})",
                ride.getId(), chosen.userId(),
                String.format("%.2f", chosen.distanceKm()), String.format("%.2f", offer.getScore()));
        return Optional.of(offer);
    }

    private double score(NearbyDriver d) {
        return d.distanceKm() - RATING_WEIGHT_KM_PER_STAR * d.rating();
    }

    // --- Driver responses ---

    public RideOffer acceptOffer(String driverId) {
        RideOffer offer = offers.findByDriverIdAndStatus(driverId, OfferStatus.PENDING)
                .orElseThrow(() -> ApiException.notFound("No pending offer for you"));

        if (offer.getExpiresAt().isBefore(Instant.now())) {
            offer.setStatus(OfferStatus.EXPIRED);
            offers.save(offer);
            throw ApiException.conflict("Offer expired");
        }

        // Delegate to the atomic accept; if it fails, the ride was taken/cancelled.
        rideService.acceptRide(driverId, offer.getRideId());

        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setRespondedAt(Instant.now());
        return offers.save(offer);
    }

    public void rejectOffer(String driverId) {
        RideOffer offer = offers.findByDriverIdAndStatus(driverId, OfferStatus.PENDING)
                .orElseThrow(() -> ApiException.notFound("No pending offer for you"));
        offer.setStatus(OfferStatus.REJECTED);
        offer.setRespondedAt(Instant.now());
        offers.save(offer);
        log.info("Driver {} rejected offer for ride {}", driverId, offer.getRideId());
        reOffer(offer);
    }

    public Optional<RideOffer> currentOfferForDriver(String driverId) {
        return offers.findByDriverIdAndStatus(driverId, OfferStatus.PENDING);
    }

    /** Offer the ride to the next driver, carrying forward the tried set. */
    private void reOffer(RideOffer previous) {
        Ride ride = rideService.get(previous.getRideId());
        if (ride.getStatus() != RideStatus.REQUESTED) {
            return; // already accepted or cancelled
        }
        offerNext(ride, previous.getTriedDriverIds());
    }
}
