package com.ridex.ride.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The ride state machine.
 *
 * <pre>
 *   REQUESTED ──accept──▶ ACCEPTED ──start──▶ STARTED ──complete──▶ COMPLETED
 *       │                    │                   │
 *       └────cancel──────────┴──────cancel───────┘
 *                         (CANCELLED)
 * </pre>
 *
 * COMPLETED and CANCELLED are terminal. The allowed-transition map is the single
 * source of truth so illegal jumps (e.g. REQUESTED → COMPLETED) are rejected.
 */
public enum RideStatus {
    REQUESTED,
    ACCEPTED,
    STARTED,
    COMPLETED,
    CANCELLED;

    private static final Map<RideStatus, Set<RideStatus>> ALLOWED = Map.of(
            REQUESTED, EnumSet.of(ACCEPTED, CANCELLED),
            ACCEPTED, EnumSet.of(STARTED, CANCELLED),
            STARTED, EnumSet.of(COMPLETED, CANCELLED),
            COMPLETED, EnumSet.noneOf(RideStatus.class),
            CANCELLED, EnumSet.noneOf(RideStatus.class));

    public boolean canTransitionTo(RideStatus target) {
        return ALLOWED.getOrDefault(this, EnumSet.noneOf(RideStatus.class)).contains(target);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
