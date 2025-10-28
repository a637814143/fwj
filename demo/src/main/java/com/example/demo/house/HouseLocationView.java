package com.example.demo.house;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record HouseLocationView(
        Long id,
        String title,
        String address,
        BigDecimal price,
        ListingStatus status,
        Double latitude,
        Double longitude,
        OffsetDateTime updatedAt
) {

    public static HouseLocationView fromEntity(SecondHandHouse house) {
        if (house == null) {
            return null;
        }
        return new HouseLocationView(
                house.getId(),
                house.getTitle(),
                house.getAddress(),
                house.getPrice(),
                house.getStatus(),
                house.getLatitude(),
                house.getLongitude(),
                house.getUpdatedAt()
        );
    }
}
