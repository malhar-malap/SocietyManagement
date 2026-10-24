package com.society.jaswanti.controller;

import com.society.jaswanti.dto.ChangePasswordDto;
import com.society.jaswanti.dto.UserDto;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.society.jaswanti.entity.Role;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final com.society.jaswanti.repository.RoomRepository roomRepository;
    private final com.society.jaswanti.repository.RoomResidentRepository roomResidentRepository;

    public UserController(UserService userService,
                          com.society.jaswanti.repository.RoomRepository roomRepository,
                          com.society.jaswanti.repository.RoomResidentRepository roomResidentRepository) {
        this.userService = userService;
        this.roomRepository = roomRepository;
        this.roomResidentRepository = roomResidentRepository;
    }



    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(convertToDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user)
                .map(updated -> ResponseEntity.ok(convertToDto(updated)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    public ResponseEntity<UserDto> updateUserStatus(@PathVariable Long id, @RequestParam String status, org.springframework.security.core.Authentication authentication) {
        return userService.updateUserStatus(id, status, authentication)
                .map(updated -> ResponseEntity.ok(convertToDto(updated)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    public ResponseEntity<UserDto> updateUserRole(@PathVariable Long id, @RequestParam String role,
                                                   org.springframework.security.core.Authentication authentication) {
        return userService.updateUserRole(id, role, authentication)
                .map(updated -> ResponseEntity.ok(convertToDto(updated)))
                .orElse(ResponseEntity.notFound().build());
    }
    

    
    @GetMapping("/profile/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserDto> getProfile(@PathVariable Long id) {
        return getUserById(id);
    }

    @PutMapping("/profile/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserDto> updateProfile(@PathVariable Long id, @RequestBody com.society.jaswanti.dto.UpdateProfileRequest request, org.springframework.security.core.Authentication auth) {
        return userService.updateProfile(id, request, auth)
                .map(user -> ResponseEntity.ok(convertToDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody ChangePasswordDto dto,
                                            org.springframework.security.core.Authentication auth) {
        return userService.changePassword(id, dto.getOldPassword(), dto.getNewPassword())
                .<ResponseEntity<?>>map(tokens -> {
                    // Changing the password revokes all sessions; hand fresh tokens back only to the account owner
                    boolean self = auth != null && auth.getPrincipal() instanceof User principal && principal.getId().equals(id);
                    return self ? ResponseEntity.ok(tokens) : ResponseEntity.ok("Password changed successfully");
                })
                .orElseGet(() -> ResponseEntity.badRequest().body("Failed to change password"));
    }
    
    @PostMapping("/{id}/setup")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<String> setupProfile(@PathVariable Long id, @RequestBody com.society.jaswanti.dto.ProfileSetupDto dto) {
        try {
            boolean success = userService.setupProfile(id, dto);
            if (success) {
                return ResponseEntity.ok("Profile setup initiated. Please check your email for verification.");
            } else {
                return ResponseEntity.badRequest().body("Failed to setup profile: user not found.");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/verify-setup")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<?> verifySetupEmail(@PathVariable Long id, @RequestParam String token) {
        return userService.verifySetupEmail(id, token)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body("Invalid or expired verification token."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, org.springframework.security.core.Authentication authentication) {
        userService.deleteUser(id, authentication);
        return ResponseEntity.ok().build();
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getStatus() != null ? user.getStatus().name() : "ACTIVE",
                user.getRole() != null ? user.getRole().name() : "RESIDENT"
        );
        java.util.Set<String> roomNumbers = new java.util.LinkedHashSet<>();
        roomResidentRepository.findByResidentIdAndIsActiveTrue(user.getId())
                .forEach(rr -> {
                    if (rr.getRoom() != null && rr.getRoom().getRoomNumber() != null) {
                        roomNumbers.add(rr.getRoom().getRoomNumber());
                    }
                });
        roomRepository.findByResidentId(user.getId())
                .forEach(r -> {
                    if (r.getRoomNumber() != null) {
                        roomNumbers.add(r.getRoomNumber());
                    }
                });
        if (!roomNumbers.isEmpty()) {
            dto.setRoomNumber(String.join(", ", roomNumbers));
        }
        dto.setActive(user.isActive());
        return dto;
    }
}
