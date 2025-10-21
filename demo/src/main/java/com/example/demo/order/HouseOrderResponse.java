package com.example.demo.order;

import com.example.demo.common.MaskingUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record HouseOrderResponse(
        Long id,
        Long houseId,
        String houseTitle,
        String buyerUsername,
        String buyerDisplayName,
        String buyerPhoneMasked,
        String sellerUsername,
        String sellerDisplayName,
        String sellerPhoneMasked,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        OrderStatus status,
        OrderProgressStage progressStage,
        String returnReason,
        OffsetDateTime viewingTime,
        String viewingMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static HouseOrderResponse fromEntity(HouseOrder order) {
        return new HouseOrderResponse(
                order.getId(),
                order.getHouse().getId(),
                order.getHouse().getTitle(),
                order.getBuyer().getUsername(),
                order.getBuyer().getDisplayName(),
                MaskingUtils.maskPhoneNumber(order.getBuyer().getPhoneNumber()),
                order.getSeller().getUsername(),
                order.getSeller().getDisplayName(),
                MaskingUtils.maskPhoneNumber(order.getSeller().getPhoneNumber()),
                order.getAmount(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getProgressStage(),
                order.getReturnReason(),
                order.getViewingTime(),
                order.getViewingMessage(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
