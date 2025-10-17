package com.example.demo.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderProgressUpdateRequest(
        @NotBlank(message = "请求人账号不能为空") String requesterUsername,
        @NotNull(message = "进度阶段不能为空") OrderProgressStage stage
) {
}
