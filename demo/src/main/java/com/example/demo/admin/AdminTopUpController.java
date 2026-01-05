package com.example.demo.admin;

import com.example.demo.wallet.AdminTopUpReviewRequest;
import com.example.demo.wallet.TopUpReviewDecision;
import com.example.demo.wallet.WalletService;
import com.example.demo.wallet.WalletTopUpView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/wallet-topups")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminTopUpController {

    private final WalletService walletService;

    public AdminTopUpController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/pending")
    public List<WalletTopUpView> listPending(@RequestParam("requester") String requesterUsername) {
        return walletService.listPendingTopUps(requesterUsername);
    }

    @PatchMapping("/{topUpId}/review")
    public WalletTopUpView review(@PathVariable Long topUpId,
                                  @Valid @RequestBody AdminTopUpReviewRequest request) {
        return walletService.reviewTopUp(topUpId, request.decision(), request.requesterUsername());
    }
}
