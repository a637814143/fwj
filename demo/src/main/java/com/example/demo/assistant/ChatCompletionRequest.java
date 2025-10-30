package com.example.demo.assistant;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        String model,
        List<Message> messages,
        Double temperature,
        Integer maxTokens
) {
    public ChatCompletionRequest(String model, List<Message> messages) {
        this(model, messages, null, null);
    }

    public record Message(String role, String content) {
    }
}
