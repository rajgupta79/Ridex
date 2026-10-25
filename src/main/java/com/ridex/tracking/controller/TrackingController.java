package com.ridex.tracking.controller;

import com.ridex.auth.security.UserPrincipal;
import com.ridex.driver.dto.LocationUpdateRequest;
import com.ridex.driver.service.DriverService;
import com.ridex.ride.model.Ride;
import com.ridex.ride.service.RideService;
import com.ridex.tracking.dto.LocationMessage;
import com.ridex.tracking.dto.TrackingBroadcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

/**
 * Receives driver location pings over STOMP and broadcasts them to the ride's
 * topic so the rider sees the driver move in real time.
 *
 * Driver sends to:   /app/ride/{rideId}/location
 * Rider subscribes:  /topic/ride/{rideId}
 */
@Controller
public class TrackingController {

    private static final Logger log = LoggerFactory.getLogger(TrackingController.class);

    private final SimpMessagingTemplate broker;
    private final RideService rideService;
    private final DriverService driverService;

    public TrackingController(SimpMessagingTemplate broker,
                              RideService rideService,
                              DriverService driverService) {
        this.broker = broker;
        this.rideService = rideService;
        this.driverService = driverService;
    }

    @MessageMapping("/ride/{rideId}/location")
    public void driverLocation(@DestinationVariable String rideId,
                               @Payload LocationMessage msg,
                               Authentication auth) {
        long serverReceivedAt = System.currentTimeMillis();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        // Only the driver assigned to this ride may publish its location.
        Ride ride = rideService.get(rideId);
        if (!principal.getId().equals(ride.getDriverId())) {
            log.warn("Rejected tracking publish for ride {} from non-assigned user {}",
                    rideId, principal.getId());
            return;
        }

        // Persist the latest position (best-effort; doesn't block the broadcast).
        try {
            driverService.updateLocation(principal.getId(),
                    new LocationUpdateRequest(msg.longitude(), msg.latitude()));
        } catch (Exception ex) {
            log.debug("Could not persist driver location: {}", ex.getMessage());
        }

        TrackingBroadcast out = new TrackingBroadcast(
                rideId, principal.getId(),
                msg.longitude(), msg.latitude(),
                msg.clientSentAt(), serverReceivedAt);
        broker.convertAndSend("/topic/ride/" + rideId, out);
    }
}
