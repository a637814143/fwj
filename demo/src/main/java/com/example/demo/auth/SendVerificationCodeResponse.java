package com.example.demo.auth;

public record SendVerificationCodeResponse(String message, String code, boolean emailSent) {
}
