package com.ridex.ride.service;

import com.ridex.auth.model.Role;
import com.ridex.auth.model.User;
import com.ridex.auth.repository.UserRepository;
import com.ridex.common.ApiException;
import com.ridex.driver.model.DriverProfile;
import com.ridex.driver.model.DriverStatus;
import com.ridex.driver.repository.DriverProfileRepository;
import com.ridex.ride.event.RideRequestedEvent;
import com.ridex.ride.model.Ride;
import com.ridex.ride.model.RideStatus;
import com.ridex.ride.repository.RideRepository;
import com.ridex.ride.service.PricingService.FareQuoteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Ride lifecycle logic. Enforces the state machine and performs the atomic
 * accept that prevents two drivers from grabbing the same ride.
 */
@Service
public class RideService {

    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    private final RideRepository rides;
    private final UserRepository users;
    private final DriverProfileRepository drivers;
    private final MongoTemplate mongo;
    private final ApplicationEventPublisher events;
    private final PricingService pricingService;

    public RideService(RideRepository rides,
                       UserRepository users,
                       DriverProfileRepository drivers,
                       MongoTemplate mongo,
                       ApplicationEventPublisher events,
                       PricingService pricingService) {
        this.rides = rides;
        this.users = users;
        this.drivers = drivers;
        this.mongo = mongo;
        this.events = events;
        this.pricingService = pricingService;
    }

    // --- Rider actions ---

    public Ride requestRide(String riderId, double pickupLng, double pickupLat,
                            double dropoffLng, double dropoffLat) {
        // One active ride per rider at a time.
        boolean hasActive = rides.existsByRiderIdAndStatusIn(
                riderId, List.of(RideStatus.REQUESTED, RideStatus.ACCEPTED, RideStatus.STARTED));
        if (hasActive) {
            throw ApiException.conflict("You already have an active ride");
        }

        // Quote the fare at request time (includes surge if drivers are scarce).
        FareQuoteResult quote = pricingService.quote(pickupLng, pickupLat, dropoffLng, dropoffLat);

        Ride ride = Ride.builder()
                .riderId(riderId)
                .status(RideStatus.REQUESTED)
                .pickup(new GeoJsonPoint(pickupLng, pickupLat))
                .dropoff(new GeoJsonPoint(dropoffLng, dropoffLat))
                .estimatedDistanceKm(quote.distanceKm())
                .fare(quote.total())
                .requestedAt(Instant.now())
                .build();
        ride = rides.save(ride);

        // Decoupled hand-off to the matching module.
        events.publishEvent(new RideRequestedEvent(ride.getId()));
        return ride;
    }

    // --- Driver actions ---

    /**
     * Atomically accept a REQUESTED ride. The findAndModify matches on
     * (id == rideId AND status == REQUESTED AND driverId == null) and, in the
     * SAME operation, flips status to ACCEPTED and assigns the driver.
     *
     * Because the match + update is one atomic MongoDB command, only ONE of two
     * concurrent drivers can succeed; the loser's query no longer matches
     * (status is already ACCEPTED), so it gets null and is rejected.
     */
    public Ride acceptRide(String driverId, String rideId) {
        User driver = users.findById(driverId)
                .orElseThrow(() -> ApiException.notFound("Driver not found"));
        if (driver.getRole() != Role.DRIVER || !driver.isApproved()) {
            throw ApiException.forbidden("Only approved drivers can accept rides");
        }

        Query query = Query.query(Criteria.where("_id").is(rideId)
                .and("status").is(RideStatus.REQUESTED.name())
                .and("driverId").is(null));

        Update update = new Update()
                .set("status", RideStatus.ACCEPTED.name())
                .set("driverId", driverId)
                .set("acceptedAt", Instant.now());

        // returnNew(true) -> give us the post-update document.
        Ride accepted = mongo.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Ride.class);

        if (accepted == null) {
            // Either the ride doesn't exist, was already taken, or isn't REQUESTED.
            Ride existing = rides.findById(rideId)
                    .orElseThrow(() -> ApiException.notFound("Ride not found"));
            if (existing.getStatus() != RideStatus.REQUESTED) {
                throw ApiException.conflict("Ride is no longer available (status " + existing.getStatus() + ")");
            }
            throw ApiException.conflict("Ride was just taken by another driver");
        }

        // Mark the driver as on a trip.
        drivers.findByUserId(driverId).ifPresent(dp -> {
            dp.setStatus(DriverStatus.ON_TRIP);
            dp.setCurrentRideId(rideId);
            drivers.save(dp);
        });

        log.info("Driver {} accepted ride {}", driverId, rideId);
        return accepted;
    }

    public Ride startRide(String driverId, String rideId) {
        Ride ride = requireDriverRide(driverId, rideId);
        return transition(ride, RideStatus.STARTED, u -> u.set("startedAt", Instant.now()));
    }

    public Ride completeRide(String driverId, String rideId) {
        Ride ride = requireDriverRide(driverId, rideId);
        Ride done = transition(ride, RideStatus.COMPLETED, u -> u.set("completedAt", Instant.now()));
        freeDriver(driverId, rideId);
        return done;
    }

    // --- Shared ---

    public Ride cancelRide(String userId, String rideId, String reason) {
        Ride ride = rides.findById(rideId)
                .orElseThrow(() -> ApiException.notFound("Ride not found"));

        // Only the rider on the ride or the assigned driver may cancel.
        boolean isRider = ride.getRiderId().equals(userId);
        boolean isDriver = userId.equals(ride.getDriverId());
        if (!isRider && !isDriver) {
            throw ApiException.forbidden("You are not a participant in this ride");
        }

        Ride cancelled = transition(ride, RideStatus.CANCELLED,
                u -> u.set("cancelledAt", Instant.now()).set("cancelReason", reason));
        if (ride.getDriverId() != null) {
            freeDriver(ride.getDriverId(), rideId);
        }
        return cancelled;
    }

    public Ride get(String rideId) {
        return rides.findById(rideId)
                .orElseThrow(() -> ApiException.notFound("Ride not found"));
    }

    public List<Ride> ridesForRider(String riderId) {
        return rides.findByRiderIdOrderByRequestedAtDesc(riderId);
    }

    public List<Ride> ridesForDriver(String driverId) {
        return rides.findByDriverIdOrderByRequestedAtDesc(driverId);
    }

    // --- Internals ---

    /**
     * Guarded, atomic status transition. Validates the state machine first,
     * then applies the change with a findAndModify conditioned on the ride still
     * being in the expected source status (so a concurrent change can't slip through).
     */
    private Ride transition(Ride ride, RideStatus target, java.util.function.Consumer<Update> extra) {
        if (!ride.getStatus().canTransitionTo(target)) {
            throw ApiException.badRequest(
                    "Illegal transition " + ride.getStatus() + " -> " + target);
        }
        Query query = Query.query(Criteria.where("_id").is(ride.getId())
                .and("status").is(ride.getStatus().name()));
        Update update = new Update().set("status", target.name());
        extra.accept(update);

        Ride updated = mongo.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Ride.class);
        if (updated == null) {
            throw ApiException.conflict("Ride changed concurrently; please retry");
        }
        return updated;
    }

    private Ride requireDriverRide(String driverId, String rideId) {
        Ride ride = rides.findById(rideId)
                .orElseThrow(() -> ApiException.notFound("Ride not found"));
        if (!driverId.equals(ride.getDriverId())) {
            throw ApiException.forbidden("This ride is not assigned to you");
        }
        return ride;
    }

    private void freeDriver(String driverId, String rideId) {
        drivers.findByUserId(driverId).ifPresent(dp -> {
            if (rideId.equals(dp.getCurrentRideId())) {
                dp.setCurrentRideId(null);
                dp.setStatus(DriverStatus.AVAILABLE);
                drivers.save(dp);
            }
        });
    }
}
