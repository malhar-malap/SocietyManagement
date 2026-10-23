package com.society.jaswanti.service;

import com.society.jaswanti.dto.AuthResponse;
import com.society.jaswanti.dto.ProfileSetupDto;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.exception.LastMasterAdminProtectionException;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.UserRepository;
import com.society.jaswanti.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final RoomRepository roomRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       JwtService jwtService,
                       RoomRepository roomRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.roomRepository = roomRepository;
    }

    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Optional<User> updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            if (updatedUser.getName() != null) user.setName(updatedUser.getName());
            if (updatedUser.getPhone() != null) user.setPhone(updatedUser.getPhone());
            if (updatedUser.getEmail() != null) user.setEmail(updatedUser.getEmail());
            // Role or status update normally shouldn't happen directly here, but could if needed.
            return userRepository.save(user);
        });
    }

    @Transactional
    public synchronized Optional<User> updateUserStatus(Long id, String status, Authentication actor) {
        requireMasterAdmin(actor);
        return userRepository.findById(id).map(user -> {
            UserStatus requestedStatus = UserStatus.valueOf(status.toUpperCase());
            if (user.getRole() == Role.MASTER_ADMIN
                    && user.isActive()
                    && user.getStatus() == UserStatus.ACTIVE
                    && requestedStatus != UserStatus.ACTIVE
                    && activeMasterAdminCount() <= 1) {
                throw new LastMasterAdminProtectionException();
            }
            user.setStatus(requestedStatus);
            if (requestedStatus == UserStatus.ACTIVE) {
                user.setActive(true);
            }
            return userRepository.save(user);
        });
    }

    @Transactional
    public synchronized Optional<User> updateUserRole(Long id, String role, Authentication actor) {
        requireMasterAdmin(actor);
        Role requestedRole = Role.valueOf(role.toUpperCase());
        return userRepository.findById(id).map(user -> {
            if (user.getRole() == Role.MASTER_ADMIN
                    && user.isActive()
                    && user.getStatus() == UserStatus.ACTIVE
                    && requestedRole != Role.MASTER_ADMIN
                    && activeMasterAdminCount() <= 1) {
                throw new LastMasterAdminProtectionException();
            }
            user.setRole(requestedRole);
            return userRepository.save(user);
        });
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    @Transactional
    public synchronized void deleteUser(Long id, Authentication actor) {
        requireMasterAdmin(actor);
        // We use soft delete by setting isActive to false instead of hard deleting
        userRepository.findById(id).ifPresent(user -> {
            if (user.getRole() == Role.MASTER_ADMIN
                    && user.isActive()
                    && user.getStatus() == UserStatus.ACTIVE
                    && activeMasterAdminCount() <= 1) {
                throw new LastMasterAdminProtectionException();
            }
            user.setActive(false);
            userRepository.save(user);
        });
    }

    private void requireMasterAdmin(Authentication actor) {
        if (actor == null || actor.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_MASTER_ADMIN".equals(authority.getAuthority()))) {
            throw new org.springframework.security.access.AccessDeniedException("Master Admin privileges are required");
        }
    }

    private long activeMasterAdminCount() {
        return userRepository.countByRoleAndIsActiveTrueAndStatus(Role.MASTER_ADMIN, UserStatus.ACTIVE);
    }

    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(oldPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public boolean verifyEmail(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus(com.society.jaswanti.entity.UserStatus.ACTIVE);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean setupProfile(Long userId, ProfileSetupDto dto) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            String newEmail = dto.getPersonalEmail() != null ? dto.getPersonalEmail().trim().toLowerCase() : null;
            if (newEmail == null || newEmail.isBlank()) {
                throw new IllegalArgumentException("Personal email is required");
            }

            Optional<User> existing = userRepository.findByEmail(newEmail);
            if (existing.isPresent() && !existing.get().getId().equals(userId)) {
                throw new IllegalArgumentException("Email " + newEmail + " is already in use by another account");
            }

            if (dto.getName() != null && !dto.getName().isBlank()) {
                user.setName(dto.getName().trim());
            }
            if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
                user.setPhone(dto.getPhone().trim());
            }
            if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            }

            user.setPendingEmail(newEmail);

            // Generate secure 4-digit numeric OTP (1000 - 9999)
            String token = String.format("%04d", new SecureRandom().nextInt(9000) + 1000);
            user.setEmailVerificationToken(token);
            user.setEmailVerificationExpiry(LocalDateTime.now().plusHours(24));

            userRepository.save(user);

            logger.info("Generated setup verification OTP for user ID {} ({}): {}", userId, newEmail, token);

            try {
                String subject = "Jaswanti Residency - Email Verification Code";
                String body = "Hello <b>" + user.getName() + "</b>,\n\n" +
                              "Please use the following 4-digit OTP code to verify your email address:\n\n" +
                              "<div style='text-align: center; margin: 25px 0;'>" +
                              "  <span style='font-size: 32px; font-weight: bold; letter-spacing: 4px; color: #d32f2f; background-color: #fce4e4; padding: 12px 24px; border-radius: 8px; display: inline-block;'>" + token + "</span>" +
                              "</div>\n\n" +
                              "This code is valid for 24 hours. Enter this code on the setup screen to complete your profile.\n\n" +
                              "If you did not request this verification, please ignore this email.\n\n" +
                              "Regards,\n<b>Jaswanti Residency Admin</b>";
                emailService.sendEmail(newEmail, subject, body);
            } catch (Exception e) {
                logger.error("Failed to send verification email to {}: {}", newEmail, e.getMessage());
            }

            return true;
        }
        return false;
    }

    @Transactional
    public Optional<AuthResponse> verifySetupEmail(Long userId, String token) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            String cleanToken = token != null ? token.trim().replaceAll("\\s+", "") : "";
            String savedToken = user.getEmailVerificationToken() != null ? user.getEmailVerificationToken().trim() : "";

            logger.info("Attempting setup verification for user ID {}: provided='{}', stored='{}', expiry='{}', now='{}'",
                    userId, cleanToken, savedToken, user.getEmailVerificationExpiry(), LocalDateTime.now());

            boolean tokenMatches = !cleanToken.isEmpty() && cleanToken.equalsIgnoreCase(savedToken);
            boolean notExpired = user.getEmailVerificationExpiry() != null && user.getEmailVerificationExpiry().isAfter(LocalDateTime.now());

            if (tokenMatches && notExpired) {
                if (user.getPendingEmail() != null && !user.getPendingEmail().isBlank()) {
                    user.setEmail(user.getPendingEmail().trim().toLowerCase());
                }
                user.setEmailVerified(true);
                user.setVerifiedAt(LocalDateTime.now());
                user.setForcePasswordChange(false);

                user.setPendingEmail(null);
                user.setEmailVerificationToken(null);
                user.setEmailVerificationExpiry(null);

                user = userRepository.save(user);

                // Issue fresh JWT tokens with the new email as principal subject
                String accessToken = jwtService.generateToken(user);
                String refreshToken = jwtService.generateRefreshToken(user);

                List<Room> rooms = roomRepository.findByResidentId(user.getId());
                Long roomId = rooms.isEmpty() ? null : rooms.get(0).getId();

                logger.info("Setup verification successful for user ID {} (verified email: {})", user.getId(), user.getEmail());

                return Optional.of(new AuthResponse(
                        accessToken,
                        refreshToken,
                        user.getRole().name(),
                        user.getName(),
                        user.getId(),
                        roomId,
                        false
                ));
            } else {
                logger.warn("Setup verification failed for user ID {}: match={}, notExpired={}", userId, tokenMatches, notExpired);
            }
        }
        return Optional.empty();
    }
}
