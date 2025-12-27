package com.example.demo.house;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.example.demo.house.ListingStatus;

public record SecondHandHouseRequest(
        @NotBlank(message = "标题不能为空") String title,
        @NotBlank(message = "地址不能为空") String address,
        @DecimalMin(value = "-90.0", message = "纬度范围应在-90至90之间")
        @DecimalMax(value = "90.0", message = "纬度范围应在-90至90之间")
        Double latitude,
        @DecimalMin(value = "-180.0", message = "经度范围应在-180至180之间")
        @DecimalMax(value = "180.0", message = "经度范围应在-180至180之间")
        Double longitude,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "价格必须大于0") BigDecimal price,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "首付金额必须大于0") BigDecimal downPayment,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "面积必须大于0") BigDecimal area,
        String description,
        @NotBlank(message = "卖家账号不能为空") @Size(max = 50, message = "卖家账号长度不能超过50个字符") String sellerUsername,
        @NotBlank(message = "卖家姓名不能为空") String sellerName,
        @NotBlank(message = "联系方式不能为空") String contactNumber,
        @NotNull @Future(message = "挂牌日期必须选择未来日期") LocalDate listingDate,
        @PositiveOrZero(message = "楼层不能为负数") Integer floor,
        List<String> keywords,
        List<String> imageUrls,
        Boolean saveAsDraft
) {
    private static List<String> sanitizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        return imageUrls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .collect(Collectors.toList());
    }

    private static List<String> sanitizeKeywords(List<String> keywords) {
        if (keywords == null) {
            return List.of();
        }
        return keywords.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .collect(Collectors.toList());
    }

    public SecondHandHouse toEntity() {
        SecondHandHouse house = new SecondHandHouse();
        house.setTitle(title);
        house.setAddress(address);
        house.setLatitude(sanitizeLatitude(latitude));
        house.setLongitude(sanitizeLongitude(longitude));
        house.setPrice(price);
        house.setDownPayment(downPayment);
        house.setArea(area);
        house.setDescription(description);
        house.setSellerUsername(sellerUsername);
        house.setSellerName(sellerName);
        house.setContactNumber(contactNumber);
        house.setListingDate(listingDate);
        house.setFloor(floor);
        house.setKeywords(sanitizeKeywords(keywords));
        house.setImageUrls(sanitizeImageUrls(imageUrls));
        house.setStatus(Boolean.TRUE.equals(saveAsDraft) ? ListingStatus.DRAFT : ListingStatus.PENDING_REVIEW);
        return house;
    }

    private static Double sanitizeLatitude(Double value) {
        return sanitizeCoordinate(value, -90d, 90d);
    }

    private static Double sanitizeLongitude(Double value) {
        return sanitizeCoordinate(value, -180d, 180d);
    }

    private static Double sanitizeCoordinate(Double value, double min, double max) {
        if (value == null) {
            return null;
        }
        double number = value;
        if (!Double.isFinite(number)) {
            return null;
        }
        if (number < min || number > max) {
            return null;
        }
        double scaled = Math.round(number * 1_000_000d) / 1_000_000d;
        if (scaled < min || scaled > max) {
            return null;
        }
        return scaled;
    }

}
