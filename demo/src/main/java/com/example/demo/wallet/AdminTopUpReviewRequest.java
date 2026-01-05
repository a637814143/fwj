package com.example.demo.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminTopUpReviewRequest(
        @NotBlank String requesterUsername,
        @NotNull TopUpReviewDecision decision
) {
}
