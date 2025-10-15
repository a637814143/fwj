package com.example.demo.conversation;

import java.time.OffsetDateTime;

public record ConversationView(
        Long id,
        ConversationParticipantView buyer,
        ConversationParticipantView seller,
        OffsetDateTime updatedAt,
        MessagePreview lastMessage
) {

    public record MessagePreview(
            Long id,
            String senderUsername,
            String senderDisplayName,
            String content,
            OffsetDateTime createdAt
    ) {
    }
}
