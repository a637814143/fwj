package com.example.demo.reputation;

import com.example.demo.auth.UserAccountRepository;
import com.example.demo.auth.UserRole;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reputations")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ReputationController {

    private final UserAccountRepository userAccountRepository;

    public ReputationController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/recommended")
    public RecommendationResponse recommended() {
        var sellerRecommendations = userAccountRepository
                .findTop5ByRoleInAndBlacklistedFalseOrderByReputationScoreDesc(UserRole.sellerRoles())
                .stream()
                .map(RecommendedUser::fromEntity)
                .toList();
        var buyerRecommendations = userAccountRepository
                .findTop5ByRoleAndBlacklistedFalseOrderByReputationScoreDesc(UserRole.BUYER)
                .stream()
                .map(RecommendedUser::fromEntity)
                .toList();
        return new RecommendationResponse(sellerRecommendations, buyerRecommendations);
    }
}
