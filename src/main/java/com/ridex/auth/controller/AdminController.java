package com.ridex.auth.controller;

import com.ridex.auth.dto.UserView;
import com.ridex.auth.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only endpoints for driver approval. Guarded by @PreAuthorize so only
 * ADMIN tokens get through, enforced by method security.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Driver approval (ADMIN only)")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/drivers/pending")
    @Operation(summary = "List drivers awaiting approval")
    public List<UserView> pendingDrivers() {
        return adminService.pendingDrivers();
    }

    @PostMapping("/drivers/{userId}/approve")
    @Operation(summary = "Approve a driver")
    public UserView approve(@PathVariable String userId) {
        return adminService.approveDriver(userId);
    }

    @PostMapping("/drivers/{userId}/revoke")
    @Operation(summary = "Revoke a driver's approval")
    public UserView revoke(@PathVariable String userId) {
        return adminService.revokeDriver(userId);
    }
}
