package com.example.demo.auth;

import com.example.demo.common.MaskingUtils;
import com.example.demo.house.SecondHandHouse;
import com.example.demo.house.SecondHandHouseRepository;
import com.example.demo.wallet.WalletService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final WalletService walletService;
    private final SecondHandHouseRepository secondHandHouseRepository;

    public AuthService(UserAccountRepository userAccountRepository,
                      WalletService walletService,
                      SecondHandHouseRepository secondHandHouseRepository) {
        this.userAccountRepository = userAccountRepository;
        this.walletService = walletService;
        this.secondHandHouseRepository = secondHandHouseRepository;
    }

    @PostConstruct
    public void preloadDemoUsers() {
        migrateLegacyLandlords();
        if (userAccountRepository.count() == 0) {
            createUser(UserRole.SELLER, "seller01", "seller123", "卖家小李");
            createUser(UserRole.BUYER, "buyer01", "buyer123", "买家小王");
            createUser(UserRole.ADMIN, "admin", "admin123", "系统管理员");
        }
    }

    private void migrateLegacyLandlords() {
        List<UserAccount> legacyLandlords = userAccountRepository.findByRole(UserRole.LANDLORD);
        if (legacyLandlords.isEmpty()) {
            return;
        }
        legacyLandlords.forEach(account -> account.setRole(UserRole.SELLER));
        userAccountRepository.saveAll(legacyLandlords);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserAccount account = userAccountRepository
                .findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        if (!account.getPassword().equals(request.getPassword())) {
            throw new InvalidCredentialsException();
        }

        if (account.isBlacklisted()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已被加入黑名单，请联系系统管理员。");
        }

        return toResponse(account, String.format("%s，欢迎登录系统！", account.getDisplayName()));
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException(request.getUsername());
        }
        if (request.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "暂不支持自助注册管理员账号，请联系系统管理员开通。");
        }

        UserAccount account = createUser(
                request.getRole(),
                request.getUsername(),
                request.getPassword(),
                request.getDisplayName()
        );

        return toResponse(account, "注册成功，已为您自动登录。");
    }

    private UserAccount createUser(UserRole role, String username, String password, String displayName) {
        UserAccount account = new UserAccount();
        account.setRole(role);
        account.setUsername(username);
        account.setPassword(password);
        account.setDisplayName(displayName);
        UserAccount saved = userAccountRepository.save(account);
        walletService.ensureWallet(saved);
        return saved;
    }

    private LoginResponse toResponse(UserAccount account, String message) {
        return new LoginResponse(
                account.getRole(),
                account.getUsername(),
                account.getDisplayName(),
                account.isBlacklisted(),
                account.getReputationScore(),
                message,
                account.isRealNameVerified(),
                account.getRealName(),
                MaskingUtils.maskPhoneNumber(account.getPhoneNumber())
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse profile(String username) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        return toResponse(account, String.format("当前信誉分：%d", account.getReputationScore()));
    }

    @Transactional
    public LoginResponse verifyIdentity(String username, RealNameVerificationRequest request) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        boolean wasVerified = account.isRealNameVerified();
        account.setRealName(request.realName().trim());
        account.setIdNumber(request.idNumber().trim());
        account.setPhoneNumber(request.phoneNumber().trim());
        account.setRealNameVerified(true);
        account.setRealNameVerifiedAt(LocalDateTime.now());
        if (!wasVerified) {
            account.increaseReputation(10);
            if (account.getReputationScore() < 80) {
                account.setReputationScore(80);
            }
        }
        userAccountRepository.save(account);
        return toResponse(account, wasVerified ? "已更新实名认证信息。" : "实名认证成功，信誉分已提升。");
    }

    @Transactional
    public LoginResponse updateAccount(String username, AccountUpdateRequest request) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        boolean changed = false;
        boolean usernameChanged = false;
        boolean displayNameChanged = false;

        String requestedUsername = normalize(request.newUsername());
        if (StringUtils.hasText(requestedUsername) && !requestedUsername.equals(account.getUsername())) {
            if (userAccountRepository.existsByUsername(requestedUsername)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在，请更换其他用户名。");
            }
            account.setUsername(requestedUsername);
            usernameChanged = true;
            changed = true;
        }

        String requestedDisplayName = normalize(request.displayName());
        if (StringUtils.hasText(requestedDisplayName) && !requestedDisplayName.equals(account.getDisplayName())) {
            account.setDisplayName(requestedDisplayName);
            displayNameChanged = true;
            changed = true;
        }

        String requestedPassword = normalize(request.newPassword());
        if (StringUtils.hasText(requestedPassword)) {
            account.setPassword(requestedPassword);
            changed = true;
        }

        if (!changed) {
            return toResponse(account, "没有需要更新的信息。");
        }

        userAccountRepository.save(account);

        if (usernameChanged || displayNameChanged) {
            String lookupUsername = usernameChanged ? username : account.getUsername();
            List<SecondHandHouse> sellerHouses = secondHandHouseRepository.findBySellerUsername(lookupUsername);
            if (!sellerHouses.isEmpty()) {
                for (SecondHandHouse house : sellerHouses) {
                    if (usernameChanged) {
                        house.setSellerUsername(account.getUsername());
                    }
                    if (displayNameChanged) {
                        house.setSellerName(account.getDisplayName());
                    }
                }
                secondHandHouseRepository.saveAll(sellerHouses);
            }
        }

        return toResponse(account, "个人信息已更新。");
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
