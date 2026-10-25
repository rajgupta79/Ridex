package com.ridex.ride.repository;

import com.ridex.ride.model.Ride;
import com.ridex.ride.model.RideStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RideRepository extends MongoRepository<Ride, String> {

    List<Ride> findByRiderIdOrderByRequestedAtDesc(String riderId);

    List<Ride> findByDriverIdOrderByRequestedAtDesc(String driverId);

    List<Ride> findByStatus(RideStatus status);

    /** Does this rider already have a ride that isn't finished? */
    boolean existsByRiderIdAndStatusIn(String riderId, List<RideStatus> statuses);
}
