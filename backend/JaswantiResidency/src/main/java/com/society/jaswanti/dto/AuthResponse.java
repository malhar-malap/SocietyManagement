package com.society.jaswanti.dto;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String role;
    private String name;
    private Long userId;
    private Long roomId;
    private boolean forcePasswordChange;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String refreshToken, String role, String name, Long userId, Long roomId, boolean forcePasswordChange) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
        this.name = name;
        this.userId = userId;
        this.roomId = roomId;
        this.forcePasswordChange = forcePasswordChange;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public boolean isForcePasswordChange() { return forcePasswordChange; }
    public void setForcePasswordChange(boolean forcePasswordChange) { this.forcePasswordChange = forcePasswordChange; }
}
