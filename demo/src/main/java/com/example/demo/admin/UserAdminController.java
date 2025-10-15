package com.example.demo.admin;

import com.example.demo.auth.UserAccount;
import com.example.demo.auth.UserAccountRepository;
import com.example.demo.auth.UserRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserAdminController {

    private final UserAccountRepository userAccountRepository;

    public UserAdminController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
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
                .findByRoleOrderByReputationScoreDesc(UserRole.SELLER)
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
