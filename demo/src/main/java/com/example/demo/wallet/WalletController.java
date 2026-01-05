package com.example.demo.wallet;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    // 查询指定用户的钱包余额与积分
    @GetMapping("/{username}")
    public WalletSummaryResponse getWallet(@PathVariable String username) {
        return walletService.getWalletSummary(username);
    }

    // 为指定用户充值，并记录外部流水号
    @PostMapping("/{username}/top-up")
    public WalletSummaryResponse topUp(@PathVariable String username, @Valid @RequestBody TopUpRequest request) {
        return walletService.submitTopUp(username, request.getAmount(), request.getReference());
    }
}
