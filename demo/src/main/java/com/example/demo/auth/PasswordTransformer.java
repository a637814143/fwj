package com.example.demo.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class PasswordTransformer {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    private PasswordTransformer() {
    }

    public static String encode(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        String once = ENCODER.encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8));
        String twice = ENCODER.encodeToString(once.getBytes(StandardCharsets.UTF_8));
        return applyRot13(twice);
    }

    public static String decode(String encodedPassword) {
        if (encodedPassword == null) {
            return null;
        }
        String rotated = applyRot13(encodedPassword);
        byte[] first = DECODER.decode(rotated);
        byte[] second = DECODER.decode(first);
        return new String(second, StandardCharsets.UTF_8);
    }

    public static boolean isEncoded(String password) {
        if (password == null || password.isBlank()) {
            return false;
        }
        try {
            return encode(decode(password)).equals(password);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        if (isEncoded(encodedPassword)) {
            return encode(rawPassword).equals(encodedPassword);
        }
        return rawPassword.equals(encodedPassword);
    }

    private static String applyRot13(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch >= 'a' && ch <= 'z') {
                builder.append((char) ((ch - 'a' + 13) % 26 + 'a'));
            } else if (ch >= 'A' && ch <= 'Z') {
                builder.append((char) ((ch - 'A' + 13) % 26 + 'A'));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
