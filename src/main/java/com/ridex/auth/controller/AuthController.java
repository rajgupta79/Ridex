package com.ridex.auth.controller;

import com.ridex.auth.dto.AuthResponse;
import com.ridex.auth.dto.LoginRequest;
import com.ridex.auth.dto.RegisterRequest;
import com.ridex.auth.dto.UserView;
import com.ridex.auth.model.User;
import com.ridex.auth.repository.UserRepository;
import com.ridex.auth.security.UserPrincipal;
import com.ridex.auth.service.AuthService;
import com.ridex.common.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Public auth endpoints (register, login) plus the authenticated "who am I" lookup.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registration, login, and current user")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a RIDER or DRIVER account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Current authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    public UserView me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw ApiException.unauthorized("Not authenticated");
        }
        User user = users.findById(principal.getId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return UserView.from(user);
    }
}
