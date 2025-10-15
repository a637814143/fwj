package com.example.demo.wallet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WalletTransactionView(
        Long id,
        WalletTransactionType type,
        BigDecimal amount,
        String reference,
        String description,
        OffsetDateTime createdAt
) {
    public static WalletTransactionView fromEntity(WalletTransaction transaction) {
        return new WalletTransactionView(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getReference(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
