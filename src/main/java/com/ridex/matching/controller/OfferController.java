package com.ridex.matching.controller;

import com.ridex.auth.security.UserPrincipal;
import com.ridex.common.ApiException;
import com.ridex.matching.dto.OfferView;
import com.ridex.matching.service.MatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Driver-facing endpoints for the current ride offer. Matching pushes offers
 * one at a time; the driver polls their current offer and accepts or rejects it.
 * (Phase 5 will also push offers live over WebSocket.)
 */
@RestController
@RequestMapping("/api/offers")
@PreAuthorize("hasRole('DRIVER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Matching", description = "Ride offers to drivers")
public class OfferController {

    private final MatchingService matchingService;

    public OfferController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping("/current")
    @Operation(summary = "My current pending ride offer (if any)")
    public ResponseEntity<OfferView> current(@AuthenticationPrincipal UserPrincipal principal) {
        return matchingService.currentOfferForDriver(principal.getId())
                .map(OfferView::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/accept")
    @Operation(summary = "Accept my current offer")
    public OfferView accept(@AuthenticationPrincipal UserPrincipal principal) {
        return OfferView.from(matchingService.acceptOffer(principal.getId()));
    }

    @PostMapping("/reject")
    @Operation(summary = "Reject my current offer (ride is re-offered to the next driver)")
    public ResponseEntity<Void> reject(@AuthenticationPrincipal UserPrincipal principal) {
        matchingService.rejectOffer(principal.getId());
        return ResponseEntity.noContent().build();
    }
}
