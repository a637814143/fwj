package com.example.demo.auth;

public class LoginResponse {

    private final UserRole role;
    private final String username;
    private final String displayName;
    private final boolean blacklisted;
    private final int reputationScore;
    private final String message;
    private final boolean realNameVerified;
    private final String realName;
    private final String maskedPhoneNumber;

    public LoginResponse(UserRole role,
                         String username,
                         String displayName,
                         boolean blacklisted,
                         int reputationScore,
                         String message,
                         boolean realNameVerified,
                         String realName,
                         String maskedPhoneNumber) {
        this.role = role;
        this.username = username;
        this.displayName = displayName;
        this.blacklisted = blacklisted;
        this.reputationScore = reputationScore;
        this.message = message;
        this.realNameVerified = realNameVerified;
        this.realName = realName;
        this.maskedPhoneNumber = maskedPhoneNumber;
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

    public boolean isRealNameVerified() {
        return realNameVerified;
    }

    public String getRealName() {
        return realName;
    }

    public String getMaskedPhoneNumber() {
        return maskedPhoneNumber;
    }
}
