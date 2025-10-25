package com.example.demo.admin;

import com.example.demo.order.PayoutRecipient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminOrderReviewRequest(
        @NotBlank(message = "请求人不能为空") String requesterUsername,
        @NotNull(message = "请选择款项发放对象") PayoutRecipient decision
) {
}
