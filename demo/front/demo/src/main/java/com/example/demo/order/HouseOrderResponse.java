package com.example.demo.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record HouseOrderResponse(
        Long id,
        Long houseId,
        String houseTitle,
        String buyerUsername,
        String buyerDisplayName,
        String sellerUsername,
        String sellerDisplayName,
        BigDecimal amount,
        OrderStatus status,
        String returnReason,
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
                order.getSeller().getUsername(),
                order.getSeller().getDisplayName(),
                order.getAmount(),
                order.getStatus(),
                order.getReturnReason(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
