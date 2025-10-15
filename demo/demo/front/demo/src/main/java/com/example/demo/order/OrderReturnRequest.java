package com.example.demo.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderReturnRequest(
        @NotBlank(message = "申请人账号不能为空") String requesterUsername,
        @Size(max = 255, message = "退换原因不能超过255个字符") String reason
) {
}
