package com.society.jaswanti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.repository.UserRepository;

@SpringBootApplication
public class JaswantiResidencyApplication {

	public static void main(String[] args) {
		SpringApplication.run(JaswantiResidencyApplication.class, args);
	}

	/**
	 * Out-of-band, one-time bootstrap. It is deliberately configuration-driven,
	 * disabled by default, and has no HTTP endpoint or default credentials.
	 * Set BOOTSTRAP_MASTER_ADMIN_ENABLED=true together with the three values for
	 * one deployment, then remove/disable them after the application starts.
	 */
	@Bean
	public CommandLineRunner bootstrapMasterAdmin(UserRepository userRepository,
	                                              PasswordEncoder passwordEncoder,
	                                              org.springframework.core.env.Environment environment,
	                                              com.society.jaswanti.service.ActivityLogService activityLogService) {
		return args -> {
			executeBootstrap(userRepository, passwordEncoder, environment::getProperty);
			if ("true".equalsIgnoreCase(environment.getProperty("BOOTSTRAP_MASTER_ADMIN_ENABLED"))) {
				// Reaching here means a new Master Admin was created (executeBootstrap throws otherwise)
				userRepository.findByEmailIgnoreCase(environment.getProperty("BOOTSTRAP_MASTER_ADMIN_EMAIL"))
						.ifPresent(created -> activityLogService.logActivity(created, "BOOTSTRAP_MASTER_ADMIN", "SYSTEM",
								"Master Admin account created by the one-time startup bootstrap"));
			}
		};
	}

	public static void executeBootstrap(UserRepository userRepository,
	                                    PasswordEncoder passwordEncoder,
	                                    java.util.function.Function<String, String> envLookup) {
		if (!"true".equalsIgnoreCase(envLookup.apply("BOOTSTRAP_MASTER_ADMIN_ENABLED"))) {
			return;
		}

		String email = requiredBootstrapValue("BOOTSTRAP_MASTER_ADMIN_EMAIL", envLookup);
		String name = requiredBootstrapValue("BOOTSTRAP_MASTER_ADMIN_NAME", envLookup);
		String password = requiredBootstrapValue("BOOTSTRAP_MASTER_ADMIN_PASSWORD", envLookup);
		if (userRepository.countByRoleAndIsActiveTrueAndStatus(Role.MASTER_ADMIN, UserStatus.ACTIVE) > 0) {
			throw new IllegalStateException("Master Admin bootstrap is already complete; disable BOOTSTRAP_MASTER_ADMIN_ENABLED.");
		}
		if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
			throw new IllegalStateException("Bootstrap email is already associated with an account.");
		}

		User masterAdmin = new User();
		masterAdmin.setName(name);
		masterAdmin.setEmail(email);
		masterAdmin.setPassword(passwordEncoder.encode(password));
		masterAdmin.setRole(Role.MASTER_ADMIN);
		masterAdmin.setStatus(UserStatus.ACTIVE);
		masterAdmin.setActive(true);
		masterAdmin.setEmailVerified(true);
		masterAdmin.setForcePasswordChange(true);
		userRepository.save(masterAdmin);
	}

	private static String requiredBootstrapValue(String variable, java.util.function.Function<String, String> envLookup) {
		String value = envLookup.apply(variable);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(variable + " must be set for Master Admin bootstrap.");
		}
		return value;
	}
}
