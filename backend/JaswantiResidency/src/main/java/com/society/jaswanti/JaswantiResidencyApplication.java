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
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.entity.Room;

@SpringBootApplication
public class JaswantiResidencyApplication {

	public static void main(String[] args) {
		SpringApplication.run(JaswantiResidencyApplication.class, args);
	}

	@Bean
	public CommandLineRunner loadData(UserRepository userRepository, RoomRepository roomRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// Fix passwords for all Flyway-seeded residents (their hashes may not match)
			java.util.List<User> allUsers = userRepository.findAll();
			boolean fixed = false;
			for (User u : allUsers) {
				if (u.getRole() == Role.ADMIN && u.getEmail() != null && u.getEmail().equals("admin@jaswanti.com")) {
					boolean updated = false;
					if (!passwordEncoder.matches("admin123", u.getPassword())) {
						u.setPassword(passwordEncoder.encode("admin123"));
						updated = true;
					}
					if (u.isForcePasswordChange()) {
						u.setForcePasswordChange(false);
						updated = true;
					}
					if (updated) {
						userRepository.save(u);
						fixed = true;
					}
				} else if (u.getRole() == Role.RESIDENT) {
					// Set all resident passwords to 'resident123'
					if (!passwordEncoder.matches("resident123", u.getPassword())) {
						u.setPassword(passwordEncoder.encode("resident123"));
						userRepository.save(u);
						fixed = true;
					}
					// Clear the test email if it's already assigned to a resident so the user can reuse it
					if (u.getEmail() != null && u.getEmail().equals("malhar3016m@gmail.com")) {
						u.setEmail(null);
						u.setEmailVerified(false);
						userRepository.save(u);
						fixed = true;
					}
				}
			}
			
			if (allUsers.isEmpty()) {
				// No users at all — create defaults
				User admin = new User();
				admin.setName("Society Admin");
				admin.setEmail("admin@jaswanti.com");
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setRole(Role.ADMIN);
				admin.setStatus(UserStatus.ACTIVE);
				admin.setEmailVerified(true);
				admin.setForcePasswordChange(false);
				userRepository.save(admin);
				fixed = true;
			}
			
			if (fixed) {
				System.out.println("=========================================");
				System.out.println("Passwords synchronized successfully!");
			} else {
				System.out.println("=========================================");
				System.out.println("All passwords already in sync.");
			}
			System.out.println("ADMIN  -> email: admin@jaswanti.com | pass: admin123");
			System.out.println("ALL RESIDENTS -> pass: resident123");
			System.out.println("  e.g. resident602@jaswanti.com / resident123");
			System.out.println("=========================================");
		};
	}
}
