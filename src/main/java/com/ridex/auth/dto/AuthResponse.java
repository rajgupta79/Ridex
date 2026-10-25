package com.ridex.auth.dto;

import com.ridex.auth.model.Role;

/**
 * Returned after a successful register/login. Carries the JWT plus a few
 * profile fields so the client doesn't need a second call to know who it is.
 */
public record AuthResponse(
        String token,
        String userId,
        String email,
        String name,
        Role role,
        boolean approved
) {}
