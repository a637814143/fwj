package com.example.demo.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final RestClient restClient;
    private final AiAssistantProperties properties;
    private final ObjectMapper objectMapper;

    public AssistantService(AiAssistantProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
            .baseUrl(properties.resolvedBaseUrl())
            .build();
    }

    public AssistantStatusResponse status() {
        return new AssistantStatusResponse(properties.hasApiKey(), properties.resolvedModel());
    }

    public AssistantResponse askQuestion(String question) {
        if (!properties.hasApiKey()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI assistant is not configured");
        }

        String trimmed = question != null ? question.trim() : "";
        if (!StringUtils.hasText(trimmed)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question must not be blank");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", properties.resolvedModel());

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
            "role", "system",
            "content", properties.resolvedSystemPrompt()
        ));
        messages.add(Map.of(
            "role", "user",
            "content", trimmed
        ));
        payload.put("messages", messages);

        try {
            AssistantChatResponse response = restClient.post()
                .uri("chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(AssistantChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from AI provider");
            }

            String answer = response.choices().stream()
                .map(AssistantChatChoice::message)
                .filter(msg -> msg != null && StringUtils.hasText(msg.content()))
                .map(AssistantChatMessage::content)
                .findFirst()
                .map(String::trim)
                .orElse("Unable to generate a valid answer, please try again with a different question.");

            return new AssistantResponse(trimmed, answer, properties.resolvedModel(), false);
        } catch (RestClientResponseException ex) {
            String providerMessage = resolveProviderErrorMessage(ex.getResponseBodyAsString());
            log.warn("AI provider returned an error: status={}, message={}", ex.getStatusCode(), providerMessage, ex);
            return fallbackResponse(trimmed, providerMessage);
        } catch (RestClientException ex) {
            log.warn("Failed to contact AI provider: {}", ex.getMessage(), ex);
            return fallbackResponse(trimmed, "无法连接到智能助手服务");
        }
    }

    private String resolveProviderErrorMessage(String body) {
        if (!StringUtils.hasText(body)) {
            return "AI provider returned an unexpected error.";
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode errorNode = node.path("error");
            if (errorNode.isMissingNode()) {
                return "AI provider returned an unexpected error.";
            }
            JsonNode messageNode = errorNode.path("message");
            if (messageNode.isMissingNode() || !messageNode.isTextual()) {
                return errorNode.toString();
            }
            return messageNode.asText();
        } catch (IOException e) {
            return body;
        }
    }

    private AssistantResponse fallbackResponse(String question, String diagnosticMessage) {
        StringBuilder builder = new StringBuilder();
        builder.append("抱歉，当前智能助手暂时无法连接到云端服务，因此未能生成专属回答。")
            .append("\n\n")
            .append("您可以先参考以下二手房交易的常见建议：")
            .append("\n1. 核验房屋的产权、抵押及查封情况；")
            .append("\n2. 实地检查房屋结构、漏水与电路等潜在问题；")
            .append("\n3. 在签约前确认税费承担、贷款安排与违约责任等关键条款。")
            .append("\n\n稍后请再次尝试提问，或联系平台客服获取人工协助。");

        if (StringUtils.hasText(diagnosticMessage)) {
            builder.append("\n\n诊断信息：").append(diagnosticMessage.trim());
        }

        return new AssistantResponse(question, builder.toString(), properties.resolvedModel(), true);
    }

    private record AssistantChatResponse(List<AssistantChatChoice> choices) {}

    private record AssistantChatChoice(AssistantChatMessage message) {}

    private record AssistantChatMessage(String role, String content) {}
}
