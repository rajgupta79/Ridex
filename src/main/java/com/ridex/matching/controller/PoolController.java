package com.ridex.matching.controller;

import com.ridex.auth.security.UserPrincipal;
import com.ridex.common.ApiException;
import com.ridex.matching.dto.PoolResult;
import com.ridex.matching.model.PoolGroup;
import com.ridex.matching.service.PoolingService;
import com.ridex.ride.model.Ride;
import com.ridex.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Cab sharing endpoints. A rider creates a ride, then opts to pool it; pooling
 * attaches them to a compatible shared trip or opens a new one.
 */
@RestController
@RequestMapping("/api/pool")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pooling", description = "Cab sharing (basic)")
public class PoolController {

    private final PoolingService poolingService;
    private final RideService rideService;

    public PoolController(PoolingService poolingService, RideService rideService) {
        this.poolingService = poolingService;
        this.rideService = rideService;
    }

    @PostMapping("/{rideId}")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Pool my ride (join a shared trip or start one)")
    public PoolResult pool(@AuthenticationPrincipal UserPrincipal principal,
                           @PathVariable String rideId) {
        Ride ride = rideService.get(rideId);
        if (!ride.getRiderId().equals(principal.getId())) {
            throw ApiException.forbidden("Not your ride");
        }
        return poolingService.pool(ride);
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "View a pool group")
    public PoolGroup group(@PathVariable String groupId) {
        return poolingService.getGroup(groupId)
                .orElseThrow(() -> ApiException.notFound("Pool group not found"));
    }
}
