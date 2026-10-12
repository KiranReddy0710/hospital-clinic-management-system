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

@Entity
@Table(name = "registration_invites", uniqueConstraints = @UniqueConstraint(columnNames = "token_hash"))
class RegistrationInvite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) public String tokenHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public AccountRole role;
    @Column(nullable = false) public Instant expiresAt;
    @Column(name = "used_at") public Instant usedAt;
    public RegistrationInvite() {}
}

record SafeAccount(Long id, String email, String fullName, AccountRole role, Long profileId) {
    static SafeAccount from(UserAccount account) {
        return new SafeAccount(account.id, account.email, account.fullName, account.role, account.profileId);
    }
}

record PatientRegistration(String email, String password, String fullName, Integer age,
                           String gender, String phone, String bloodGroup, String address) {}
record InvitedRegistration(String inviteCode, String email, String password, String fullName,
                           String phone, String specialization) {}
record LoginRequest(String email, String password) {}
record InviteRequest(AccountRole role) {}
record InviteResponse(String inviteCode, AccountRole role, Instant expiresAt) {}
