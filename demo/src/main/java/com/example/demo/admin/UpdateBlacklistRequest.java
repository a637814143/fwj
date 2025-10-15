package com.example.demo.admin;

import jakarta.validation.constraints.NotBlank;

public record UpdateBlacklistRequest(
        @NotBlank(message = "请求人不能为空") String requesterUsername,
        boolean blacklisted
) {
}
