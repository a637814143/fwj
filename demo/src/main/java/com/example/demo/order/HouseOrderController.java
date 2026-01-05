package com.example.demo.order;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class HouseOrderController {

    private final HouseOrderService houseOrderService;

    public HouseOrderController(HouseOrderService houseOrderService) {
        this.houseOrderService = houseOrderService;
    }

    // 按用户名查询该用户相关的订单列表
    @GetMapping("/by-user/{username}")
    public List<HouseOrderResponse> findOrders(@PathVariable String username) {
        return houseOrderService.findOrdersByUser(username);
    }

    // 创建新的购房订单
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseOrderResponse createOrder(@Valid @RequestBody HouseOrderRequest request) {
        return houseOrderService.createOrder(request);
    }

    // 创建看房预约或订金订单
    @PostMapping("/reserve")
    @ResponseStatus(HttpStatus.CREATED)
    public HouseOrderResponse reserveHouse(@Valid @RequestBody HouseReservationRequest request) {
        return houseOrderService.reserveHouse(request);
    }

    // 提交订单退款申请
    @PostMapping("/{orderId}/return")
    public HouseOrderResponse requestReturn(@PathVariable Long orderId,
                                            @Valid @RequestBody OrderReturnRequest request) {
        return houseOrderService.requestReturn(orderId, request);
    }

    // 预约线下看房时间
    @PostMapping("/{orderId}/viewing")
    public HouseOrderResponse scheduleViewing(@PathVariable Long orderId,
                                              @Valid @RequestBody ViewingScheduleRequest request) {
        return houseOrderService.scheduleViewing(orderId, request);
    }

    // 确认买卖双方看房完成
    @PostMapping("/{orderId}/viewing/confirm")
    public HouseOrderResponse confirmViewing(@PathVariable Long orderId,
                                             @Valid @RequestBody ViewingConfirmationRequest request) {
        return houseOrderService.confirmViewing(orderId, request);
    }

    // 更新订单进度节点
    @PostMapping("/{orderId}/progress")
    public HouseOrderResponse updateProgress(@PathVariable Long orderId,
                                             @Valid @RequestBody OrderProgressUpdateRequest request) {
        return houseOrderService.updateProgress(orderId, request);
    }

    // 结算卖家回款或赔付
    @PostMapping("/{orderId}/seller-repay")
    public HouseOrderResponse settleSellerRepayment(@PathVariable Long orderId,
                                                    @Valid @RequestBody SellerRepayRequest request) {
        return houseOrderService.settleSellerRepayment(orderId, request);
    }
}
