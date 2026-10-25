package com.ridex.driver.controller;

import com.ridex.auth.security.UserPrincipal;
import com.ridex.driver.dto.DriverView;
import com.ridex.driver.dto.LocationUpdateRequest;
import com.ridex.driver.dto.NearbyDriver;
import com.ridex.driver.model.DriverStatus;
import com.ridex.driver.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Driver-facing endpoints (location + availability) and a nearby-driver search.
 * All require authentication; driver-only actions are guarded by role.
 */
@RestController
@RequestMapping("/api/drivers")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Driver", description = "Driver location, availability, and proximity search")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PutMapping("/me/location")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update my current location (brings me online if offline)")
    public DriverView updateLocation(@AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody LocationUpdateRequest request) {
        return driverService.updateLocation(principal.getId(), request);
    }

    @PostMapping("/me/status/{status}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Set my availability (OFFLINE / AVAILABLE / ON_TRIP)")
    public DriverView setStatus(@AuthenticationPrincipal UserPrincipal principal,
                                @PathVariable DriverStatus status) {
        return driverService.setStatus(principal.getId(), status);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get my driver profile")
    public DriverView myProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return driverService.getMyProfile(principal.getId());
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find nearest available drivers within a radius (km)")
    public List<NearbyDriver> nearby(@RequestParam double longitude,
                                     @RequestParam double latitude,
                                     @RequestParam(required = false) Double radiusKm,
                                     @RequestParam(required = false) Integer limit) {
        return driverService.findNearby(longitude, latitude, radiusKm, limit);
    }
}
