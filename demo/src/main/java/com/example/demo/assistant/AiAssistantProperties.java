package com.example.demo.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "assistant.ai")
public class AiAssistantProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private String systemPrompt = "You are a helpful AI assistant who specializes in the Chinese second-hand housing market. "
        + "Provide concise, practical advice tailored for prospective home buyers in mainland China. "
        + "Answer in the same language as the question.";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }

    public String resolvedBaseUrl() {
        String candidate = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://api.openai.com/v1";
        return candidate.endsWith("/") ? candidate.substring(0, candidate.length() - 1) : candidate;
    }

    public String resolvedModel() {
        return StringUtils.hasText(model) ? model.trim() : "gpt-4o-mini";
    }

    public String resolvedSystemPrompt() {
        return StringUtils.hasText(systemPrompt) ? systemPrompt.trim() : "You are a helpful AI assistant.";
    }
}
