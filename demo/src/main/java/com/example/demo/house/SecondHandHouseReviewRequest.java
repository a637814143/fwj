package com.example.demo.house;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SecondHandHouseReviewRequest(
        @NotBlank(message = "请提供审核人账号") String reviewerUsername,
        @NotNull(message = "请选择审核状态") ListingStatus status,
        @Size(max = 255, message = "审核备注不能超过255个字符") String message
) {
}
