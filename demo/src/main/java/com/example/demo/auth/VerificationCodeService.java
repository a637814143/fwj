package com.example.demo.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class VerificationCodeService {

    private static final Duration CODE_EXPIRATION = Duration.ofMinutes(10);
    private static final Duration RESEND_INTERVAL = Duration.ofMinutes(1);

    private static final class CodeEntry {
        private final String code;
        private final Instant expiresAt;
        private final Instant lastSentAt;

        private CodeEntry(String code, Instant expiresAt, Instant lastSentAt) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.lastSentAt = lastSentAt;
        }
    }

    private final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();

    public String createCode(String email) {
        String key = normalizeKey(email);
        Instant now = Instant.now();
        CodeEntry existing = codes.get(key);
        if (existing != null && existing.expiresAt.isAfter(now)
                && existing.lastSentAt.plus(RESEND_INTERVAL).isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "验证码请求过于频繁，请稍后再试。");
        }
        String code = String.format(Locale.ROOT, "%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        codes.put(key, new CodeEntry(code, now.plus(CODE_EXPIRATION), now));
        return code;
    }

    public void verifyAndConsume(String email, String code) {
        String key = normalizeKey(email);
        CodeEntry entry = codes.get(key);
        if (entry == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先获取验证码");
        }
        Instant now = Instant.now();
        if (entry.expiresAt.isBefore(now)) {
            codes.remove(key);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码已过期，请重新获取");
        }
        if (!Objects.equals(entry.code, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码不正确，请重新输入");
        }
        codes.remove(key);
    }

    private String normalizeKey(String email) {
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱不能为空");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
