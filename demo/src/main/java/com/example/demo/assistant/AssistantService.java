package com.example.demo.assistant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AssistantService {

    private static final int MAX_QUESTION_LENGTH = 2000;
    private static final String SYSTEM_PROMPT = "你是一个专注于中国二手房交易平台的智能助手。"
            + "请使用简洁、专业且友好的语气回答用户的提问，提供与房产买卖、贷款、过户、城市政策等相关的实用建议。"
            + "当问题超出你的知识范围或涉及法律/投资建议时，应提醒用户联系专业人士。";

    private final AssistantAiProperties properties;

    public AssistantService(AssistantAiProperties properties) {
        this.properties = properties;
    }

    public AssistantAnswer askQuestion(String rawQuestion) {
        if (!StringUtils.hasText(rawQuestion)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "问题内容不能为空");
        }
        String question = rawQuestion.trim();
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "问题内容长度不能超过 2000 字");
        }
        if (!properties.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 问答服务尚未配置，请联系管理员。");
        }

        RestClient client = createClient();
        ChatCompletionRequest request = new ChatCompletionRequest(properties.getModel(), buildMessages(question), 0.8, null);
        try {
            ChatCompletionResponse response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
            String answer = extractAnswer(response);
            return mapToAnswer(response, answer);
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 问答服务暂时不可用，请稍后再试。", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法连接到 AI 问答服务，请稍后再试。", ex);
        }
    }

    private RestClient createClient() {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private List<ChatCompletionRequest.Message> buildMessages(String question) {
        List<ChatCompletionRequest.Message> messages = new ArrayList<>();
        messages.add(new ChatCompletionRequest.Message("system", SYSTEM_PROMPT));
        messages.add(new ChatCompletionRequest.Message("user", question));
        return messages;
    }

    private String extractAnswer(ChatCompletionResponse response) {
        if (response == null || CollectionUtils.isEmpty(response.choices())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 问答服务暂时不可用，请稍后再试。");
        }
        return response.choices().stream()
                .map(ChatCompletionResponse.Choice::message)
                .filter(Objects::nonNull)
                .map(ChatCompletionResponse.Message::content)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 问答服务暂时不可用，请稍后再试。"));
    }

    private AssistantAnswer mapToAnswer(ChatCompletionResponse response, String answer) {
        ChatCompletionResponse.Usage usage = response == null ? null : response.usage();
        AssistantAnswer.TokenUsage tokenUsage = null;
        if (usage != null) {
            tokenUsage = new AssistantAnswer.TokenUsage(
                    usage.promptTokens(),
                    usage.completionTokens(),
                    usage.totalTokens()
            );
        }
        return new AssistantAnswer(answer, response == null ? null : response.model(), tokenUsage);
    }
}
