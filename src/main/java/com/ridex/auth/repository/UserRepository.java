package com.ridex.auth.repository;

import com.ridex.auth.model.Role;
import com.ridex.auth.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    /** Used by the admin endpoint to list drivers awaiting approval. */
    List<User> findByRoleAndApproved(Role role, boolean approved);
}
