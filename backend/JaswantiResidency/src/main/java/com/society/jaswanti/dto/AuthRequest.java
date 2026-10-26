package com.society.jaswanti.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthRequest {
    @NotBlank(message = "Identifier is required")
    private String identifier; // Room number or Email

    @NotBlank(message = "Password is required")
    private String password;

    public AuthRequest() {}

    public AuthRequest(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
