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

    public UserController(UserService userService, com.society.jaswanti.repository.RoomRepository roomRepository) {
        this.userService = userService;
        this.roomRepository = roomRepository;
    }



    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(convertToDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user)
                .map(updated -> ResponseEntity.ok(convertToDto(updated)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUserStatus(@PathVariable Long id, @RequestParam String status) {
        return userService.updateUserStatus(id, status)
                .map(updated -> ResponseEntity.ok(convertToDto(updated)))
                .orElse(ResponseEntity.notFound().build());
    }
    

    
    @GetMapping("/profile/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserDto> getProfile(@PathVariable Long id) {
        return getUserById(id);
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<String> changePassword(@PathVariable Long id, @RequestBody ChangePasswordDto dto) {
        boolean success = userService.changePassword(id, dto.getOldPassword(), dto.getNewPassword());
        if (success) {
            return ResponseEntity.ok("Password changed successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to change password");
        }
    }
    
    @PostMapping("/{id}/setup")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<String> setupProfile(@PathVariable Long id, @RequestBody com.society.jaswanti.dto.ProfileSetupDto dto) {
        boolean success = userService.setupProfile(id, dto);
        if (success) {
            return ResponseEntity.ok("Profile setup initiated. Please check your email for verification.");
        } else {
            return ResponseEntity.badRequest().body("Failed to setup profile");
        }
    }

    @PostMapping("/{id}/verify-setup")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<String> verifySetupEmail(@PathVariable Long id, @RequestParam String token) {
        boolean success = userService.verifySetupEmail(id, token);
        if (success) {
            return ResponseEntity.ok("Email verified and profile setup completed successfully.");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired verification token.");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
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
        List<com.society.jaswanti.entity.Room> rooms = roomRepository.findByResidentId(user.getId());
        if (!rooms.isEmpty()) {
            dto.setRoomNumber(rooms.get(0).getRoomNumber());
        }
        return dto;
    }
}
