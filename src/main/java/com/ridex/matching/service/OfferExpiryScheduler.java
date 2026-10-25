package com.ridex.matching.service;

import com.ridex.matching.model.OfferStatus;
import com.ridex.matching.model.RideOffer;
import com.ridex.matching.repository.RideOfferRepository;
import com.ridex.ride.model.Ride;
import com.ridex.ride.model.RideStatus;
import com.ridex.ride.service.RideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Periodically expires offers the driver never answered, then re-offers the
 * ride to the next candidate. Runs every few seconds; the offer timeout itself
 * (default 15s) is configured via ridex.matching.offer-timeout-seconds.
 */
@Component
public class OfferExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(OfferExpiryScheduler.class);

    private final RideOfferRepository offers;
    private final RideService rideService;
    private final MatchingService matchingService;

    public OfferExpiryScheduler(RideOfferRepository offers,
                                RideService rideService,
                                MatchingService matchingService) {
        this.offers = offers;
        this.rideService = rideService;
        this.matchingService = matchingService;
    }

    /** Sweep every 3 seconds for PENDING offers whose deadline has passed. */
    @Scheduled(fixedDelay = 3000)
    public void expireStaleOffers() {
        List<RideOffer> stale = offers.findByStatusAndExpiresAtBefore(OfferStatus.PENDING, Instant.now());
        if (stale.isEmpty()) {
            return;
        }
        for (RideOffer offer : stale) {
            offer.setStatus(OfferStatus.EXPIRED);
            offer.setRespondedAt(Instant.now());
            offers.save(offer);
            log.info("Offer for ride {} to driver {} expired; re-offering",
                    offer.getRideId(), offer.getDriverId());

            Ride ride = rideService.get(offer.getRideId());
            if (ride.getStatus() == RideStatus.REQUESTED) {
                matchingService.offerNext(ride, offer.getTriedDriverIds());
            }
        }
    }
}
