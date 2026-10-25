package com.ridex.ride.controller;

import com.ridex.auth.security.UserPrincipal;
import com.ridex.ride.dto.RideRequest;
import com.ridex.ride.dto.RideView;
import com.ridex.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ride lifecycle endpoints. Riders request/cancel/list their rides; drivers
 * accept/start/complete/cancel rides assigned to them.
 */
@RestController
@RequestMapping("/api/rides")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ride", description = "Ride request and lifecycle")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Request a ride (RIDER)")
    public ResponseEntity<RideView> request(@AuthenticationPrincipal UserPrincipal principal,
                                            @Valid @RequestBody RideRequest req) {
        var ride = rideService.requestRide(principal.getId(),
                req.pickupLng(), req.pickupLat(), req.dropoffLng(), req.dropoffLat());
        return ResponseEntity.status(HttpStatus.CREATED).body(RideView.from(ride));
    }

    @PostMapping("/{rideId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Accept a requested ride (DRIVER, atomic)")
    public RideView accept(@AuthenticationPrincipal UserPrincipal principal,
                           @PathVariable String rideId) {
        return RideView.from(rideService.acceptRide(principal.getId(), rideId));
    }

    @PostMapping("/{rideId}/start")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Start an accepted ride (DRIVER)")
    public RideView start(@AuthenticationPrincipal UserPrincipal principal,
                          @PathVariable String rideId) {
        return RideView.from(rideService.startRide(principal.getId(), rideId));
    }

    @PostMapping("/{rideId}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Complete a started ride (DRIVER)")
    public RideView complete(@AuthenticationPrincipal UserPrincipal principal,
                             @PathVariable String rideId) {
        return RideView.from(rideService.completeRide(principal.getId(), rideId));
    }

    @PostMapping("/{rideId}/cancel")
    @Operation(summary = "Cancel a ride (rider or assigned driver)")
    public RideView cancel(@AuthenticationPrincipal UserPrincipal principal,
                           @PathVariable String rideId,
                           @RequestParam(required = false) String reason) {
        return RideView.from(rideService.cancelRide(principal.getId(), rideId, reason));
    }

    @GetMapping("/{rideId}")
    @Operation(summary = "Get a ride by id")
    public RideView get(@PathVariable String rideId) {
        return RideView.from(rideService.get(rideId));
    }

    @GetMapping("/mine")
    @Operation(summary = "List my rides (as rider or as driver)")
    public List<RideView> mine(@AuthenticationPrincipal UserPrincipal principal) {
        var asRider = rideService.ridesForRider(principal.getId());
        var asDriver = rideService.ridesForDriver(principal.getId());
        return java.util.stream.Stream.concat(asRider.stream(), asDriver.stream())
                .map(RideView::from)
                .toList();
    }
}
