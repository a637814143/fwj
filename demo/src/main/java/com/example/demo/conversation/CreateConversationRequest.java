package com.example.demo.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
        @NotBlank(message = "买家账号不能为空") String buyerUsername,
        @NotBlank(message = "卖家账号不能为空") String sellerUsername,
        @Size(max = 2000, message = "首条消息长度不能超过 2000 字") String initialMessage
) {
}
