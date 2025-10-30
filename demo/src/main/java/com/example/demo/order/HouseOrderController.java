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

    @GetMapping("/by-user/{username}")
    public List<HouseOrderResponse> findOrders(@PathVariable String username) {
        return houseOrderService.findOrdersByUser(username);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseOrderResponse createOrder(@Valid @RequestBody HouseOrderRequest request) {
        return houseOrderService.createOrder(request);
    }

    @PostMapping("/reserve")
    @ResponseStatus(HttpStatus.CREATED)
    public HouseOrderResponse reserveHouse(@Valid @RequestBody HouseReservationRequest request) {
        return houseOrderService.reserveHouse(request);
    }

    @PostMapping("/{orderId}/return")
    public HouseOrderResponse requestReturn(@PathVariable Long orderId,
                                            @Valid @RequestBody OrderReturnRequest request) {
        return houseOrderService.requestReturn(orderId, request);
    }

    @PostMapping("/{orderId}/viewing")
    public HouseOrderResponse scheduleViewing(@PathVariable Long orderId,
                                              @Valid @RequestBody ViewingScheduleRequest request) {
        return houseOrderService.scheduleViewing(orderId, request);
    }

    @PostMapping("/{orderId}/viewing/confirm")
    public HouseOrderResponse confirmViewing(@PathVariable Long orderId,
                                             @Valid @RequestBody ViewingConfirmationRequest request) {
        return houseOrderService.confirmViewing(orderId, request);
    }

    @PostMapping("/{orderId}/progress")
    public HouseOrderResponse updateProgress(@PathVariable Long orderId,
                                             @Valid @RequestBody OrderProgressUpdateRequest request) {
        return houseOrderService.updateProgress(orderId, request);
    }
}
