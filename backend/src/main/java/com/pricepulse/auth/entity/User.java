package com.pricepulse.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ NOT NULL DEFAULT NOW()")
    private Instant createdAt;

    @Column(name = "token_invalidated_at")
    private Instant tokenInvalidatedAt;

    @PrePersist
    private void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // ── UserDetails ──────────────────────────────────────────────────────────

    /** Spring Security uses email as the username. */
    @Override
    public String getUsername() {
        return email;
    }

    /** The stored BCrypt hash — Spring Security uses this for credential matching. */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /** PricePulse has no roles; return an empty list. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
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

    // ── Getters and setters ──────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getTokenInvalidatedAt() {
        return tokenInvalidatedAt;
    }

    public void setTokenInvalidatedAt(Instant tokenInvalidatedAt) {
        this.tokenInvalidatedAt = tokenInvalidatedAt;
    }
}
