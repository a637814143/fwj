package com.example.demo.auth;

public class LoginResponse {

    private final UserRole role;
    private final String username;
    private final String displayName;
    private final boolean blacklisted;
    private final int reputationScore;
    private final String message;

    public LoginResponse(UserRole role,
                         String username,
                         String displayName,
                         boolean blacklisted,
                         int reputationScore,
                         String message) {
        this.role = role;
        this.username = username;
        this.displayName = displayName;
        this.blacklisted = blacklisted;
        this.reputationScore = reputationScore;
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

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public int getReputationScore() {
        return reputationScore;
    }

    public String getMessage() {
        return message;
    }
}
