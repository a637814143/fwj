package com.example.demo.auth;

public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String username) {
        super(String.format("用户名 %s 已被注册，请更换后重试。", username));
    }
}
