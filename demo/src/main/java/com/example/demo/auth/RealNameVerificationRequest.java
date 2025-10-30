package com.example.demo.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RealNameVerificationRequest(
        @NotBlank(message = "真实姓名不能为空") @Size(max = 50, message = "真实姓名长度不能超过50个字符") String realName,
        @NotBlank(message = "身份证号不能为空") @Size(max = 64, message = "身份证号长度不能超过64个字符") String idNumber,
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "\\d{11}", message = "手机号需为11位数字") String phoneNumber
) {
}
