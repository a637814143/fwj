package com.example.demo.conversation;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    // 买家与卖家发起新的会话并发送首条消息
    @PostMapping
    public ConversationView startConversation(@Valid @RequestBody CreateConversationRequest request) {
        return conversationService.startConversation(request.buyerUsername(), request.sellerUsername(), request.initialMessage());
    }

    // 查询指定用户参与的会话列表
    @GetMapping
    public List<ConversationView> listConversations(@RequestParam("username") String username) {
        return conversationService.listForUser(username);
    }

    // 查看某个会话的消息记录，校验请求人身份
    @GetMapping("/{id}/messages")
    public List<ConversationMessageView> listMessages(@PathVariable("id") Long conversationId,
                                                      @RequestParam("requester") String requester) {
        return conversationService.listMessages(conversationId, requester);
    }

    // 在会话中发送新消息
    @PostMapping("/{id}/messages")
    public ConversationMessageView sendMessage(@PathVariable("id") Long conversationId,
                                               @Valid @RequestBody SendMessageRequest request) {
        return conversationService.sendMessage(conversationId, request.senderUsername(), request.content());
    }
}
