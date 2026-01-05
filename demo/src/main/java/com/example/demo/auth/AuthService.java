package com.example.demo.auth;

import com.example.demo.common.EmailService;
import com.example.demo.common.MaskingUtils;
import com.example.demo.wallet.WalletService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Locale;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final WalletService walletService;
    private final VerificationCodeService verificationCodeService;
    private final EmailService emailService;

    public AuthService(UserAccountRepository userAccountRepository,
                       WalletService walletService,
                       VerificationCodeService verificationCodeService,
                       EmailService emailService) {
        this.userAccountRepository = userAccountRepository;
        this.walletService = walletService;
        this.verificationCodeService = verificationCodeService;
        this.emailService = emailService;
    }

    @PostConstruct
    public void preloadDemoUsers() {
        migrateLegacyLandlords();
        if (userAccountRepository.count() == 0) {
            createUser(UserRole.SELLER, "seller01", "seller123", "卖家小李", "seller01@example.com");
            createUser(UserRole.BUYER, "buyer01", "buyer123", "买家小王", "buyer01@example.com");
            createUser(UserRole.ADMIN, "admin", "admin123", "系统管理员", "admin@example.com");
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
        String normalizedUsername = normalize(request.getUsername());
        if (normalizedUsername == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名不能为空");
        }
        if (userAccountRepository.existsByUsername(normalizedUsername)) {
            throw new DuplicateUsernameException(normalizedUsername);
        }
        if (request.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "暂不支持自助注册管理员账号，请联系系统管理员开通。");
        }

        String normalizedEmail = normalizeEmail(request.getEmail());
        if (normalizedEmail == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入有效的邮箱地址");
        }
        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已注册账号，请直接登录或更换邮箱。");
        }

        String normalizedPassword = normalizePassword(request.getPassword());
        if (normalizedPassword == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码不能为空");
        }

        String verificationCode = normalizeVerificationCode(request.getVerificationCode());
        verificationCodeService.verifyAndConsume(normalizedEmail, verificationCode);

        String displayName = normalize(request.getDisplayName());
        if (displayName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "昵称不能为空");
        }

        UserAccount account = createUser(
                request.getRole(),
                normalizedUsername,
                normalizedPassword,
                displayName,
                normalizedEmail
        );

        return toResponse(account, "注册成功，已为您自动登录。");
    }

    @Transactional(readOnly = true)
    public SendVerificationCodeResponse sendRegistrationVerificationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入有效的邮箱地址");
        }
        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已注册账号，请直接登录或更换邮箱。");
        }
        String code = verificationCodeService.createCode(normalizedEmail);
        boolean emailSent = emailService.sendVerificationCode(normalizedEmail, code);
        String message = emailSent
                ? "验证码已发送至邮箱，请注意查收。"
                : "邮件发送失败，已在页面展示验证码，请直接使用验证码完成注册。";
        return new SendVerificationCodeResponse(message, code, emailSent);
    }

    private UserAccount createUser(UserRole role, String username, String password, String displayName, String email) {
        UserAccount account = new UserAccount();
        account.setRole(role);
        account.setUsername(username);
        account.setPassword(password);
        account.setDisplayName(displayName);
        account.setEmail(email);
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
    public LoginResponse updateAccount(String pathUsername, AccountUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空");
        }
        String targetUsername = resolveTargetUsername(pathUsername, request.requesterUsername());
        UserAccount account = userAccountRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        validateOperatorPermissions(account, request.requesterUsername());

        boolean changed = false;

        String newUsername = normalize(request.newUsername());
        if (newUsername != null && !newUsername.equalsIgnoreCase(account.getUsername())) {
            if (userAccountRepository.existsByUsername(newUsername)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "新的登录账号已被占用，请更换后再试。");
            }
            account.setUsername(newUsername);
            changed = true;
        }

        String newDisplayName = normalize(request.displayName());
        if (newDisplayName != null && !Objects.equals(newDisplayName, account.getDisplayName())) {
            account.setDisplayName(newDisplayName);
            changed = true;
        }

        String desiredPassword = normalizePassword(request.newPassword());
        if (desiredPassword != null) {
            String currentPassword = normalize(request.currentPassword());
            if (currentPassword == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "修改密码需提供当前密码。");
            }
            if (!account.getPassword().equals(currentPassword)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前密码不正确，请重新输入。");
            }
            account.setPassword(desiredPassword);
            changed = true;
        }

        if (!changed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未检测到需要更新的内容。");
        }

        UserAccount saved = userAccountRepository.save(account);
        return toResponse(saved, "账号信息已更新。");
    }

    private String resolveTargetUsername(String pathUsername, String requesterUsername) {
        if (pathUsername != null && !pathUsername.isBlank()) {
            return pathUsername.trim();
        }
        if (requesterUsername != null && !requesterUsername.isBlank()) {
            return requesterUsername.trim();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请提供需要更新的账号。");
    }

    private void validateOperatorPermissions(UserAccount target, String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()
                || operatorUsername.equalsIgnoreCase(target.getUsername())) {
            return;
        }
        UserAccount operator = userAccountRepository.findByUsername(operatorUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "操作账号不存在"));
        if (operator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可修改其他用户信息。");
        }
        if (operator.isBlacklisted()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "操作账号已被加入黑名单。");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizePassword(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        validatePasswordStrength(normalized);
        return normalized;
    }

    private String normalizeEmail(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeVerificationCode(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入验证码");
        }
        return normalized;
    }

    private void validatePasswordStrength(String password) {
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                hasUpper = true;
            } else if (Character.isLowerCase(ch)) {
                hasLower = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            }
        }
        if (!hasUpper || !hasLower || !hasDigit) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码需包含大小写字母与数字的组合。");
        }
        if (containsSequentialPattern(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码中不允许出现连续的数字或字母序列。");
        }
    }

    private boolean containsSequentialPattern(String value) {
        char[] chars = value.toCharArray();
        int ascendingRun = 1;
        int descendingRun = 1;
        for (int i = 1; i < chars.length; i++) {
            char prev = chars[i - 1];
            char current = chars[i];

            if (isSequential(prev, current, 1)) {
                ascendingRun++;
            } else {
                ascendingRun = 1;
            }

            if (isSequential(prev, current, -1)) {
                descendingRun++;
            } else {
                descendingRun = 1;
            }

            if (ascendingRun >= 3 || descendingRun >= 3) {
                return true;
            }
        }

        return containsKeyboardSequence(value);
    }

    private boolean containsKeyboardSequence(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        String[] rows = {"1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm"};
        for (int i = 0; i <= lower.length() - 3; i++) {
            String slice = lower.substring(i, i + 3);
            String reversed = new StringBuilder(slice).reverse().toString();
            for (String row : rows) {
                if (row.contains(slice) || row.contains(reversed)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSequential(char prev, char current, int step) {
        if (Character.isDigit(prev) && Character.isDigit(current)) {
            return current - prev == step;
        }
        if (Character.isLetter(prev) && Character.isLetter(current)) {
            char prevLower = Character.toLowerCase(prev);
            char currentLower = Character.toLowerCase(current);
            return currentLower - prevLower == step;
        }
        return false;
    }
}
