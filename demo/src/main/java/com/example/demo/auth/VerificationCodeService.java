package com.example.demo.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeService.class);

    private static final int EXPIRATION_MINUTES = 10;

    private final Map<String, VerificationCode> codeStore = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public void sendEmailCode(String email) {
        String normalizedEmail = normalize(email);
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("email must not be blank");
        }
        String code = generateCode();
        codeStore.put(normalizedEmail, new VerificationCode(code, LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES)));
        log.info("[register] send email code {} to {} (mock send)", code, normalizedEmail);
    }

    public boolean validateCode(String email, String code) {
        String normalizedEmail = normalize(email);
        String normalizedCode = normalize(code);
        if (normalizedEmail == null || normalizedCode == null) {
            return false;
        }
        VerificationCode existing = codeStore.get(normalizedEmail);
        if (existing == null || existing.expiresAt().isBefore(LocalDateTime.now())) {
            codeStore.remove(normalizedEmail);
            return false;
        }
        boolean valid = Objects.equals(existing.code(), normalizedCode);
        if (valid) {
            codeStore.remove(normalizedEmail);
        }
        return valid;
    }

    private String generateCode() {
        int value = 100000 + random.nextInt(900000);
        return String.valueOf(value);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record VerificationCode(String code, LocalDateTime expiresAt) {
    }
}
