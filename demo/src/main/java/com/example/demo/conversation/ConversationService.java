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

    private static final int MAX_MESSAGE_LENGTH = 2000;

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

        UserAccount buyer = requireBuyerAccount(buyerUsername);
        UserAccount seller = requireSellerAccount(sellerUsername);

        Conversation conversation = getOrCreateConversation(buyer, seller);

        if (StringUtils.hasText(initialMessage)) {
            sendMessageInternal(conversation, buyer, initialMessage);
        }

        Conversation persisted = conversationRepository.findById(conversation.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在"));
        return toView(persisted);
    }

    public ConversationMessageView sendMessageBetween(String buyerUsername,
                                                      String sellerUsername,
                                                      String senderUsername,
                                                      String content) {
        if (!StringUtils.hasText(buyerUsername) || !StringUtils.hasText(sellerUsername) || !StringUtils.hasText(senderUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "发送消息时参数不完整");
        }

        UserAccount buyer = requireBuyerAccount(buyerUsername);
        UserAccount seller = requireSellerAccount(sellerUsername);
        Conversation conversation = getOrCreateConversation(buyer, seller);

        UserAccount sender;
        if (buyerUsername.equals(senderUsername)) {
            sender = buyer;
        } else if (sellerUsername.equals(senderUsername)) {
            sender = seller;
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "发送方必须是会话参与者");
        }

        ConversationMessage message = sendMessageInternal(conversation, sender, content);
        return toMessageView(message);
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
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息内容长度不能超过 2000 字");
        }
        ConversationMessage message = new ConversationMessage(conversation, sender, normalized);
        ConversationMessage saved = messageRepository.save(message);
        conversation.setUpdatedAt(saved.getCreatedAt() == null ? OffsetDateTime.now() : saved.getCreatedAt());
        conversationRepository.save(conversation);
        return saved;
    }

    private Conversation getOrCreateConversation(UserAccount buyer, UserAccount seller) {
        return conversationRepository
                .findByBuyer_UsernameAndSeller_Username(buyer.getUsername(), seller.getUsername())
                .orElseGet(() -> conversationRepository.save(new Conversation(buyer, seller)));
    }

    private UserAccount requireBuyerAccount(String username) {
        UserAccount buyer = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "买家账号不存在"));
        if (buyer.getRole() != UserRole.BUYER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "指定账号不是买家角色");
        }
        return buyer;
    }

    private UserAccount requireSellerAccount(String username) {
        UserAccount seller = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "卖家账号不存在"));
        if (!seller.getRole().isSellerRole()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "指定账号不是卖家角色");
        }
        return seller;
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
