package com.example.demo.admin;

import com.example.demo.auth.UserAccount;
import com.example.demo.auth.UserAccountRepository;
import com.example.demo.auth.UserRole;
import com.example.demo.conversation.Conversation;
import com.example.demo.conversation.ConversationMessageRepository;
import com.example.demo.conversation.ConversationRepository;
import com.example.demo.house.SecondHandHouseRepository;
import com.example.demo.order.HouseOrderRepository;
import com.example.demo.wallet.UserWalletRepository;
import com.example.demo.wallet.WalletTransactionRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserAdminController {

    private final UserAccountRepository userAccountRepository;
    private final HouseOrderRepository houseOrderRepository;
    private final SecondHandHouseRepository secondHandHouseRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final UserWalletRepository userWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public UserAdminController(UserAccountRepository userAccountRepository,
                               HouseOrderRepository houseOrderRepository,
                               SecondHandHouseRepository secondHandHouseRepository,
                               ConversationRepository conversationRepository,
                               ConversationMessageRepository conversationMessageRepository,
                               UserWalletRepository userWalletRepository,
                               WalletTransactionRepository walletTransactionRepository) {
        this.userAccountRepository = userAccountRepository;
        this.houseOrderRepository = houseOrderRepository;
        this.secondHandHouseRepository = secondHandHouseRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.userWalletRepository = userWalletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @GetMapping("/users")
    public List<UserAccountView> listUsers(@RequestParam("requester") String requesterUsername) {
        UserAccount requester = requireAdmin(requesterUsername);
        return userAccountRepository.findAll().stream()
                .sorted(Comparator.comparingInt(UserAccount::getReputationScore).reversed())
                .map(UserAccountView::fromEntity)
                .toList();
    }

    @PatchMapping("/users/{username}/blacklist")
    public UserAccountView updateBlacklist(@PathVariable("username") String username,
                                           @Valid @RequestBody UpdateBlacklistRequest request) {
        requireAdmin(request.requesterUsername());
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "目标账号不存在"));
        account.setBlacklisted(request.blacklisted());
        if (request.blacklisted()) {
            account.decreaseReputation(20);
        } else {
            account.increaseReputation(5);
        }
        return UserAccountView.fromEntity(userAccountRepository.save(account));
    }

    @GetMapping("/reputations")
    public ReputationOverview reputationOverview(@RequestParam("requester") String requesterUsername) {
        requireAdmin(requesterUsername);

        List<UserAccountView> sellers = userAccountRepository
                .findByRoleInOrderByReputationScoreDesc(UserRole.sellerRoles())
                .stream()
                .map(UserAccountView::fromEntity)
                .toList();

        List<UserAccountView> buyers = userAccountRepository
                .findByRoleOrderByReputationScoreDesc(UserRole.BUYER)
                .stream()
                .map(UserAccountView::fromEntity)
                .toList();

        long blacklistedCount = userAccountRepository.countByBlacklistedTrue();

        return new ReputationOverview(sellers, buyers, blacklistedCount);
    }

    @DeleteMapping("/users/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteUser(@PathVariable("username") String username,
                           @RequestParam("requester") String requesterUsername) {
        requireAdmin(requesterUsername);
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "目标账号不存在"));
        if (account.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不允许删除管理员账号");
        }

        List<Conversation> conversations = conversationRepository
                .findByBuyer_UsernameOrSeller_Username(username, username);
        if (!conversations.isEmpty()) {
            conversationMessageRepository.deleteByConversation_IdIn(
                    conversations.stream().map(Conversation::getId).toList()
            );
            conversationRepository.deleteAll(conversations);
        }
        houseOrderRepository.deleteAll(
                houseOrderRepository.findByBuyer_UsernameOrSeller_UsernameOrderByCreatedAtDesc(username, username)
        );

        userWalletRepository.findByUserAccount(account).ifPresent(wallet -> {
            walletTransactionRepository.deleteByWallet(wallet);
            userWalletRepository.delete(wallet);
        });

        if (account.getRole().isSellerRole()) {
            secondHandHouseRepository.deleteAll(secondHandHouseRepository.findBySellerUsername(username));
        }

        userAccountRepository.delete(account);
    }

    private UserAccount requireAdmin(String requesterUsername) {
        if (requesterUsername == null || requesterUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求人不能为空");
        }
        UserAccount requester = userAccountRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "请求人账号不存在"));
        if (requester.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅系统管理员可以执行该操作");
        }
        if (requester.isBlacklisted()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "请求人已被加入黑名单");
        }
        return requester;
    }
}
