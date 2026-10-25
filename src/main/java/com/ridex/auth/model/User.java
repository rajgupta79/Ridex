package com.ridex.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A RideX account. One document per registered user, regardless of role.
 * Drivers additionally get a DriverProfile (Phase 2) keyed by this user's id.
 */
@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;

    /** Unique login identifier. */
    @Indexed(unique = true)
    private String email;

    /** BCrypt hash — never the plaintext password. */
    private String passwordHash;

    private String name;

    private Role role;

    /**
     * Driver approval gate. Riders and admins are enabled immediately.
     * A DRIVER stays disabled (approved=false) until an ADMIN approves them,
     * which blocks them from going online or accepting rides.
     */
    @Builder.Default
    private boolean approved = false;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
