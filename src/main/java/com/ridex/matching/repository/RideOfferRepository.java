package com.ridex.matching.repository;

import com.ridex.matching.model.OfferStatus;
import com.ridex.matching.model.RideOffer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RideOfferRepository extends MongoRepository<RideOffer, String> {

    /** The current pending offer for a ride, if any. */
    Optional<RideOffer> findByRideIdAndStatus(String rideId, OfferStatus status);

    /** A driver's currently pending offer (what they'd see to accept/reject). */
    Optional<RideOffer> findByDriverIdAndStatus(String driverId, OfferStatus status);

    /** Pending offers whose deadline has passed — used by the sweeper. */
    List<RideOffer> findByStatusAndExpiresAtBefore(OfferStatus status, Instant cutoff);

    List<RideOffer> findByRideId(String rideId);
}
