package com.society.jaswanti.controller;

import com.society.jaswanti.audit.AuditTrail;
import com.society.jaswanti.audit.AuditedAction;
import com.society.jaswanti.audit.AuditExempt;

import com.society.jaswanti.dto.AuthRequest;
import com.society.jaswanti.dto.AuthResponse;
import com.society.jaswanti.dto.ForgotPasswordRequest;
import com.society.jaswanti.dto.RefreshTokenRequest;
import com.society.jaswanti.dto.ResetPasswordRequest;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.exception.LoginLockoutException;
import com.society.jaswanti.security.CustomUserDetailsService;
import com.society.jaswanti.security.JwtService;
import com.society.jaswanti.security.LoginRateLimiterService;
import com.society.jaswanti.service.ActivityLogService;
import com.society.jaswanti.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RoomRepository roomRepository;
    private final com.society.jaswanti.repository.RoomResidentRepository roomResidentRepository;
    private final com.society.jaswanti.repository.UserRepository userRepository;
    private final com.society.jaswanti.service.EmailService emailService;
    private final PasswordResetService passwordResetService;
    private final LoginRateLimiterService loginRateLimiterService;
    private final ActivityLogService activityLogService;

    public AuthController(AuthenticationManager authenticationManager,
                          CustomUserDetailsService userDetailsService,
                          JwtService jwtService,
                          RoomRepository roomRepository,
                          com.society.jaswanti.repository.RoomResidentRepository roomResidentRepository,
                          com.society.jaswanti.repository.UserRepository userRepository,
                          com.society.jaswanti.service.EmailService emailService,
                          PasswordResetService passwordResetService,
                          LoginRateLimiterService loginRateLimiterService,
                          ActivityLogService activityLogService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.roomRepository = roomRepository;
        this.roomResidentRepository = roomResidentRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordResetService = passwordResetService;
        this.loginRateLimiterService = loginRateLimiterService;
        this.activityLogService = activityLogService;
    }

    private Long resolveRoomId(Long userId) {
        if (userId == null) return null;
        List<Room> rooms = roomRepository.findByResidentId(userId);
        if (!rooms.isEmpty()) {
            return rooms.get(0).getId();
        }
        List<com.society.jaswanti.entity.RoomResident> rrs = roomResidentRepository.findByResidentIdAndIsActiveTrue(userId);
        if (!rrs.isEmpty() && rrs.get(0).getRoom() != null) {
            return rrs.get(0).getRoom().getId();
        }
        return null;
    }

    @PostMapping("/login")
    @AuditedAction(action = "LOGIN", module = "AUTH", summary = "Signed in")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String identifier = request.getIdentifier();

        AuditTrail.actorLabel(identifier);
        if (loginRateLimiterService.isBlocked(identifier, clientIp)) {
            AuditTrail.describe("Sign-in refused for '" + identifier + "': too many failed attempts (locked out)");
            throw new LoginLockoutException("Too many failed login attempts. Please try again after 15 minutes.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            AuditTrail.describe("Failed sign-in attempt for '" + identifier + "'");
            boolean justLocked = loginRateLimiterService.recordFailedAttempt(identifier, clientIp);
            if (justLocked) {
                recordLockoutActivity(identifier, clientIp);
                throw new LoginLockoutException("Too many failed login attempts. Please try again after 15 minutes.");
            }
            throw ex;
        }

        loginRateLimiterService.recordSuccessfulLogin(identifier, clientIp);

        // If authentication passes, load the user details
        User user = (User) userDetailsService.loadUserByUsername(request.getIdentifier());
        AuditTrail.actor(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        Long roomId = resolveRoomId(user.getId());

        return ResponseEntity.ok(new AuthResponse(
                accessToken,
                refreshToken,
                user.getRole().name(),
                user.getName(),
                user.getId(),
                roomId,
                user.isForcePasswordChange()
        ));
    }

    /**
     * The client address as Tomcat resolved it. X-Forwarded-For is never read here directly: Tomcat applies it only
     * for connections from a trusted proxy (server.tomcat.remoteip.internal-proxies), so a client cannot dodge the
     * per-IP limit or lock out someone else's address by sending the header itself.
     */
    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void recordLockoutActivity(String identifier, String ip) {
        try {
            String description = String.format("Login lockout triggered for identifier '%s' from IP %s", identifier, ip);
            User user = userRepository.findByEmailIgnoreCase(identifier != null ? identifier.trim() : "").orElse(null);
            if (user != null) {
                activityLogService.logActivity(user, "LOGIN_LOCKOUT", "AUTH", description);
            } else {
                // No matching account: record the attempted identifier rather than attributing the event to someone else
                activityLogService.logAnonymous(identifier, "LOGIN_LOCKOUT", "AUTH", description);
            }
        } catch (Exception e) {
            // Logging failure should not disrupt the security block
        }
    }

    @PostMapping("/refresh")
    @AuditExempt(reason = "Access tokens refresh automatically every few minutes; logging each refresh would bury real actions")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        try {
            String username = jwtService.extractUsername(refreshToken);

            if (username != null) {
                User user = (User) userDetailsService.loadUserByUsername(username);
                if (jwtService.isRefreshTokenValid(refreshToken, user)) {
                    String newAccessToken = jwtService.generateToken(user);
                    String newRefreshToken = jwtService.generateRefreshToken(user);

                    Long roomId = resolveRoomId(user.getId());

                    return ResponseEntity.ok(new AuthResponse(
                            newAccessToken,
                            newRefreshToken,
                            user.getRole().name(),
                            user.getName(),
                            user.getId(),
                            roomId,
                            user.isForcePasswordChange()
                    ));
                }
            }
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException
                 | org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            // Expired, malformed or tampered token, or the account no longer exists: the client must sign in again
        }
        return ResponseEntity.status(401).build();
    }

    /**
     * Signs the account out on the server: bumping the token version invalidates every access and refresh token
     * issued so far (on all devices), so a token copied from this browser stops working too. Always answers 204, so
     * the endpoint reveals nothing about the token it was given.
     */
    @PostMapping("/logout")
    @AuditExempt(reason = "Signing out is routine account activity, like signing in, and is not recorded in the system log")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        try {
            String refreshToken = request != null ? request.getRefreshToken() : null;
            String username = refreshToken != null ? jwtService.extractUsername(refreshToken) : null;
            if (username != null) {
                User user = (User) userDetailsService.loadUserByUsername(username);
                if (jwtService.isRefreshTokenValid(refreshToken, user)) {
                    userRepository.findById(user.getId()).ifPresent(u -> {
                        u.setTokenVersion(u.getTokenVersion() + 1);
                        userRepository.save(u);
                    });
                }
            }
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException
                 | org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            // Already invalid: nothing to revoke
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @AuditedAction(action = "REQUEST_PASSWORD_RESET", module = "AUTH", summary = "Requested a password reset code")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest body) {
        if (body == null || body.getEmail() == null || body.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
        }

        AuditTrail.actorLabel(body.getEmail());
        AuditTrail.describe("Requested a password reset code for '" + body.getEmail() + "'");
        passwordResetService.initiatePasswordReset(body.getEmail());
        return ResponseEntity.ok(Map.of("message", "If your email is registered, you will receive a password reset OTP shortly."));
    }

    @PostMapping("/reset-password")
    @AuditedAction(action = "RESET_PASSWORD", module = "AUTH", summary = "Reset a password using an emailed code")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Request body is required."));
        }

        AuditTrail.actorLabel(body.getEmail());
        AuditTrail.describe("Reset the password for '" + body.getEmail() + "' using an emailed code");
        passwordResetService.resetPassword(body.getEmail(), body.getOtp(), body.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }
}
