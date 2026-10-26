package com.society.jaswanti.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
// Internal security state must never reach API clients when a User is serialised (e.g. nested in a Room or Transaction)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(value = {
        "pendingEmail", "emailVerificationExpiry", "emailVerificationAttempts",
        "passwordResetOtpExpiry", "passwordResetOtpAttempts", "passwordResetOtpLastSentAt",
        "tokenVersion", "authorities", "username", "accountNonExpired", "accountNonLocked",
        "credentialsNonExpired", "enabled", "hibernateLazyInitializer", "handler"
}, ignoreUnknown = true)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "force_password_change", nullable = false)
    private boolean forcePasswordChange = true;

    @Column(name = "pending_email")
    private String pendingEmail;

    @Column(name = "email_verification_token")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String emailVerificationToken;

    @Column(name = "email_verification_expiry")
    private LocalDateTime emailVerificationExpiry;

    @Column(name = "email_verification_attempts", nullable = false)
    private int emailVerificationAttempts = 0;

    @Column(name = "password_reset_otp_hash")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String passwordResetOtpHash;

    @Column(name = "password_reset_otp_expiry")
    private LocalDateTime passwordResetOtpExpiry;

    @Column(name = "password_reset_otp_attempts", nullable = false)
    private Integer passwordResetOtpAttempts = 0;

    @Column(name = "password_reset_otp_last_sent_at")
    private LocalDateTime passwordResetOtpLastSentAt;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    // A temporary password generated during the current request, so the controller can show it to the admin once.
    // Never persisted and never serialised.
    @Transient
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String issuedTemporaryPassword;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public User() {}

    public User(String email, String password, String name, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    // UserDetails Methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        // Fallback to ID if email is not provided yet
        return email != null ? email : String.valueOf(id);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.BLOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive && status == UserStatus.ACTIVE;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isForcePasswordChange() { return forcePasswordChange; }
    public void setForcePasswordChange(boolean forcePasswordChange) { this.forcePasswordChange = forcePasswordChange; }

    public String getPendingEmail() { return pendingEmail; }
    public void setPendingEmail(String pendingEmail) { this.pendingEmail = pendingEmail; }

    public String getEmailVerificationToken() { return emailVerificationToken; }
    public void setEmailVerificationToken(String emailVerificationToken) { this.emailVerificationToken = emailVerificationToken; }

    public LocalDateTime getEmailVerificationExpiry() { return emailVerificationExpiry; }
    public void setEmailVerificationExpiry(LocalDateTime emailVerificationExpiry) { this.emailVerificationExpiry = emailVerificationExpiry; }

    public String getPasswordResetOtpHash() { return passwordResetOtpHash; }
    public void setPasswordResetOtpHash(String passwordResetOtpHash) { this.passwordResetOtpHash = passwordResetOtpHash; }

    public LocalDateTime getPasswordResetOtpExpiry() { return passwordResetOtpExpiry; }
    public void setPasswordResetOtpExpiry(LocalDateTime passwordResetOtpExpiry) { this.passwordResetOtpExpiry = passwordResetOtpExpiry; }

    public Integer getPasswordResetOtpAttempts() { return passwordResetOtpAttempts != null ? passwordResetOtpAttempts : 0; }
    public void setPasswordResetOtpAttempts(Integer passwordResetOtpAttempts) { this.passwordResetOtpAttempts = passwordResetOtpAttempts; }

    public LocalDateTime getPasswordResetOtpLastSentAt() { return passwordResetOtpLastSentAt; }
    public void setPasswordResetOtpLastSentAt(LocalDateTime passwordResetOtpLastSentAt) { this.passwordResetOtpLastSentAt = passwordResetOtpLastSentAt; }

    public int getEmailVerificationAttempts() { return emailVerificationAttempts; }
    public void setEmailVerificationAttempts(int emailVerificationAttempts) { this.emailVerificationAttempts = emailVerificationAttempts; }

    public String getIssuedTemporaryPassword() { return issuedTemporaryPassword; }
    public void setIssuedTemporaryPassword(String issuedTemporaryPassword) { this.issuedTemporaryPassword = issuedTemporaryPassword; }

    public int getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(int tokenVersion) { this.tokenVersion = tokenVersion; }
}
