package com.ridex.auth.service;

import com.ridex.auth.dto.AuthResponse;
import com.ridex.auth.dto.LoginRequest;
import com.ridex.auth.dto.RegisterRequest;
import com.ridex.auth.model.Role;
import com.ridex.auth.model.User;
import com.ridex.auth.repository.UserRepository;
import com.ridex.auth.security.JwtService;
import com.ridex.common.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Business logic for registration and login. Controllers stay thin and delegate here.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
        // ADMIN cannot be self-registered; admins are bootstrapped or promoted.
        if (req.role() == Role.ADMIN) {
            throw ApiException.badRequest("Cannot self-register as ADMIN");
        }
        if (users.existsByEmail(req.email())) {
            throw ApiException.conflict("Email already registered");
        }

        // Riders are usable immediately. Drivers must be approved by an admin first.
        boolean approved = req.role() == Role.RIDER;

        User user = User.builder()
                .email(req.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .name(req.name())
                .role(req.role())
                .approved(approved)
                .build();

        user = users.save(user);
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.isApproved());
    }
}
