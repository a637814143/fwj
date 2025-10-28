package com.example.demo.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountUpdateRequest(
        String requesterUsername,
        @Size(min = 3, max = 50, message = "用户名长度需在3到50个字符之间") String newUsername,
        @Size(min = 2, max = 100, message = "昵称长度需在2到100个字符之间") String displayName,
        @Size(min = 6, max = 100, message = "密码长度需在6到100个字符之间") String newPassword,
        @Size(min = 6, max = 100, message = "当前密码长度需在6到100个字符之间") String currentPassword
) {
}
