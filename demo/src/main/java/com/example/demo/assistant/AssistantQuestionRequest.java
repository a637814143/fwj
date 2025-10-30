package com.example.demo.assistant;

import jakarta.validation.constraints.NotBlank;

public record AssistantQuestionRequest(@NotBlank(message = "Question must not be blank") String question) {
}
