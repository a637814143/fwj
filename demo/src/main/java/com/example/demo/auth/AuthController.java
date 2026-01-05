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

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/register/verification-code")
    public SendVerificationCodeResponse sendRegisterVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        return authService.sendRegistrationVerificationCode(request.getEmail());
    }

    @GetMapping("/profile/{username}")
    public LoginResponse profile(@PathVariable String username) {
        return authService.profile(username);
    }

    @PostMapping("/verify/{username}")
    public LoginResponse verify(@PathVariable String username,
                                @Valid @RequestBody RealNameVerificationRequest request) {
        return authService.verifyIdentity(username, request);
    }

    @PatchMapping("/profile/{username}")
    public LoginResponse updateProfile(@PathVariable String username,
                                       @Valid @RequestBody AccountUpdateRequest request) {
        return authService.updateAccount(username, request);
    }

    @PatchMapping("/profile")
    public LoginResponse updateCurrentProfile(@Valid @RequestBody AccountUpdateRequest request) {
        return authService.updateAccount(null, request);
    }
}
