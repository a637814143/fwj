package com.example.demo.admin;

import com.example.demo.auth.UserAccount;
import com.example.demo.auth.UserRole;

public record UserAccountView(
        String username,
        String displayName,
        UserRole role,
        boolean blacklisted,
        int reputationScore,
        int reservationBreaches,
        int returnCount
) {
    public static UserAccountView fromEntity(UserAccount account) {
        return new UserAccountView(
                account.getUsername(),
                account.getDisplayName(),
                account.getRole(),
                account.isBlacklisted(),
                account.getReputationScore(),
                account.getReservationBreaches(),
                account.getReturnCount()
        );
    }
}
