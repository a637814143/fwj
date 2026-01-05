package com.example.demo.wallet;

import com.example.demo.auth.UserAccount;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WalletTopUpView(
        Long id,
        String username,
        String displayName,
        String role,
        BigDecimal amount,
        String reference,
        String status,
        OffsetDateTime createdAt
) {
    public static WalletTopUpView fromEntity(WalletTopUpRequest request) {
        UserAccount user = request.getUser();
        return new WalletTopUpView(
                request.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole().name(),
                request.getAmount(),
                request.getReference(),
                request.getStatus().name(),
                request.getCreatedAt()
        );
    }
}
