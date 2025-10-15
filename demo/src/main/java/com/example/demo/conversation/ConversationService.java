package com.example.demo.conversation;

import com.example.demo.auth.UserAccount;
import com.example.demo.auth.UserAccountRepository;
import com.example.demo.auth.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final UserAccountRepository userAccountRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMessageRepository messageRepository,
                               UserAccountRepository userAccountRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public ConversationView startConversation(String buyerUsername, String sellerUsername, String initialMessage) {
        if (!StringUtils.hasText(buyerUsername) || !StringUtils.hasText(sellerUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "买家和卖家账号不能为空");
        }
        if (Objects.equals(buyerUsername, sellerUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法与自己发起对话");
        }

        UserAccount buyer = userAccountRepository.findByUsername(buyerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "买家账号不存在"));
        if (buyer.getRole() != UserRole.BUYER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有买家角色可以主动发起对话");
        }

        UserAccount seller = userAccountRepository.findByUsername(sellerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "卖家账号不存在"));
        if (seller.getRole() != UserRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "指定账号不是合法的卖家角色");
        }

        Conversation conversation = conversationRepository
                .findByBuyer_UsernameAndSeller_Username(buyerUsername, sellerUsername)
                .orElseGet(() -> conversationRepository.save(new Conversation(buyer, seller)));

        if (StringUtils.hasText(initialMessage)) {
            sendMessageInternal(conversation, buyer, initialMessage);
        }

        Conversation persisted = conversationRepository.findById(conversation.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在"));
        return toView(persisted);
    }

    @Transactional(readOnly = true)
    public List<ConversationView> listForUser(String username) {
        if (!StringUtils.hasText(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "查询对话时必须提供用户名");
        }
        return conversationRepository.findByParticipant(username).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationMessageView> listMessages(Long conversationId, String requester) {
        Conversation conversation = getConversationForParticipant(conversationId, requester);
        return messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(this::toMessageView)
                .toList();
    }

    public ConversationMessageView sendMessage(Long conversationId, String senderUsername, String content) {
        Conversation conversation = getConversationForParticipant(conversationId, senderUsername);
        UserAccount sender = getParticipant(conversation, senderUsername);
        ConversationMessage message = sendMessageInternal(conversation, sender, content);
        return toMessageView(message);
    }

    private Conversation getConversationForParticipant(Long conversationId, String username) {
        if (conversationId == null || !StringUtils.hasText(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "查询会话信息时参数不完整");
        }
        return conversationRepository.findByIdAndParticipant(conversationId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到相关会话或无权访问"));
    }

    private ConversationMessage sendMessageInternal(Conversation conversation, UserAccount sender, String content) {
        if (!StringUtils.hasText(content)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息内容不能为空");
        }
        String normalized = content.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息内容不能为空");
        }
        ConversationMessage message = new ConversationMessage(conversation, sender, normalized);
        ConversationMessage saved = messageRepository.save(message);
        conversation.setUpdatedAt(saved.getCreatedAt() == null ? OffsetDateTime.now() : saved.getCreatedAt());
        conversationRepository.save(conversation);
        return saved;
    }

    private UserAccount getParticipant(Conversation conversation, String username) {
        if (conversation.getBuyer().getUsername().equals(username)) {
            return conversation.getBuyer();
        }
        if (conversation.getSeller().getUsername().equals(username)) {
            return conversation.getSeller();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "您无权参与该会话");
    }

    private ConversationView toView(Conversation conversation) {
        ConversationParticipantView buyer = new ConversationParticipantView(
                conversation.getBuyer().getUsername(),
                conversation.getBuyer().getDisplayName()
        );
        ConversationParticipantView seller = new ConversationParticipantView(
                conversation.getSeller().getUsername(),
                conversation.getSeller().getDisplayName()
        );
        ConversationView.MessagePreview lastMessage = messageRepository
                .findFirstByConversation_IdOrderByCreatedAtDesc(conversation.getId())
                .map(message -> new ConversationView.MessagePreview(
                        message.getId(),
                        message.getSender().getUsername(),
                        message.getSender().getDisplayName(),
                        message.getContent(),
                        message.getCreatedAt()
                ))
                .orElse(null);
        return new ConversationView(conversation.getId(), buyer, seller, conversation.getUpdatedAt(), lastMessage);
    }

    private ConversationMessageView toMessageView(ConversationMessage message) {
        return new ConversationMessageView(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getUsername(),
                message.getSender().getDisplayName(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
