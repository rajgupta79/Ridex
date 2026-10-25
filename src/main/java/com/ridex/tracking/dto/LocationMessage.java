package com.ridex.tracking.dto;

/**
 * A live location ping sent by a driver over WebSocket during a ride.
 * {@code clientSentAt} is an epoch-millis timestamp set by the sender, used to
 * measure end-to-end latency (receive time minus this).
 */
public record LocationMessage(
        double longitude,
        double latitude,
        long clientSentAt
) {}
