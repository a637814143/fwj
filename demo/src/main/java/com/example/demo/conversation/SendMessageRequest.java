package com.example.demo.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "发送人账号不能为空") String senderUsername,
        @NotBlank(message = "消息内容不能为空")
        @Size(max = 2000, message = "消息内容长度不能超过 2000 字") String content
) {
}
