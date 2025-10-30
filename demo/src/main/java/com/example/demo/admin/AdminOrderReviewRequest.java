package com.example.demo.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminOrderReviewRequest(
        @NotBlank(message = "请求人不能为空") String requesterUsername,
        @NotNull(message = "请选择审核结果") AdminReviewDecision decision
) {
}
