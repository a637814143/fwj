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
        BigDecimal adminHoldAmount,
        BigDecimal platformFee,
        BigDecimal releasedAmount,
        PayoutRecipient fundsReleasedTo,
        boolean adminReviewed,
        String adminReviewedBy,
        OffsetDateTime adminReviewedAt,
        String returnReason,
        OffsetDateTime viewingTime,
        String viewingMessage,
        boolean buyerViewingConfirmed,
        boolean sellerViewingConfirmed,
        boolean sellerRepayRequired,
        BigDecimal sellerRepayAmount,
        String sellerRepayReference,
        String sellerRepayDescription,
        OffsetDateTime sellerRepaySettledAt,
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
                order.getAdminHoldAmount(),
                order.getPlatformFee(),
                order.getReleasedAmount(),
                order.getFundsReleasedTo(),
                order.isAdminReviewed(),
                order.getAdminReviewedBy(),
                order.getAdminReviewedAt(),
                order.getReturnReason(),
                order.getViewingTime(),
                order.getViewingMessage(),
                order.isBuyerViewingConfirmed(),
                order.isSellerViewingConfirmed(),
                order.isSellerRepayRequired(),
                order.getSellerRepayAmount(),
                order.getSellerRepayReference(),
                order.getSellerRepayDescription(),
                order.getSellerRepaySettledAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
