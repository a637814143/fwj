package com.example.demo.assistant;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/ask")
    public AssistantAnswer ask(@Valid @RequestBody AskQuestionRequest request) {
        return assistantService.askQuestion(request.question());
    }
}
