package com.example.demo.wallet;

import java.math.BigDecimal;
import java.util.List;

public record WalletSummaryResponse(
        String username,
        String displayName,
        String virtualPort,
        BigDecimal balance,
        List<WalletTransactionView> transactions
) {
}
