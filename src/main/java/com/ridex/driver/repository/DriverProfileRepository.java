package com.ridex.driver.repository;

import com.ridex.driver.model.DriverProfile;
import com.ridex.driver.model.DriverStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Note: the "nearest drivers" proximity search is NOT a derived query here.
 * A plain find-with-$near is rejected by MongoDB ($near must drive the sort),
 * so {@code DriverService} uses a geoNear aggregation via MongoTemplate instead.
 */
public interface DriverProfileRepository extends MongoRepository<DriverProfile, String> {

    Optional<DriverProfile> findByUserId(String userId);

    long countByStatus(DriverStatus status);
}
