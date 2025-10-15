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

    @GetMapping("/{username}")
    public WalletSummaryResponse getWallet(@PathVariable String username) {
        return walletService.getWalletSummary(username);
    }

    @PostMapping("/{username}/top-up")
    public WalletSummaryResponse topUp(@PathVariable String username, @Valid @RequestBody TopUpRequest request) {
        return walletService.topUp(username, request.getAmount(), request.getReference());
    }
}
