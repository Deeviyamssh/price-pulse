package com.pricepulse.auth.repository;

import com.pricepulse.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Look up a user by their email address.
     * Used during login and duplicate-email checks at registration.
     */
    Optional<User> findByEmail(String email);
}
