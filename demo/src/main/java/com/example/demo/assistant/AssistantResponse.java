package com.example.demo.assistant;

public record AssistantResponse(String question, String answer, String model, boolean degraded) {
}
