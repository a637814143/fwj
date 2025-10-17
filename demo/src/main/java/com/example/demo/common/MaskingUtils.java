package com.example.demo.common;

public final class MaskingUtils {

    private MaskingUtils() {
    }

    public static String maskPhoneNumber(String raw) {
        if (raw == null) {
            return null;
        }
        String phone = raw.trim();
        if (phone.isEmpty()) {
            return raw;
        }
        int length = phone.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        int prefixLength = Math.min(3, Math.max(0, length - 4));
        int suffixLength = Math.min(4, length - prefixLength);
        StringBuilder builder = new StringBuilder(length);
        builder.append(phone, 0, prefixLength);
        builder.append("****");
        if (suffixLength > 0) {
            builder.append(phone.substring(length - suffixLength));
        }
        return builder.toString();
    }
}
