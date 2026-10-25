package com.ridex.driver.model;

/**
 * Operational availability of a driver.
 * - OFFLINE: not accepting rides
 * - AVAILABLE: online and free to be matched
 * - ON_TRIP: currently serving a ride (not matchable for a solo ride)
 */
public enum DriverStatus {
    OFFLINE,
    AVAILABLE,
    ON_TRIP
}
