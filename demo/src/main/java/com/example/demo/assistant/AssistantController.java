package com.example.demo.assistant;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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

    // 获取 AI 助手的服务状态与额度
    @GetMapping("/status")
    public AssistantStatusResponse status() {
        return assistantService.status();
    }

    // 向 AI 助手提问并返回生成的回复
    @PostMapping("/ask")
    public AssistantResponse ask(@Valid @RequestBody AssistantQuestionRequest request) {
        return assistantService.askQuestion(request.question());
    }
}
