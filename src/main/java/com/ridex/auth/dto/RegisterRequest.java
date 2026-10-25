package com.ridex.auth.dto;

import com.ridex.auth.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. Role is chosen at signup (RIDER or DRIVER);
 * ADMIN accounts are not self-registerable.
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72, message = "password must be 8-72 characters") String password,
        @NotBlank String name,
        @NotNull Role role
) {}
