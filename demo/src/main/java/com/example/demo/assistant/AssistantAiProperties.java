package com.example.demo.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "assistant.ai")
public class AssistantAiProperties {

    private static final String DEFAULT_BASE_URL = "https://api.moonshot.cn/v1";
    private static final String DEFAULT_MODEL = "kimi-k2-turbo-preview";

    private String baseUrl = DEFAULT_BASE_URL;
    private String apiKey = "";
    private String model = DEFAULT_MODEL;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        String candidate = baseUrl == null ? "" : baseUrl.trim();
        if (candidate.isEmpty()) {
            candidate = DEFAULT_BASE_URL;
        }
        while (candidate.endsWith("/")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        this.baseUrl = candidate;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public String getModel() {
        String candidate = model == null ? "" : model.trim();
        if (candidate.isEmpty()) {
            return DEFAULT_MODEL;
        }
        return candidate;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }
}
