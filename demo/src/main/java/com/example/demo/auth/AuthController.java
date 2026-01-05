package com.example.demo.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 用户登录，返回登录成功后的会话信息
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // 用户注册新账号并返回登录态
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // 发送注册验证码到邮箱
    @PostMapping("/register/verification-code")
    public SendVerificationCodeResponse sendRegisterVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        return authService.sendRegistrationVerificationCode(request.getEmail());
    }

    // 根据用户名获取个人资料
    @GetMapping("/profile/{username}")
    public LoginResponse profile(@PathVariable String username) {
        return authService.profile(username);
    }

    // 实名认证身份信息
    @PostMapping("/verify/{username}")
    public LoginResponse verify(@PathVariable String username,
                                @Valid @RequestBody RealNameVerificationRequest request) {
        return authService.verifyIdentity(username, request);
    }

    // 更新指定用户的账号信息
    @PatchMapping("/profile/{username}")
    public LoginResponse updateProfile(@PathVariable String username,
                                       @Valid @RequestBody AccountUpdateRequest request) {
        return authService.updateAccount(username, request);
    }

    // 更新当前登录用户的账号信息
    @PatchMapping("/profile")
    public LoginResponse updateCurrentProfile(@Valid @RequestBody AccountUpdateRequest request) {
        return authService.updateAccount(null, request);
    }
}
