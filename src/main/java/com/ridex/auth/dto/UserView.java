package com.ridex.auth.dto;

import com.ridex.auth.model.Role;
import com.ridex.auth.model.User;

import java.time.Instant;

/**
 * A safe projection of a user for API responses — never exposes the password hash.
 */
public record UserView(
        String id,
        String email,
        String name,
        Role role,
        boolean approved,
        Instant createdAt
) {
    public static UserView from(User user) {
        return new UserView(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.isApproved(),
                user.getCreatedAt());
    }
}
