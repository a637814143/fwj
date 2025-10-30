package com.example.demo.order;

import jakarta.validation.constraints.NotBlank;

public record ViewingConfirmationRequest(
        @NotBlank(message = "请求人不能为空") String requesterUsername
) {
}
