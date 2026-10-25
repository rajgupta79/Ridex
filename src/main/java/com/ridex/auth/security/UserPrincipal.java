package com.ridex.auth.security;

import com.ridex.auth.model.Role;
import com.ridex.auth.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts our {@link User} to Spring Security's {@link UserDetails}.
 * The authenticated principal exposes the user id (handy for controllers via
 * {@code @AuthenticationPrincipal}) and the role as a ROLE_ authority.
 */
public class UserPrincipal implements UserDetails {

    private final String id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean approved;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.approved = user.isApproved();
    }

    public String getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public boolean isApproved() {
        return approved;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
