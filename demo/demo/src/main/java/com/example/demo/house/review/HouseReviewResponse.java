package com.example.demo.house.review;

import java.time.OffsetDateTime;

public record HouseReviewResponse(
        Long id,
        Long houseId,
        String buyerUsername,
        String buyerDisplayName,
        int rating,
        String comment,
        OffsetDateTime createdAt
) {
    public static HouseReviewResponse fromEntity(HouseReview review) {
        return new HouseReviewResponse(
                review.getId(),
                review.getHouse().getId(),
                review.getBuyerUsername(),
                review.getBuyerDisplayName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
