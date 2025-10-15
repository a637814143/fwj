package com.example.demo.reputation;

import com.example.demo.auth.UserAccount;
import com.example.demo.auth.UserRole;

public record RecommendedUser(
        String username,
        String displayName,
        UserRole role,
        int reputationScore
) {
    public static RecommendedUser fromEntity(UserAccount account) {
        return new RecommendedUser(
                account.getUsername(),
                account.getDisplayName(),
                account.getRole(),
                account.getReputationScore()
        );
    }
}
