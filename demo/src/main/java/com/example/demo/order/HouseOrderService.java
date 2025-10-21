package com.example.demo.order;

import com.example.demo.auth.UserAccount;
import com.example.demo.auth.UserAccountRepository;
import com.example.demo.conversation.ConversationService;
import com.example.demo.house.ListingStatus;
import com.example.demo.house.SecondHandHouse;
import com.example.demo.house.SecondHandHouseRepository;
import com.example.demo.wallet.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@Transactional
public class HouseOrderService {

    private final HouseOrderRepository orderRepository;
    private final SecondHandHouseRepository houseRepository;
    private final UserAccountRepository userAccountRepository;
    private final WalletService walletService;
    private final ConversationService conversationService;

    public HouseOrderService(HouseOrderRepository orderRepository,
                             SecondHandHouseRepository houseRepository,
                             UserAccountRepository userAccountRepository,
                             WalletService walletService,
                             ConversationService conversationService) {
        this.orderRepository = orderRepository;
        this.houseRepository = houseRepository;
        this.userAccountRepository = userAccountRepository;
        this.walletService = walletService;
        this.conversationService = conversationService;
    }

    @Transactional(readOnly = true)
    public List<HouseOrderResponse> findOrdersByUser(String username) {
        return orderRepository
                .findByBuyer_UsernameOrSeller_UsernameOrderByCreatedAtDesc(username, username)
                .stream()
                .map(HouseOrderResponse::fromEntity)
                .toList();
    }

    public HouseOrderResponse reserveHouse(@Valid HouseReservationRequest request) {
        SecondHandHouse house = houseRepository.findById(request.houseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "房源不存在"));
        ensureApprovedHouse(house);
        if (house.getPrice() == null || house.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "房源价格异常，无法预定");
        }
        UserAccount buyer = userAccountRepository.findByUsername(request.buyerUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "买家账号不存在"));
        if (house.getSellerUsername() == null || house.getSellerUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "房源缺少卖家账号，无法预定");
        }
        UserAccount seller = userAccountRepository.findByUsername(house.getSellerUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "卖家账号不存在"));

        if (buyer.getId().equals(seller.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能预定自己发布的房源");
        }

        if (!buyer.isRealNameVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "买家未完成实名认证，无法预定房源");
        }

        ensureNotBlacklisted(buyer, "买家账号已被加入黑名单，无法预定房源");
        ensureNotBlacklisted(seller, "卖家账号已被加入黑名单，无法接受预定");

        Optional<HouseOrder> existingReservation = orderRepository
                .findFirstByHouse_IdAndStatusOrderByCreatedAtDesc(house.getId(), OrderStatus.RESERVED);
        if (existingReservation.isPresent()) {
            HouseOrder reservation = existingReservation.get();
            if (reservation.getBuyer().getUsername().equals(buyer.getUsername())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "您已预定该房源，请耐心等待卖家处理");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该房源已有其他买家预定");
        }

        BigDecimal deposit = house.getPrice()
                .multiply(BigDecimal.valueOf(0.1))
                .setScale(2, RoundingMode.HALF_UP);

        HouseOrder order = new HouseOrder();
        order.setHouse(house);
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setAmount(deposit);
        order = orderRepository.save(order);

        String reference = "RESERVE-" + order.getId();
        String description = String.format("房源《%s》预定定金", house.getTitle());
        walletService.processPayment(order, reference, description);
        order.markReserved();
        order.setProgressStage(OrderProgressStage.RESERVED);
        order = orderRepository.save(order);

        seller.increaseReputation(1);
        buyer.increaseReputation(1);
        userAccountRepository.save(seller);
        userAccountRepository.save(buyer);

        conversationService.sendMessageBetween(
                buyer.getUsername(),
                seller.getUsername(),
                buyer.getUsername(),
                "我已经预定，可以预约时间看房"
        );

        return HouseOrderResponse.fromEntity(order);
    }

    public HouseOrderResponse scheduleViewing(Long orderId, @Valid ViewingScheduleRequest request) {
        HouseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));

        if (!order.getSeller().getUsername().equals(request.sellerUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅房源所属卖家可以预约看房时间");
        }

        if (order.getStatus() != OrderStatus.RESERVED && order.getStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前订单状态不支持预约看房");
        }

        ensureNotBlacklisted(order.getSeller(), "卖家账号已被加入黑名单，无法预约看房");
        ensureNotBlacklisted(order.getBuyer(), "买家账号已被加入黑名单，无法接受预约");

        OffsetDateTime viewingTime = parseViewingTime(request.viewingTime());
        String viewingMessage = normalizeMessage(request.message());

        order.setViewingTime(viewingTime);
        order.setViewingMessage(viewingMessage);
        if (order.getProgressStage() == OrderProgressStage.RESERVED) {
            order.setProgressStage(OrderProgressStage.VIEWED);
        }
        order = orderRepository.save(order);

        String formattedTime = viewingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        StringBuilder content = new StringBuilder("看房预约时间：").append(formattedTime);
        if (viewingMessage != null) {
            content.append("，备注：").append(viewingMessage);
        }

        conversationService.sendMessageBetween(
                order.getBuyer().getUsername(),
                order.getSeller().getUsername(),
                order.getSeller().getUsername(),
                content.toString()
        );

        return HouseOrderResponse.fromEntity(order);
    }

    public HouseOrderResponse createOrder(@Valid HouseOrderRequest request) {
        SecondHandHouse house = houseRepository.findById(request.houseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "房源不存在"));
        ensureApprovedHouse(house);
        if (house.getSellerUsername() == null || house.getSellerUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "房源缺少卖家账号，无法发起支付");
        }
        UserAccount buyer = userAccountRepository.findByUsername(request.buyerUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "买家账号不存在"));
        UserAccount seller = userAccountRepository.findByUsername(house.getSellerUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "卖家账号不存在"));
        if (buyer.getId().equals(seller.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能购买自己发布的房源");
        }

        if (!buyer.isRealNameVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "买家未完成实名认证，无法购买房源");
        }

        ensureNotBlacklisted(buyer, "买家账号已被加入黑名单，无法购买房源");
        ensureNotBlacklisted(seller, "卖家账号已被加入黑名单，无法出售房源");

        orderRepository.findFirstByHouse_IdAndStatusOrderByCreatedAtDesc(house.getId(), OrderStatus.RESERVED)
                .ifPresent(reservation -> handleExistingReservation(reservation, buyer, seller));

        HouseOrder order = new HouseOrder();
        order.setHouse(house);
        order.setBuyer(buyer);
        order.setSeller(seller);
        PaymentMethod paymentMethod = request.paymentMethod();
        order.setPaymentMethod(paymentMethod);
        BigDecimal amount = resolvePaymentAmount(house, paymentMethod);
        order.setAmount(amount);
        order = orderRepository.save(order);

        String reference = "ORDER-" + order.getId();
        String description = paymentMethod == PaymentMethod.INSTALLMENT
                ? String.format("房源《%s》分期付款（首期）", house.getTitle())
                : String.format("房源《%s》购房全款支付", house.getTitle());
        walletService.processPayment(order, reference, description);
        order.markPaid();
        order.setProgressStage(OrderProgressStage.BALANCE_PAID);
        order = orderRepository.save(order);

        seller.increaseReputation(3);
        buyer.increaseReputation(2);
        userAccountRepository.save(seller);
        userAccountRepository.save(buyer);

        return HouseOrderResponse.fromEntity(order);
    }

    public HouseOrderResponse requestReturn(Long orderId, OrderReturnRequest request) {
        HouseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前订单状态不支持退换");
        }
        if (!order.getBuyer().getUsername().equals(request.requesterUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅买家本人可以申请退换");
        }

        String reference = "ORDER-" + order.getId();
        String reason = request.reason() == null || request.reason().isBlank() ? "买家申请退换" : request.reason();
        walletService.processRefund(order, reference, reason);
        order.markReturned(reason);
        order = orderRepository.save(order);

        UserAccount buyer = order.getBuyer();
        buyer.recordReturn();
        int penalty = Math.min(20, buyer.getReturnCount() * 5);
        buyer.decreaseReputation(penalty);
        userAccountRepository.save(buyer);
        return HouseOrderResponse.fromEntity(order);
    }

    public HouseOrderResponse updateProgress(Long orderId, OrderProgressUpdateRequest request) {
        HouseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        if (!order.getSeller().getUsername().equals(request.requesterUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅卖家本人可以更新交易进度");
        }
        OrderProgressStage current = order.getProgressStage();
        OrderProgressStage target = request.stage();
        if (current == target) {
            return HouseOrderResponse.fromEntity(order);
        }
        if (!canTransition(current, target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的进度更新请求");
        }
        order.setProgressStage(target);
        if (target == OrderProgressStage.HANDED_OVER) {
            UserAccount seller = order.getSeller();
            UserAccount buyer = order.getBuyer();
            seller.increaseReputation(5);
            buyer.increaseReputation(3);
            userAccountRepository.save(seller);
            userAccountRepository.save(buyer);
        }
        order = orderRepository.save(order);
        return HouseOrderResponse.fromEntity(order);
    }

    private void ensureNotBlacklisted(UserAccount account, String message) {
        if (account.isBlacklisted()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private void ensureApprovedHouse(SecondHandHouse house) {
        if (house.getStatus() != ListingStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "房源尚未通过审核，暂不可交易");
        }
    }

    private BigDecimal resolvePaymentAmount(SecondHandHouse house, PaymentMethod paymentMethod) {
        if (paymentMethod == null || paymentMethod == PaymentMethod.FULL) {
            BigDecimal price = house.getPrice();
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "房源全款价格无效");
            }
            return price;
        }
        BigDecimal installment = house.getInstallmentMonthlyPayment();
        if (installment == null || installment.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "房源未配置有效的分期金额");
        }
        return installment;
    }

    private boolean canTransition(OrderProgressStage current, OrderProgressStage target) {
        return switch (current) {
            case RESERVED -> target == OrderProgressStage.VIEWED;
            case VIEWED -> target == OrderProgressStage.BALANCE_PAID;
            case BALANCE_PAID -> target == OrderProgressStage.HANDED_OVER;
            case HANDED_OVER -> false;
        };
    }

    private OffsetDateTime parseViewingTime(String viewingTime) {
        try {
            return OffsetDateTime.parse(viewingTime);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "预约时间格式不正确，应为ISO 8601时间格式", ex);
        }
    }

    private String normalizeMessage(String message) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void handleExistingReservation(HouseOrder reservation, UserAccount currentBuyer, UserAccount seller) {
        if (reservation.getStatus() != OrderStatus.RESERVED) {
            return;
        }

        String reference = "RESERVE-" + reservation.getId();
        if (reservation.getBuyer().getUsername().equals(currentBuyer.getUsername())) {
            String message = "购房成功，系统自动退回预付定金";
            walletService.processRefund(reservation, reference, message);
            reservation.markReturned(message);
            orderRepository.save(reservation);
            seller.increaseReputation(2);
            currentBuyer.increaseReputation(2);
            userAccountRepository.save(seller);
            userAccountRepository.save(currentBuyer);
            return;
        }

        String message = "卖家未履行预定，系统自动退回定金";
        walletService.processRefund(reservation, reference, message);
        reservation.markReturned(message);
        orderRepository.save(reservation);

        seller.recordReservationBreach();
        seller.decreaseReputation(15);
        userAccountRepository.save(seller);

        UserAccount reservedBuyer = reservation.getBuyer();
        reservedBuyer.increaseReputation(1);
        userAccountRepository.save(reservedBuyer);
    }
}
