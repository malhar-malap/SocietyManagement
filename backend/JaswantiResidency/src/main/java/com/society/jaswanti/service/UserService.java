package com.society.jaswanti.service;

import com.society.jaswanti.dto.AuthResponse;
import com.society.jaswanti.dto.ProfileSetupDto;
import com.society.jaswanti.dto.UpdateProfileRequest;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.entity.RoomResident;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.exception.LastMasterAdminProtectionException;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.repository.RoomResidentRepository;
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

    private static final SecureRandom RANDOM = new SecureRandom();
    static final int SETUP_CODE_VALID_MINUTES = 15;
    static final int MAX_SETUP_CODE_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final RoomRepository roomRepository;
    private final RoomResidentRepository roomResidentRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       JwtService jwtService,
                       RoomRepository roomRepository,
                       RoomResidentRepository roomResidentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.roomRepository = roomRepository;
        this.roomResidentRepository = roomResidentRepository;
    }

    private static final String TEMP_PASSWORD_LETTERS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";
    private static final String TEMP_PASSWORD_DIGITS = "23456789";

    /** 12 characters, at least one letter and one digit, without look-alike characters (0/O, 1/l/I). */
    static String generateTemporaryPassword() {
        String all = TEMP_PASSWORD_LETTERS + TEMP_PASSWORD_DIGITS;
        char[] chars = new char[12];
        chars[0] = TEMP_PASSWORD_LETTERS.charAt(RANDOM.nextInt(TEMP_PASSWORD_LETTERS.length()));
        chars[1] = TEMP_PASSWORD_DIGITS.charAt(RANDOM.nextInt(TEMP_PASSWORD_DIGITS.length()));
        for (int i = 2; i < chars.length; i++) {
            chars[i] = all.charAt(RANDOM.nextInt(all.length()));
        }
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char t = chars[i]; chars[i] = chars[j]; chars[j] = t;
        }
        return new String(chars);
    }

    private static boolean isMasterAdmin(Authentication actor) {
        return actor != null && actor.getAuthorities().stream().anyMatch(a -> "ROLE_MASTER_ADMIN".equals(a.getAuthority()));
    }

    /**
     * Creates an account that must change its password at first sign-in. Admins create residents; a Master Admin
     * may also create admins. Master Admins are only ever created by the startup bootstrap.
     */
    public User createUser(com.society.jaswanti.dto.CreateUserRequest request, Authentication actor) {
        Role role = Role.RESIDENT;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                role = Role.valueOf(request.getRole().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown role: " + request.getRole());
            }
        }
        if (role == Role.MASTER_ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Master Admin accounts cannot be created through the API");
        }
        if (role == Role.ADMIN && !isMasterAdmin(actor)) {
            throw new org.springframework.security.access.AccessDeniedException("Only a Master Admin can create administrator accounts");
        }
        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("A valid email address is required");
        }
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("Email " + email + " is already in use by another account");
        }
        String password = request.getPassword();
        String issued = null;
        if (password == null || password.isBlank()) {
            password = issued = generateTemporaryPassword();
        } else {
            com.society.jaswanti.security.PasswordValidator.validate(password);
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setActive(true);
        user.setEmailVerified(false);
        user.setForcePasswordChange(true);
        user.setPassword(passwordEncoder.encode(password));
        User saved = userRepository.save(user);
        saved.setIssuedTemporaryPassword(issued);
        return saved;
    }

    /**
     * Replaces a user's password with a one-time temporary one and ends their sessions. Admins may reset residents;
     * a Master Admin may reset anyone.
     */
    public Optional<User> resetPasswordByAdmin(Long id, Authentication actor) {
        return userRepository.findById(id).map(user -> {
            if (user.getRole() != Role.RESIDENT && !isMasterAdmin(actor)) {
                throw new org.springframework.security.access.AccessDeniedException("Only a Master Admin can reset an administrator's password");
            }
            issueTemporaryPassword(user);
            User saved = userRepository.save(user);
            saved.setIssuedTemporaryPassword(user.getIssuedTemporaryPassword());
            return saved;
        });
    }

    private void issueTemporaryPassword(User user) {
        String temporary = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporary));
        user.setForcePasswordChange(true);
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setIssuedTemporaryPassword(temporary);
    }

    private static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /**
     * Edits a user's contact details. Administrator accounts can only be edited by a Master Admin: otherwise an
     * admin could point a Master Admin's email at their own inbox and take the account over via password reset.
     */
    public Optional<User> updateUser(Long id, User updatedUser, Authentication actor) {
        return userRepository.findById(id).map(user -> {
            boolean actorIsMaster = actor != null && actor.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_MASTER_ADMIN".equals(a.getAuthority()));
            if ((user.getRole() == Role.ADMIN || user.getRole() == Role.MASTER_ADMIN) && !actorIsMaster) {
                throw new org.springframework.security.access.AccessDeniedException("Only a Master Admin can edit administrator accounts");
            }
            if (updatedUser.getName() != null && !updatedUser.getName().isBlank()) user.setName(updatedUser.getName().trim());
            if (updatedUser.getPhone() != null) user.setPhone(updatedUser.getPhone().trim());
            if (updatedUser.getEmail() != null && !updatedUser.getEmail().isBlank()) {
                String email = updatedUser.getEmail().trim().toLowerCase();
                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    throw new IllegalArgumentException("Invalid email address: " + email);
                }
                if (!email.equalsIgnoreCase(user.getEmail())) {
                    userRepository.findByEmailIgnoreCase(email)
                            .filter(other -> !other.getId().equals(id))
                            .ifPresent(other -> { throw new IllegalArgumentException("Email " + email + " is already in use by another account"); });
                    user.setEmail(email);
                    // The email is the sign-in identity: end existing sessions for the old identity
                    user.setTokenVersion(user.getTokenVersion() + 1);
                }
            }
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
            } else {
                user.setActive(false);
            }
            user.setTokenVersion(user.getTokenVersion() + 1);
            // Reactivating an account that never completed first sign-in would bring its initial (possibly
            // well-known, e.g. seeded Temp_<room>) password back to life; issue a fresh temporary password instead
            if (requestedStatus == UserStatus.ACTIVE && user.isForcePasswordChange()) {
                issueTemporaryPassword(user);
            }
            String issued = user.getIssuedTemporaryPassword();
            User saved = userRepository.save(user);
            saved.setIssuedTemporaryPassword(issued);
            return saved;
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
            user.setTokenVersion(user.getTokenVersion() + 1);
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
            user.setStatus(UserStatus.BLOCKED);
            user.setTokenVersion(user.getTokenVersion() + 1);
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

    /**
     * Changes the password and revokes all existing tokens. Returns a fresh token pair
     * so the caller's own session can continue; empty if the old password does not match.
     */
    public Optional<AuthResponse> changePassword(Long userId, String oldPassword, String newPassword) {
        com.society.jaswanti.security.PasswordValidator.validate(newPassword);
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(oldPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setTokenVersion(user.getTokenVersion() + 1);
                user = userRepository.save(user);
                return Optional.of(buildAuthResponse(user));
            }
        }
        return Optional.empty();
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        List<Room> rooms = roomRepository.findByResidentId(user.getId());
        Long roomId = null;
        if (!rooms.isEmpty()) {
            roomId = rooms.get(0).getId();
        } else {
            List<RoomResident> rrs = roomResidentRepository.findByResidentIdAndIsActiveTrue(user.getId());
            if (!rrs.isEmpty() && rrs.get(0).getRoom() != null) {
                roomId = rrs.get(0).getRoom().getId();
            }
        }

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getRole().name(),
                user.getName(),
                user.getId(),
                roomId,
                user.isForcePasswordChange()
        );
    }

    @Transactional
    public Optional<User> updateProfile(Long userId, UpdateProfileRequest request, Authentication auth) {
        return userRepository.findById(userId).map(user -> {
            if (request.getName() != null && !request.getName().isBlank()) {
                user.setName(request.getName().trim());
            }
            if (request.getPhone() != null) {
                user.setPhone(request.getPhone().trim());
            }
            logger.info("Updated profile for user ID {}: name='{}', phone='{}'", userId, user.getName(), user.getPhone());
            return userRepository.save(user);
        });
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

            Optional<User> existing = userRepository.findByEmailIgnoreCase(newEmail);
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
                com.society.jaswanti.security.PasswordValidator.validate(dto.getNewPassword());
                user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            }

            user.setPendingEmail(newEmail);

            // 6-digit code, stored only as a BCrypt hash, valid for a short window with a limited number of tries
            String token = String.format("%06d", RANDOM.nextInt(1_000_000));
            user.setEmailVerificationToken(passwordEncoder.encode(token));
            user.setEmailVerificationExpiry(LocalDateTime.now().plusMinutes(SETUP_CODE_VALID_MINUTES));
            user.setEmailVerificationAttempts(0);

            userRepository.save(user);

            logger.info("Generated setup verification OTP for user ID {} ({})", userId, newEmail);

            try {
                String subject = "Jaswanti Residency - Email Verification Code";
                String body = "Hello <b>" + user.getName() + "</b>,\n\n" +
                              "Please use the following 6-digit code to verify your email address:\n\n" +
                              "<div style='text-align: center; margin: 25px 0;'>" +
                              "  <span style='font-size: 32px; font-weight: bold; letter-spacing: 4px; color: #d32f2f; background-color: #fce4e4; padding: 12px 24px; border-radius: 8px; display: inline-block;'>" + token + "</span>" +
                              "</div>\n\n" +
                              "This code is valid for " + SETUP_CODE_VALID_MINUTES + " minutes. Enter it on the setup screen to complete your profile.\n\n" +
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
            String savedHash = user.getEmailVerificationToken();

            logger.info("Attempting setup verification for user ID {} (expiry='{}')", userId, user.getEmailVerificationExpiry());

            if (user.getEmailVerificationAttempts() >= MAX_SETUP_CODE_ATTEMPTS) {
                throw new IllegalArgumentException("Too many incorrect codes. Request a new verification code.");
            }
            boolean tokenMatches = !cleanToken.isEmpty() && savedHash != null && passwordEncoder.matches(cleanToken, savedHash);
            boolean notExpired = user.getEmailVerificationExpiry() != null && user.getEmailVerificationExpiry().isAfter(LocalDateTime.now());

            if (tokenMatches && notExpired) {
                if (user.getPendingEmail() != null && !user.getPendingEmail().isBlank()) {
                    user.setEmail(user.getPendingEmail().trim().toLowerCase());
                }
                user.setEmailVerified(true);
                user.setVerifiedAt(LocalDateTime.now());
                user.setForcePasswordChange(false);
                user.setTokenVersion(user.getTokenVersion() + 1);

                user.setPendingEmail(null);
                user.setEmailVerificationToken(null);
                user.setEmailVerificationExpiry(null);

                user = userRepository.save(user);

                logger.info("Setup verification successful for user ID {} (verified email: {})", user.getId(), user.getEmail());

                // Issue fresh JWT tokens with the new email as principal subject
                return Optional.of(buildAuthResponse(user));
            } else {
                logger.warn("Setup verification failed for user ID {}: match={}, notExpired={}", userId, tokenMatches, notExpired);
                if (!tokenMatches && savedHash != null) {
                    user.setEmailVerificationAttempts(user.getEmailVerificationAttempts() + 1);
                    userRepository.save(user);
                }
            }
        }
        return Optional.empty();
    }
}
