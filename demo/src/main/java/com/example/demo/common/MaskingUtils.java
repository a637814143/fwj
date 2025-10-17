package com.example.demo.common;

public final class MaskingUtils {

    private MaskingUtils() {
    }

    public static String maskDisplayName(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return raw;
        }
        if (value.length() == 1) {
            return value + "*";
        }
        return value.charAt(0) + "**";
    }

    public static String maskUsername(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.length() <= 2) {
            return "*".repeat(value.length());
        }
        int maskLength = Math.max(1, value.length() - 2);
        return value.substring(0, 1) + "*".repeat(maskLength) + value.substring(value.length() - 1);
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
