package com.example.demo.house;

import com.example.demo.common.MaskingUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public record SecondHandHouseView(
        Long id,
        String title,
        String address,
        BigDecimal price,
        BigDecimal area,
        String description,
        String sellerUsername,
        String sellerName,
        String contactNumber,
        LocalDate listingDate,
        List<String> imageUrls,
        List<String> keywords,
        boolean sensitiveMasked
) {

    public static SecondHandHouseView fromEntity(SecondHandHouse house, boolean maskSensitive) {
        if (house == null) {
            return null;
        }
        String sellerName = maskSensitive ? MaskingUtils.maskDisplayName(house.getSellerName()) : house.getSellerName();
        String contactNumber = maskSensitive ? MaskingUtils.maskPhoneNumber(house.getContactNumber()) : house.getContactNumber();
        List<String> images = house.getImageUrls() == null ? List.of() : List.copyOf(house.getImageUrls());
        List<String> keywords = house.getKeywords() == null ? List.of() : List.copyOf(house.getKeywords());
        return new SecondHandHouseView(
                house.getId(),
                house.getTitle(),
                house.getAddress(),
                house.getPrice(),
                house.getArea(),
                house.getDescription(),
                house.getSellerUsername(),
                sellerName,
                contactNumber,
                house.getListingDate(),
                Collections.unmodifiableList(images),
                Collections.unmodifiableList(keywords),
                maskSensitive
        );
    }
}
