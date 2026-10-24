package com.society.jaswanti.service;

import com.society.jaswanti.entity.User;
import com.society.jaswanti.exception.InvalidOtpException;
import com.society.jaswanti.exception.InvalidPasswordException;
import com.society.jaswanti.exception.PasswordResetCooldownException;
import com.society.jaswanti.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    // Constant-time dummy BCrypt hash to eliminate timing enumeration when an email does not exist
    private static final String DUMMY_HASH = "$2a$10$wK1eRjN150g0e0WvE7/2qu0Q7zWnZ16Z0wG9n1F7HjX8d0Q6sL/7m";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    // In-memory cooldown cache to ensure non-existent emails follow identical rate-limiting
    private final Map<String, LocalDateTime> nonExistentEmailCooldowns = new ConcurrentHashMap<>();

    public PasswordResetService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void initiatePasswordReset(String rawEmail) {
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }

        String email = rawEmail.trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        // Check if 60-second resend cooldown is active
        LocalDateTime lastSent = nonExistentEmailCooldowns.get(email);
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isPresent()) {
            LocalDateTime userLastSent = userOpt.get().getPasswordResetOtpLastSentAt();
            if (userLastSent != null && (lastSent == null || userLastSent.isAfter(lastSent))) {
                lastSent = userLastSent;
            }
        }

        if (lastSent != null && lastSent.plusSeconds(60).isAfter(now)) {
            throw new PasswordResetCooldownException("Please wait before requesting another OTP.");
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Cryptographically secure 6-digit random OTP
            int code = secureRandom.nextInt(1_000_000);
            String otp = String.format("%06d", code);

            // Store salted hash, 10-minute expiry, zero failed attempts, and current timestamp
            user.setPasswordResetOtpHash(passwordEncoder.encode(otp));
            user.setPasswordResetOtpExpiry(now.plusMinutes(10));
            user.setPasswordResetOtpAttempts(0);
            user.setPasswordResetOtpLastSentAt(now);
            userRepository.save(user);

            // Send via email - never log the OTP
            sendResetEmail(user.getEmail(), otp);
        } else {
            // Track unknown email request to prevent enumeration via resend cooldown
            nonExistentEmailCooldowns.put(email, now);
            cleanupCooldownCache(now);
            // Simulate BCrypt hashing delay for timing uniformity
            passwordEncoder.encode("000000");
        }
    }

    @Transactional(noRollbackFor = {InvalidOtpException.class})
    public void resetPassword(String rawEmail, String otp, String newPassword) {
        // 1. Password input validation first (independent of account existence)
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new InvalidPasswordException("Password must be at least 6 characters long.");
        }

        if (rawEmail == null || rawEmail.trim().isEmpty() || otp == null || otp.trim().isEmpty()) {
            throw new InvalidOtpException("Invalid or expired OTP.");
        }

        String email = rawEmail.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

        if (userOpt.isEmpty()) {
            // Constant-time dummy verification to mitigate timing attacks
            passwordEncoder.matches(otp.trim(), DUMMY_HASH);
            throw new InvalidOtpException("Invalid or expired OTP.");
        }

        User user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // 2. Validate OTP presence, expiration, and attempt limit
        if (user.getPasswordResetOtpHash() == null ||
            user.getPasswordResetOtpExpiry() == null ||
            user.getPasswordResetOtpExpiry().isBefore(now) ||
            user.getPasswordResetOtpAttempts() >= 5) {

            // Invalidate state if expired or attempt limit exceeded
            if (user.getPasswordResetOtpAttempts() >= 5 ||
                (user.getPasswordResetOtpExpiry() != null && user.getPasswordResetOtpExpiry().isBefore(now))) {
                user.setPasswordResetOtpHash(null);
                user.setPasswordResetOtpExpiry(null);
                user.setPasswordResetOtpAttempts(0);
                userRepository.saveAndFlush(user);
            }
            // Execute dummy match to ensure constant-time comparison matching unknown accounts
            passwordEncoder.matches(otp.trim(), DUMMY_HASH);
            throw new InvalidOtpException("Invalid or expired OTP.");
        }

        // 3. Verify OTP hash match
        boolean matches = passwordEncoder.matches(otp.trim(), user.getPasswordResetOtpHash());
        if (!matches) {
            int attempts = user.getPasswordResetOtpAttempts() + 1;
            user.setPasswordResetOtpAttempts(attempts);
            if (attempts >= 5) {
                // Lockout: Invalidate OTP
                user.setPasswordResetOtpHash(null);
                user.setPasswordResetOtpExpiry(null);
                user.setPasswordResetOtpAttempts(0);
            }
            userRepository.saveAndFlush(user);
            throw new InvalidOtpException("Invalid or expired OTP.");
        }

        // 4. Success: update password with BCrypt encoder and invalidate OTP (single-use)
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setPasswordResetOtpHash(null);
        user.setPasswordResetOtpExpiry(null);
        user.setPasswordResetOtpAttempts(0);
        user.setForcePasswordChange(false);
        userRepository.save(user);
    }

    public void clearCooldownCache() {
        nonExistentEmailCooldowns.clear();
    }

    private void sendResetEmail(String toEmail, String otp) {
        try {
            String subject = "Jaswanti Residency - Password Reset OTP";
            String body = "Hello,\n\n"
                    + "Your OTP for password reset is:\n\n"
                    + "<div style='text-align: center; margin: 20px 0;'>"
                    + "  <span style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #d32f2f; background-color: #fce4e4; padding: 10px 20px; border-radius: 5px;'>" + otp + "</span>"
                    + "</div>\n\n"
                    + "This OTP is valid for 10 minutes. If you did not request this reset, please ignore this email.\n\n"
                    + "Regards,\n<b>Jaswanti Residency Admin</b>";
            emailService.sendEmail(toEmail, subject, body);
        } catch (Exception e) {
            logger.error("Failed to send password reset email: {}", e.getMessage());
        }
    }

    private void cleanupCooldownCache(LocalDateTime now) {
        if (nonExistentEmailCooldowns.size() > 500) {
            nonExistentEmailCooldowns.entrySet().removeIf(entry -> entry.getValue().plusMinutes(2).isBefore(now));
        }
    }
}
