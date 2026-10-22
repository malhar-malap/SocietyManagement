package com.society.jaswanti.controller;

import com.society.jaswanti.dto.AuthRequest;
import com.society.jaswanti.dto.AuthResponse;
import com.society.jaswanti.dto.ForgotPasswordRequest;
import com.society.jaswanti.dto.RefreshTokenRequest;
import com.society.jaswanti.dto.ResetPasswordRequest;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.security.CustomUserDetailsService;
import com.society.jaswanti.security.JwtService;
import com.society.jaswanti.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RoomRepository roomRepository;
    private final com.society.jaswanti.repository.UserRepository userRepository;
    private final com.society.jaswanti.service.EmailService emailService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthenticationManager authenticationManager,
                          CustomUserDetailsService userDetailsService,
                          JwtService jwtService,
                          RoomRepository roomRepository,
                          com.society.jaswanti.repository.UserRepository userRepository,
                          com.society.jaswanti.service.EmailService emailService,
                          PasswordResetService passwordResetService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword())
        );

        // If authentication passes, load the user details
        User user = (User) userDetailsService.loadUserByUsername(request.getIdentifier());

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        List<Room> rooms = roomRepository.findByResidentId(user.getId());
        Long roomId = rooms.isEmpty() ? null : rooms.get(0).getId();

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

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String username = jwtService.extractUsername(refreshToken);

        if (username != null) {
            User user = (User) userDetailsService.loadUserByUsername(username);
            if (jwtService.isTokenValid(refreshToken, user)) {
                String accessToken = jwtService.generateToken(user);

                List<Room> rooms = roomRepository.findByResidentId(user.getId());
                Long roomId = rooms.isEmpty() ? null : rooms.get(0).getId();

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
        }
        return ResponseEntity.status(401).build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest body) {
        if (body == null || body.getEmail() == null || body.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
        }

        passwordResetService.initiatePasswordReset(body.getEmail());
        return ResponseEntity.ok(Map.of("message", "If your email is registered, you will receive a password reset OTP shortly."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Request body is required."));
        }

        passwordResetService.resetPassword(body.getEmail(), body.getOtp(), body.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }
}
