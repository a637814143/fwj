package com.example.demo.auth;

public class LoginResponse {

    private final UserRole role;
    private final String username;
    private final String displayName;
    private final String message;

    public LoginResponse(UserRole role, String username, String displayName, String message) {
        this.role = role;
        this.username = username;
        this.displayName = displayName;
        this.message = message;
    }

    public UserRole getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMessage() {
        return message;
    }
}
