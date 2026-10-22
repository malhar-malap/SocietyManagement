package com.society.jaswanti.service;

import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.UserStatus;
import com.society.jaswanti.exception.LastMasterAdminProtectionException;
import com.society.jaswanti.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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

    public boolean setupProfile(Long userId, com.society.jaswanti.dto.ProfileSetupDto dto) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setName(dto.getName());
            user.setPhone(dto.getPhone());
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            
            user.setPendingEmail(dto.getPersonalEmail());
            // Generate 4-digit OTP
            String token = String.format("%04d", new java.util.Random().nextInt(10000));
            user.setEmailVerificationToken(token);
            user.setEmailVerificationExpiry(java.time.LocalDateTime.now().plusHours(24));
            
            userRepository.save(user);
            
            System.out.println("=================================================");
            System.out.println("Sending Email Verification for " + dto.getPersonalEmail());
            System.out.println("OTP Token: " + token);
            System.out.println("=================================================");
            
            try {
                String subject = "Jaswanti Residency - Email Verification";
                String body = "Hello <b>" + dto.getName() + "</b>,\n\n" +
                              "Please use the following 4-digit OTP to verify your email address:\n\n" +
                              "<div style='text-align: center; margin: 20px 0;'>" +
                              "  <span style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #d32f2f; background-color: #fce4e4; padding: 10px 20px; border-radius: 5px;'>" + token + "</span>" +
                              "</div>\n\n" +
                              "If you did not request this verification, please ignore this email.\n\n" +
                              "Regards,\n<b>Jaswanti Residency Admin</b>";
                emailService.sendEmail(dto.getPersonalEmail(), subject, body);
                System.out.println("Verification email sent successfully.");
            } catch (Exception e) {
                System.err.println("Failed to send verification email: " + e.getMessage());
            }
            
            return true;
        }
        return false;
    }

    public boolean verifySetupEmail(Long userId, String token) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            if (token != null && token.equals(user.getEmailVerificationToken()) 
                && user.getEmailVerificationExpiry() != null 
                && user.getEmailVerificationExpiry().isAfter(java.time.LocalDateTime.now())) {
                
                user.setEmail(user.getPendingEmail());
                user.setEmailVerified(true);
                user.setVerifiedAt(java.time.LocalDateTime.now());
                user.setForcePasswordChange(false);
                
                user.setPendingEmail(null);
                user.setEmailVerificationToken(null);
                user.setEmailVerificationExpiry(null);
                
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }
}
