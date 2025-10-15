package com.example.demo.conversation;

import java.time.OffsetDateTime;

public record ConversationMessageView(
        Long id,
        Long conversationId,
        String senderUsername,
        String senderDisplayName,
        String content,
        OffsetDateTime createdAt
) {
}
