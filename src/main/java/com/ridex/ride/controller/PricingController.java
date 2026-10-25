package com.ridex.ride.controller;

import com.ridex.ride.dto.FareQuote;
import com.ridex.ride.service.PricingService;
import com.ridex.ride.service.PricingService.FareQuoteResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fare estimation. Lets a rider see the price (and any surge) before requesting.
 */
@RestController
@RequestMapping("/api/pricing")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pricing", description = "Fare quotes with surge")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping("/quote")
    @Operation(summary = "Estimate the fare for a pickup -> dropoff trip")
    public FareQuote quote(@RequestParam double pickupLng,
                           @RequestParam double pickupLat,
                           @RequestParam double dropoffLng,
                           @RequestParam double dropoffLat) {
        FareQuoteResult r = pricingService.quote(pickupLng, pickupLat, dropoffLng, dropoffLat);
        return new FareQuote(r.distanceKm(), r.baseFare(), r.distanceFare(),
                r.surgeMultiplier(), r.nearbyDrivers(), r.total());
    }
}
