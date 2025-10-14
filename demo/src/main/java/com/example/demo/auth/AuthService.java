package com.example.demo.auth;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final Map<UserRole, Map<String, String>> credentialStore = new HashMap<>();
    private final Map<UserRole, Map<String, String>> displayNames = new HashMap<>();

    public AuthService() {
        register(UserRole.LANDLORD, "landlord01", "owner123", "房东小李");
        register(UserRole.BUYER, "buyer01", "buyer123", "买家小王");
        register(UserRole.ADMIN, "admin", "admin123", "系统管理员");
    }

    public LoginResponse login(LoginRequest request) {
        Map<String, String> userPasswords = credentialStore.get(request.getRole());
        if (userPasswords == null) {
            throw new InvalidCredentialsException();
        }

        String expectedPassword = userPasswords.get(request.getUsername());
        if (expectedPassword == null || !expectedPassword.equals(request.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String displayName = displayNames
                .getOrDefault(request.getRole(), Map.of())
                .getOrDefault(request.getUsername(), request.getUsername());

        return new LoginResponse(
                request.getRole(),
                request.getUsername(),
                displayName,
                String.format("%s，欢迎登录系统！", displayName)
        );
    }

    private void register(UserRole role, String username, String password, String displayName) {
        credentialStore.computeIfAbsent(role, key -> new HashMap<>()).put(username, password);
        displayNames.computeIfAbsent(role, key -> new HashMap<>()).put(username, displayName);
    }
}
