package com.ridex.auth.service;

import com.ridex.auth.dto.UserView;
import com.ridex.auth.model.Role;
import com.ridex.auth.model.User;
import com.ridex.auth.repository.UserRepository;
import com.ridex.common.ApiException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Admin operations: review and approve drivers.
 */
@Service
public class AdminService {

    private final UserRepository users;

    public AdminService(UserRepository users) {
        this.users = users;
    }

    /** Drivers who have registered but are not yet approved. */
    public List<UserView> pendingDrivers() {
        return users.findByRoleAndApproved(Role.DRIVER, false).stream()
                .map(UserView::from)
                .toList();
    }

    public UserView approveDriver(String userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + userId));
        if (user.getRole() != Role.DRIVER) {
            throw ApiException.badRequest("User is not a driver");
        }
        user.setApproved(true);
        return UserView.from(users.save(user));
    }

    public UserView revokeDriver(String userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + userId));
        if (user.getRole() != Role.DRIVER) {
            throw ApiException.badRequest("User is not a driver");
        }
        user.setApproved(false);
        return UserView.from(users.save(user));
    }
}
