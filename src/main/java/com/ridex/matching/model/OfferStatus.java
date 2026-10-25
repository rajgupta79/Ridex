package com.ridex.matching.model;

/**
 * Lifecycle of a single ride offer to one driver.
 * - PENDING: waiting for the driver to respond
 * - ACCEPTED: driver took the ride
 * - REJECTED: driver declined
 * - EXPIRED: driver didn't respond within the timeout
 */
public enum OfferStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED
}
