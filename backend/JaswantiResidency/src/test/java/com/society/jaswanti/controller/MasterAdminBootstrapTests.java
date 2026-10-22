package com.society.jaswanti.controller;

import com.society.jaswanti.JaswantiResidencyApplication;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class MasterAdminBootstrapTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean up test bootstrap user if present
        userRepository.findByEmailIgnoreCase("bootstrap-master@jaswanti.com")
                .ifPresent(userRepository::delete);
    }

    @Test
    void successfulBootstrapCreatesMasterAdmin() {
        // Ensure no active master admin exists before bootstrap run
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.MASTER_ADMIN && u.isActive() && u.getStatus() == UserStatus.ACTIVE)
                .forEach(u -> {
                    u.setStatus(UserStatus.INACTIVE);
                    userRepository.save(u);
                });

        Map<String, String> env = Map.of(
                "BOOTSTRAP_MASTER_ADMIN_ENABLED", "true",
                "BOOTSTRAP_MASTER_ADMIN_EMAIL", "bootstrap-master@jaswanti.com",
                "BOOTSTRAP_MASTER_ADMIN_NAME", "Bootstrap Master Admin",
                "BOOTSTRAP_MASTER_ADMIN_PASSWORD", "StrongTemporaryPass123!"
        );

        JaswantiResidencyApplication.executeBootstrap(userRepository, passwordEncoder, env::get);

        User created = userRepository.findByEmailIgnoreCase("bootstrap-master@jaswanti.com").orElseThrow();
        assertThat(created.getName()).isEqualTo("Bootstrap Master Admin");
        assertThat(created.getRole()).isEqualTo(Role.MASTER_ADMIN);
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.isActive()).isTrue();
        assertThat(created.isEmailVerified()).isTrue();
        assertThat(created.isForcePasswordChange()).isTrue();
        assertThat(passwordEncoder.matches("StrongTemporaryPass123!", created.getPassword())).isTrue();
    }

    @Test
    void missingConfigurationThrowsIllegalStateException() {
        // Missing email
        Map<String, String> missingEmail = Map.of(
                "BOOTSTRAP_MASTER_ADMIN_ENABLED", "true",
                "BOOTSTRAP_MASTER_ADMIN_NAME", "Bootstrap Master Admin",
                "BOOTSTRAP_MASTER_ADMIN_PASSWORD", "StrongTemporaryPass123!"
        );
        assertThatThrownBy(() -> JaswantiResidencyApplication.executeBootstrap(userRepository, passwordEncoder, missingEmail::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_MASTER_ADMIN_EMAIL must be set");

        // Missing name
        Map<String, String> missingName = Map.of(
                "BOOTSTRAP_MASTER_ADMIN_ENABLED", "true",
                "BOOTSTRAP_MASTER_ADMIN_EMAIL", "bootstrap-master@jaswanti.com",
                "BOOTSTRAP_MASTER_ADMIN_PASSWORD", "StrongTemporaryPass123!"
        );
        assertThatThrownBy(() -> JaswantiResidencyApplication.executeBootstrap(userRepository, passwordEncoder, missingName::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_MASTER_ADMIN_NAME must be set");

        // Missing password
        Map<String, String> missingPassword = Map.of(
                "BOOTSTRAP_MASTER_ADMIN_ENABLED", "true",
                "BOOTSTRAP_MASTER_ADMIN_EMAIL", "bootstrap-master@jaswanti.com",
                "BOOTSTRAP_MASTER_ADMIN_NAME", "Bootstrap Master Admin"
        );
        assertThatThrownBy(() -> JaswantiResidencyApplication.executeBootstrap(userRepository, passwordEncoder, missingPassword::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_MASTER_ADMIN_PASSWORD must be set");
    }

    @Test
    void refusesBootstrapWhenActiveMasterAdminAlreadyExists() {
        User existingMaster = userRepository.findByEmailIgnoreCase("active-master-check@jaswanti.com")
                .orElseGet(User::new);
        existingMaster.setEmail("active-master-check@jaswanti.com");
        existingMaster.setName("Existing Active Master");
        existingMaster.setPassword("hashed");
        existingMaster.setRole(Role.MASTER_ADMIN);
        existingMaster.setStatus(UserStatus.ACTIVE);
        existingMaster.setActive(true);
        userRepository.save(existingMaster);

        Map<String, String> env = Map.of(
                "BOOTSTRAP_MASTER_ADMIN_ENABLED", "true",
                "BOOTSTRAP_MASTER_ADMIN_EMAIL", "new-bootstrap-master@jaswanti.com",
                "BOOTSTRAP_MASTER_ADMIN_NAME", "New Master",
                "BOOTSTRAP_MASTER_ADMIN_PASSWORD", "StrongTemporaryPass123!"
        );

        assertThatThrownBy(() -> JaswantiResidencyApplication.executeBootstrap(userRepository, passwordEncoder, env::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Master Admin bootstrap is already complete");
    }

    @Test
    void refusesBootstrapWhenEmailAlreadyInUse() {
        // Deactivate all active master admins to isolate the email collision check
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.MASTER_ADMIN && u.isActive() && u.getStatus() == UserStatus.ACTIVE)
                .forEach(u -> {
                    u.setStatus(UserStatus.INACTIVE);
                    userRepository.save(u);
                });

        User existingResident = userRepository.findByEmailIgnoreCase("existing-resident@jaswanti.com")
                .orElseGet(User::new);
        existingResident.setEmail("existing-resident@jaswanti.com");
        existingResident.setName("Existing Resident");
        existingResident.setPassword("hashed");
        existingResident.setRole(Role.RESIDENT);
        existingResident.setStatus(UserStatus.ACTIVE);
        existingResident.setActive(true);
        userRepository.save(existingResident);

        Map<String, String> env = Map.of(
                "BOOTSTRAP_MASTER_ADMIN_ENABLED", "true",
                "BOOTSTRAP_MASTER_ADMIN_EMAIL", "existing-resident@jaswanti.com",
                "BOOTSTRAP_MASTER_ADMIN_NAME", "Colliding Name",
                "BOOTSTRAP_MASTER_ADMIN_PASSWORD", "StrongTemporaryPass123!"
        );

        assertThatThrownBy(() -> JaswantiResidencyApplication.executeBootstrap(userRepository, passwordEncoder, env::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bootstrap email is already associated with an account");
    }

    @Test
    void disabledByDefaultDoesNothing() {
        Map<String, String> disabled = new HashMap<>();
        disabled.put("BOOTSTRAP_MASTER_ADMIN_ENABLED", "false");

        JaswantiResidencyApplication.executeBootstrap(userRepository, passwordEncoder, disabled::get);

        assertThat(userRepository.findByEmailIgnoreCase("bootstrap-master@jaswanti.com")).isEmpty();
    }
}
