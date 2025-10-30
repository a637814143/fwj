package com.example.demo.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskQuestionRequest(
        @NotBlank(message = "问题内容不能为空")
        @Size(max = 2000, message = "问题内容长度不能超过 2000 字")
        String question
) {
}
