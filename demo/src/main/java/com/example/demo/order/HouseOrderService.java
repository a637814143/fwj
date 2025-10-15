package com.example.demo.order;

import com.example.demo.auth.UserAccount;
import com.example.demo.auth.UserAccountRepository;
import com.example.demo.house.SecondHandHouse;
import com.example.demo.house.SecondHandHouseRepository;
import com.example.demo.wallet.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class HouseOrderService {

    private final HouseOrderRepository orderRepository;
    private final SecondHandHouseRepository houseRepository;
    private final UserAccountRepository userAccountRepository;
    private final WalletService walletService;

    public HouseOrderService(HouseOrderRepository orderRepository,
                             SecondHandHouseRepository houseRepository,
                             UserAccountRepository userAccountRepository,
                             WalletService walletService) {
        this.orderRepository = orderRepository;
        this.houseRepository = houseRepository;
        this.userAccountRepository = userAccountRepository;
        this.walletService = walletService;
    }

    @Transactional(readOnly = true)
    public List<HouseOrderResponse> findOrdersByUser(String username) {
        return orderRepository
                .findByBuyer_UsernameOrSeller_UsernameOrderByCreatedAtDesc(username, username)
                .stream()
                .map(HouseOrderResponse::fromEntity)
                .toList();
    }

    public HouseOrderResponse createOrder(@Valid HouseOrderRequest request) {
        SecondHandHouse house = houseRepository.findById(request.houseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "房源不存在"));
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

        HouseOrder order = new HouseOrder();
        order.setHouse(house);
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setAmount(house.getPrice());
        order = orderRepository.save(order);

        String reference = "ORDER-" + order.getId();
        String description = String.format("房源《%s》购房支付", house.getTitle());
        walletService.processPayment(order, reference, description);
        order.markPaid();
        order = orderRepository.save(order);
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
        return HouseOrderResponse.fromEntity(order);
    }
}
