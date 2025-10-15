package com.example.demo.auth;

import com.example.demo.wallet.WalletService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final WalletService walletService;

    public AuthService(UserAccountRepository userAccountRepository, WalletService walletService) {
        this.userAccountRepository = userAccountRepository;
        this.walletService = walletService;
    }

    @PostConstruct
    public void preloadDemoUsers() {
        if (userAccountRepository.count() == 0) {
            createUser(UserRole.SELLER, "seller01", "seller123", "卖家小李");
            createUser(UserRole.BUYER, "buyer01", "buyer123", "买家小王");
            createUser(UserRole.ADMIN, "admin", "admin123", "系统管理员");
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserAccount account = userAccountRepository
                .findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        if (!account.getPassword().equals(request.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return toResponse(account, String.format("%s，欢迎登录系统！", account.getDisplayName()));
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException(request.getUsername());
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
                message
        );
    }
}
