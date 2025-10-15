package com.example.demo.order;

import jakarta.validation.constraints.NotBlank;

public record ViewingScheduleRequest(
        @NotBlank(message = "卖家账号不能为空") String sellerUsername,
        @NotBlank(message = "预约时间不能为空") String viewingTime,
        String message
) {
}
