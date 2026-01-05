package com.example.demo.wallet;

import com.example.demo.auth.UserAccount;
import com.example.demo.auth.UserAccountRepository;
import com.example.demo.order.HouseOrder;
import com.example.demo.auth.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class WalletService {

    private final UserWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WalletTopUpRequestRepository topUpRequestRepository;
    private final UserAccountRepository userAccountRepository;
    public WalletService(UserWalletRepository walletRepository,
                         WalletTransactionRepository transactionRepository,
                         WalletTopUpRequestRepository topUpRequestRepository,
                         UserAccountRepository userAccountRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.topUpRequestRepository = topUpRequestRepository;
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

    public List<WalletTopUpView> listPendingTopUps(String requesterUsername) {
        UserAccount admin = getAdmin(requesterUsername);
        ensureWallet(admin);
        return topUpRequestRepository.findByStatusOrderByCreatedAtAsc(TopUpStatus.PENDING)
                .stream()
                .map(WalletTopUpView::fromEntity)
                .toList();
    }

    public WalletTopUpView reviewTopUp(Long topUpId, TopUpReviewDecision decision, String requesterUsername) {
        if (decision == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择审核结果");
        }
        UserAccount admin = getAdmin(requesterUsername);
        WalletTopUpRequest request = topUpRequestRepository.findById(topUpId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "充值申请不存在"));
        if (request.getStatus() != TopUpStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该充值申请已审核");
        }
        if (decision == TopUpReviewDecision.REJECT) {
            request.markReviewed(TopUpStatus.REJECTED, admin.getUsername());
            topUpRequestRepository.save(request);
            return WalletTopUpView.fromEntity(request);
        }
        UserWallet wallet = ensureWallet(request.getUser());
        wallet.increase(request.getAmount());
        walletRepository.save(wallet);
        createTransaction(wallet, WalletTransactionType.TOP_UP, request.getAmount(),
                request.getReference(), "钱包充值审核通过");
        request.markReviewed(TopUpStatus.APPROVED, admin.getUsername());
        topUpRequestRepository.save(request);
        return WalletTopUpView.fromEntity(request);
    }

    public WalletSummaryResponse submitTopUp(String username, BigDecimal amount, String reference) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "充值金额必须大于0");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        UserAccount account = getAccount(username);
        WalletTopUpRequest request = new WalletTopUpRequest();
        request.setUser(account);
        request.setAmount(amount);
        request.setReference(normalizeReference(reference));
        topUpRequestRepository.save(request);
        return toSummary(account, ensureWallet(account));
    }

    public void processEscrowPayment(HouseOrder order,
                                     BigDecimal paymentAmount,
                                     String reference,
                                     String description,
                                     UserAccount adminAccount) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付金额必须大于0");
        }
        if (paymentAmount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        BigDecimal amount = paymentAmount.setScale(2, RoundingMode.HALF_UP);
        UserWallet buyerWallet = ensureWallet(order.getBuyer());
        if (buyerWallet.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "买家钱包余额不足");
        }
        UserWallet adminWallet = ensureWallet(adminAccount);
        buyerWallet.decrease(amount);
        adminWallet.increase(amount);
        walletRepository.save(buyerWallet);
        walletRepository.save(adminWallet);
        String normalizedReference = normalizeReference(reference);
        createTransaction(buyerWallet, WalletTransactionType.PAYMENT, amount.negate(), normalizedReference, description);
        createTransaction(adminWallet, WalletTransactionType.RECEIVE, amount, normalizedReference, description);
    }

    public void processEscrowPayment(HouseOrder order, String reference, String description, UserAccount adminAccount) {
        processEscrowPayment(order, order.getAmount(), reference, description, adminAccount);
    }

    public void releaseEscrow(HouseOrder order,
                              String reference,
                              String description,
                              UserAccount adminAccount,
                              UserAccount recipient,
                              BigDecimal releaseAmount,
                              BigDecimal platformFee,
                              WalletTransactionType recipientTransactionType) {
        if (releaseAmount == null || releaseAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "发放金额无效");
        }
        releaseAmount = releaseAmount.setScale(2, RoundingMode.HALF_UP);
        platformFee = platformFee == null ? BigDecimal.ZERO : platformFee.setScale(2, RoundingMode.HALF_UP);
        UserWallet adminWallet = ensureWallet(adminAccount);
        if (adminWallet.getBalance().compareTo(releaseAmount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员钱包余额不足，无法完成发放");
        }
        UserWallet recipientWallet = ensureWallet(recipient);
        adminWallet.decrease(releaseAmount);
        recipientWallet.increase(releaseAmount);
        walletRepository.save(adminWallet);
        walletRepository.save(recipientWallet);
        String normalizedReference = normalizeReference(reference);
        createTransaction(adminWallet, WalletTransactionType.PAYMENT, releaseAmount.negate(), normalizedReference, description);
        createTransaction(recipientWallet, recipientTransactionType, releaseAmount, normalizedReference, description);
        if (platformFee.compareTo(BigDecimal.ZERO) > 0) {
            createTransaction(adminWallet, WalletTransactionType.RECEIVE, platformFee, normalizedReference, "平台抽成收入");
        }
    }

    public void settleSellerRepayment(UserAccount seller,
                                      UserAccount adminAccount,
                                      BigDecimal amount,
                                      String reference,
                                      String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "归还金额无效");
        }
        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);
        UserWallet sellerWallet = ensureWallet(seller);
        if (sellerWallet.getBalance().compareTo(normalizedAmount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "卖家钱包余额不足，无法归还垫付金额");
        }
        UserWallet adminWallet = ensureWallet(adminAccount);
        sellerWallet.decrease(normalizedAmount);
        adminWallet.increase(normalizedAmount);
        walletRepository.save(sellerWallet);
        walletRepository.save(adminWallet);
        String normalizedReference = normalizeReference(reference);
        createTransaction(sellerWallet, WalletTransactionType.PAYMENT, normalizedAmount.negate(), normalizedReference, description);
        createTransaction(adminWallet, WalletTransactionType.RECEIVE, normalizedAmount, normalizedReference, description);
    }

    private UserAccount getAccount(String username) {
        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到指定用户账号"));
    }

    private UserAccount getAdmin(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员账号不能为空");
        }
        UserAccount account = getAccount(username);
        if (account.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可审核充值");
        }
        if (account.isBlacklisted()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "管理员账号已被拉黑，无法审核充值");
        }
        return account;
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

    private String normalizeReference(String reference) {
        return Optional.ofNullable(reference)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(null);
    }
}
