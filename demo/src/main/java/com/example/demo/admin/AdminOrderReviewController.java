package com.example.demo.admin;

import com.example.demo.order.HouseOrderResponse;
import com.example.demo.order.HouseOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminOrderReviewController {

    private final HouseOrderService orderService;

    public AdminOrderReviewController(HouseOrderService orderService) {
        this.orderService = orderService;
    }

    // 管理员查看待审核的赔付/退款申请
    @GetMapping("/pending")
    public List<HouseOrderResponse> listPending(@RequestParam("requester") String requesterUsername) {
        return orderService.listPendingAdminReviews(requesterUsername);
    }

    // 管理员审核订单赔付结果
    @PatchMapping("/{orderId}/review")
    public HouseOrderResponse review(@PathVariable Long orderId,
                                     @Valid @RequestBody AdminOrderReviewRequest request) {
        return orderService.reviewPayout(orderId, request.decision(), request.requesterUsername());
    }
}
