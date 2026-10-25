package com.ridex.auth.model;

/**
 * Application roles. Spring Security expects authorities prefixed with "ROLE_",
 * which we add when building the UserDetails, so here we keep the clean names.
 */
public enum Role {
    RIDER,
    DRIVER,
    ADMIN
}
