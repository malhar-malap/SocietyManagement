package com.society.jaswanti.dto;

public class UserDto {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;

    private String status;
    private String role;
    private String roomNumber; // derived
    private boolean active = true;
    // Only present in the response that created it (new account, password reset, reactivation); shown to the admin once
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String temporaryPassword;

    public String getTemporaryPassword() { return temporaryPassword; }
    public void setTemporaryPassword(String temporaryPassword) { this.temporaryPassword = temporaryPassword; }

    public UserDto() {}

    public UserDto(Long id, String email, String fullName, String phoneNumber, String status, String role) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
