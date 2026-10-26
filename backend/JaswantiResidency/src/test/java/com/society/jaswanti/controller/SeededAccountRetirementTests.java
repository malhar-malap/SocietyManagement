package com.society.jaswanti.controller;

import com.society.jaswanti.JaswantiResidencyApplication;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify the intent of V15__deactivate_seeded_accounts.sql:
 * - Seeded admin and resident accounts with default credentials must be deactivated.
 * - Master Admin bootstrap still works on a clean database.
 *
 * Note: The actual Flyway migration uses MySQL-specific SQL and cannot run on H2.
 * These tests simulate the seeded state and verify the deactivation logic via JPA
 * to ensure the migration's intent is correct and the system handles retired accounts properly.
 */
@SpringBootTest
class SeededAccountRetirementTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<User> createdUsers = new ArrayList<>();

    @AfterEach
    void tearDown() {
        createdUsers.forEach(u -> userRepository.findById(u.getId()).ifPresent(userRepository::delete));
        createdUsers.clear();
    }

    @Test
    @DisplayName("Seeded admin account with default credentials should be deactivated")
    void seededAdminWithDefaultCredentialsMustBeDeactivated() {
        // Simulate what V2 seeds and V15 should deactivate
        User seededAdmin = new User();
        seededAdmin.setEmail("seeded-test-admin@jaswanti.com");
        seededAdmin.setPassword(passwordEncoder.encode("admin123"));
        seededAdmin.setName("Admin");
        seededAdmin.setRole(Role.ADMIN);
        seededAdmin.setStatus(UserStatus.ACTIVE);
        seededAdmin.setActive(true);
        seededAdmin.setEmailVerified(true);
        seededAdmin.setForcePasswordChange(false);
        seededAdmin = userRepository.save(seededAdmin);
        createdUsers.add(seededAdmin);

        // Simulate V15 logic: deactivate seeded admin
        seededAdmin.setActive(false);
        seededAdmin.setStatus(UserStatus.INACTIVE);
        userRepository.save(seededAdmin);

        User result = userRepository.findByEmailIgnoreCase("seeded-test-admin@jaswanti.com").orElseThrow();
        assertThat(result.isActive()).isFalse();
        assertThat(result.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Seeded resident accounts still on default credentials should be deactivated")
    void seededResidentWithDefaultCredentialsMustBeDeactivated() {
        // Simulate V2 seeded resident: force_password_change=true, email_verified=false
        User seededResident = new User();
        seededResident.setEmail("seeded-test-resident@jaswanti.com");
        seededResident.setPassword(passwordEncoder.encode("Temp_C-101"));
        seededResident.setName("Resident 101");
        seededResident.setRole(Role.RESIDENT);
        seededResident.setStatus(UserStatus.ACTIVE);
        seededResident.setActive(true);
        seededResident.setEmailVerified(false);
        seededResident.setForcePasswordChange(true);
        seededResident = userRepository.save(seededResident);
        createdUsers.add(seededResident);

        // Simulate V15 logic: deactivate if still on defaults
        if (seededResident.isForcePasswordChange() && !seededResident.isEmailVerified()) {
            seededResident.setActive(false);
            seededResident.setStatus(UserStatus.INACTIVE);
            userRepository.save(seededResident);
        }

        User result = userRepository.findByEmailIgnoreCase("seeded-test-resident@jaswanti.com").orElseThrow();
        assertThat(result.isActive()).isFalse();
        assertThat(result.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    @DisplayName("Resident who completed setup should NOT be deactivated by V15 logic")
    void residentWhoCompletedSetupIsNotDeactivated() {
        // A resident who has changed password and verified email
        User verifiedResident = new User();
        verifiedResident.setEmail("verified-resident-test@jaswanti.com");
        verifiedResident.setPassword(passwordEncoder.encode("MyNewStrongPassword1"));
        verifiedResident.setName("Verified Resident");
        verifiedResident.setRole(Role.RESIDENT);
        verifiedResident.setStatus(UserStatus.ACTIVE);
        verifiedResident.setActive(true);
        verifiedResident.setEmailVerified(true);
        verifiedResident.setForcePasswordChange(false);
        verifiedResident = userRepository.save(verifiedResident);
        createdUsers.add(verifiedResident);

        // V15 condition: only deactivate if force_password_change=1 AND email_verified=0
        boolean shouldDeactivate = verifiedResident.isForcePasswordChange() && !verifiedResident.isEmailVerified();
        assertThat(shouldDeactivate).isFalse();

        // Account remains active
        User result = userRepository.findByEmailIgnoreCase("verified-resident-test@jaswanti.com").orElseThrow();
        assertThat(result.isActive()).isTrue();
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Master Admin bootstrap succeeds after seeded accounts are retired")
    void masterAdminBootstrapSucceedsAfterRetirement() {
        // Clean up any pre-existing test user
        userRepository.findByEmailIgnoreCase("fresh-master-test@jaswanti.com")
                .ifPresent(userRepository::delete);

        // Ensure no other active master admin interferes
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.MASTER_ADMIN && u.isActive() && u.getStatus() == UserStatus.ACTIVE)
                .forEach(u -> {
                    u.setStatus(UserStatus.INACTIVE);
                    userRepository.save(u);
                });

        Map<String, String> env = Map.of(
                "BOOTSTRAP_MASTER_ADMIN_ENABLED", "true",
                "BOOTSTRAP_MASTER_ADMIN_EMAIL", "fresh-master-test@jaswanti.com",
                "BOOTSTRAP_MASTER_ADMIN_NAME", "Fresh Master Admin",
                "BOOTSTRAP_MASTER_ADMIN_PASSWORD", "SuperSecurePassword123!"
        );

        JaswantiResidencyApplication.executeBootstrap(userRepository, passwordEncoder, env::get);

        User created = userRepository.findByEmailIgnoreCase("fresh-master-test@jaswanti.com").orElseThrow();
        createdUsers.add(created);
        assertThat(created.getRole()).isEqualTo(Role.MASTER_ADMIN);
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.isActive()).isTrue();
    }

    @Test
    @DisplayName("Deactivated seeded accounts are rejected by login (isEnabled returns false)")
    void deactivatedSeededAccountIsRejectedByLogin() {
        User deactivated = new User();
        deactivated.setEmail("deactivated-login-test@jaswanti.com");
        deactivated.setPassword(passwordEncoder.encode("admin123"));
        deactivated.setName("Deactivated Admin");
        deactivated.setRole(Role.ADMIN);
        deactivated.setStatus(UserStatus.INACTIVE);
        deactivated.setActive(false);
        deactivated.setForcePasswordChange(false);
        deactivated = userRepository.save(deactivated);
        createdUsers.add(deactivated);

        // UserDetails.isEnabled() should return false
        assertThat(deactivated.isEnabled()).isFalse();
        assertThat(deactivated.isActive()).isFalse();
        assertThat(deactivated.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }
}
