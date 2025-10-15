package com.example.demo.wallet;

import com.example.demo.auth.UserAccount;
import com.example.demo.auth.UserAccountRepository;
import com.example.demo.order.HouseOrder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WalletService {

    private final UserWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserAccountRepository userAccountRepository;
    public WalletService(UserWalletRepository walletRepository,
                         WalletTransactionRepository transactionRepository,
                         UserAccountRepository userAccountRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public UserWallet ensureWallet(UserAccount account) {
        return walletRepository.findByUserAccount(account)
                .orElseGet(() -> walletRepository.save(createWallet(account)));
    }

    public WalletSummaryResponse getWalletSummary(String username) {
        UserAccount account = getAccount(username);
        UserWallet wallet = ensureWallet(account);
        return toSummary(account, wallet);
    }

    public WalletSummaryResponse topUp(String username, BigDecimal amount, String reference) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "充值金额必须大于0");
        }
        UserAccount account = getAccount(username);
        UserWallet wallet = ensureWallet(account);
        wallet.increase(amount);
        walletRepository.save(wallet);
        createTransaction(wallet, WalletTransactionType.TOP_UP, amount, reference, "钱包充值");
        return toSummary(account, wallet);
    }

    public void processPayment(HouseOrder order, String reference, String description) {
        BigDecimal amount = order.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付金额必须大于0");
        }
        UserWallet buyerWallet = ensureWallet(order.getBuyer());
        if (buyerWallet.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "买家钱包余额不足");
        }
        UserWallet sellerWallet = ensureWallet(order.getSeller());
        buyerWallet.decrease(amount);
        sellerWallet.increase(amount);
        walletRepository.save(buyerWallet);
        walletRepository.save(sellerWallet);
        createTransaction(buyerWallet, WalletTransactionType.PAYMENT, amount.negate(), reference, description);
        createTransaction(sellerWallet, WalletTransactionType.RECEIVE, amount, reference, description);
    }

    public void processRefund(HouseOrder order, String reference, String description) {
        BigDecimal amount = order.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "退款金额必须大于0");
        }
        UserWallet sellerWallet = ensureWallet(order.getSeller());
        if (sellerWallet.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "卖家钱包余额不足，无法完成退款");
        }
        UserWallet buyerWallet = ensureWallet(order.getBuyer());
        sellerWallet.decrease(amount);
        buyerWallet.increase(amount);
        walletRepository.save(sellerWallet);
        walletRepository.save(buyerWallet);
        createTransaction(sellerWallet, WalletTransactionType.REFUND, amount.negate(), reference, description);
        createTransaction(buyerWallet, WalletTransactionType.REFUND, amount, reference, description);
    }

    private UserAccount getAccount(String username) {
        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到指定用户账号"));
    }

    private WalletSummaryResponse toSummary(UserAccount account, UserWallet wallet) {
        List<WalletTransactionView> views = transactionRepository
                .findTop10ByWalletOrderByCreatedAtDesc(wallet)
                .stream()
                .map(WalletTransactionView::fromEntity)
                .toList();
        return new WalletSummaryResponse(
                account.getUsername(),
                account.getDisplayName(),
                wallet.getVirtualPort(),
                wallet.getBalance(),
                views
        );
    }

    private UserWallet createWallet(UserAccount account) {
        UserWallet wallet = new UserWallet();
        wallet.setUserAccount(account);
        wallet.setVirtualPort(generateVirtualPort());
        wallet.setBalance(BigDecimal.ZERO);
        return wallet;
    }

    private String generateVirtualPort() {
        String candidate;
        do {
            candidate = "VP-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 12).toUpperCase();
        } while (walletRepository.existsByVirtualPort(candidate));
        return candidate;
    }

    private void createTransaction(UserWallet wallet,
                                   WalletTransactionType type,
                                   BigDecimal amount,
                                   String reference,
                                   String description) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setReference(reference);
        transaction.setDescription(description);
        transactionRepository.save(transaction);
    }
}
