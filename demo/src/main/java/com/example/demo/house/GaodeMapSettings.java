package com.example.demo.house;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Optional;

@ConfigurationProperties(prefix = "gaode.api")
public class GaodeMapSettings {

    private static final String DEFAULT_KEY = "8b3a5828dd1e9d106d49f5319c40f6ef";

    private final String apiKey;
    private final String jsSecurityCode;
    private final String clientIp;

    public GaodeMapSettings(String key, String jsSecurityCode, String clientIp) {
        String resolvedKey = key == null ? "" : key.trim();
        if (resolvedKey.isBlank()) {
            resolvedKey = DEFAULT_KEY;
        }
        this.apiKey = resolvedKey;
        this.jsSecurityCode = jsSecurityCode == null ? "" : jsSecurityCode.trim();
        this.clientIp = clientIp == null ? "" : clientIp.trim();
    }

    public String apiKey() {
        return apiKey;
    }

    public Optional<String> jsSecurityCode() {
        return jsSecurityCode.isBlank() ? Optional.empty() : Optional.of(jsSecurityCode);
    }

    public boolean hasApiKey() {
        return !apiKey.isBlank();
    }

    public Optional<String> clientIp() {
        return clientIp.isBlank() ? Optional.empty() : Optional.of(clientIp);
    }
}
