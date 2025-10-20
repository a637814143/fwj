package com.example.demo.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findByConversation_IdOrderByCreatedAtAsc(Long conversationId);

    Optional<ConversationMessage> findFirstByConversation_IdOrderByCreatedAtDesc(Long conversationId);

    void deleteByConversation_IdIn(Collection<Long> conversationIds);

    void deleteBySender_Username(String username);
}
