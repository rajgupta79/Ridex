package com.ridex.ride.event;

/**
 * Published when a rider requests a ride. The matching module listens for this
 * and starts offering the ride to drivers. Using an event keeps ride and
 * matching decoupled (no circular dependency between the two services).
 */
public record RideRequestedEvent(String rideId) {}
