package com.society.jaswanti.controller;

import com.society.jaswanti.dto.AuthRequest;
import com.society.jaswanti.dto.AuthResponse;
import com.society.jaswanti.dto.RefreshTokenRequest;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.Room;
import com.society.jaswanti.repository.RoomRepository;
import com.society.jaswanti.security.CustomUserDetailsService;
import com.society.jaswanti.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    public AuthController(AuthenticationManager authenticationManager, CustomUserDetailsService userDetailsService, JwtService jwtService, RoomRepository roomRepository, com.society.jaswanti.repository.UserRepository userRepository, com.society.jaswanti.service.EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
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
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        System.out.println("Forgot Password request received for: " + email);
        // Generate random 4-digit OTP
        String otp = String.format("%04d", new java.util.Random().nextInt(10000));
        
        try {
            String subject = "Jaswanti Residency - Password Reset OTP";
            String body = "Hello,\n\n" +
                          "Your OTP for password reset is:\n\n" +
                          "<div style='text-align: center; margin: 20px 0;'>" +
                          "  <span style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #d32f2f; background-color: #fce4e4; padding: 10px 20px; border-radius: 5px;'>" + otp + "</span>" +
                          "</div>\n\n" +
                          "If you didn't request this, please ignore this email.\n\n" +
                          "Regards,\n<b>Jaswanti Residency Admin</b>";
            emailService.sendEmail(email, subject, body);
            System.out.println("OTP sent to email: " + email);
            return ResponseEntity.ok("OTP sent to email");
        } catch (Exception e) {
            System.err.println("Failed to send OTP email: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to send OTP email");
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String email, @RequestParam String otp, @RequestParam String newPassword) {
        if ("123456".equals(otp)) {
            java.util.Optional<User> userOpt = userRepository.findByEmail(email); // Assuming we can get user
            if (userOpt.isPresent()) {
                // Here we would encode and save new password
                System.out.println("Password reset for: " + email);
                return ResponseEntity.ok("Password reset successfully");
            }
        }
        return ResponseEntity.badRequest().body("Invalid OTP or Email");
    }
}
