package com.clinic.management;

import jakarta.persistence.*;
import java.time.Instant;

enum AccountRole { ADMIN, DOCTOR, PATIENT }

@Entity
@Table(name = "user_accounts", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, unique = true, length = 254) public String email;
    @Column(name = "password_hash", nullable = false) public String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public AccountRole role;
    @Column(name = "profile_id") public Long profileId;
    @Column(name = "full_name", nullable = false, length = 120) public String fullName;
    @Column(name = "created_at", nullable = false, updatable = false) public Instant createdAt = Instant.now();
    public UserAccount() {}
}

record SafeAccount(Long id, String email, String fullName, AccountRole role, Long profileId) {
    static SafeAccount from(UserAccount account) {
        return new SafeAccount(account.id, account.email, account.fullName, account.role, account.profileId);
    }
}

record LoginRequest(String email, String password) {}
