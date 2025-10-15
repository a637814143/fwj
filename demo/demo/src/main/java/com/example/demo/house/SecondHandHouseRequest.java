package com.example.demo.house;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record SecondHandHouseRequest(
        @NotBlank(message = "标题不能为空") String title,
        @NotBlank(message = "地址不能为空") String address,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "价格必须大于0") BigDecimal price,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "面积必须大于0") BigDecimal area,
        String description,
        @NotBlank(message = "卖家账号不能为空") @Size(max = 50, message = "卖家账号长度不能超过50个字符") String sellerUsername,
        @NotBlank(message = "卖家姓名不能为空") String sellerName,
        @NotBlank(message = "联系方式不能为空") String contactNumber,
        @NotNull @PastOrPresent(message = "挂牌日期不能是未来日期") LocalDate listingDate,
        @Size(max = 10, message = "最多上传10张房源图片") List<@Size(max = 512, message = "图片地址长度不能超过512个字符") String> imageUrls
) {
    public SecondHandHouse toEntity() {
        SecondHandHouse house = new SecondHandHouse();
        house.setTitle(title);
        house.setAddress(address);
        house.setPrice(price);
        house.setArea(area);
        house.setDescription(description);
        house.setSellerUsername(sellerUsername);
        house.setSellerName(sellerName);
        house.setContactNumber(contactNumber);
        house.setListingDate(listingDate);
        house.setImageUrls(sanitisedImages());
        return house;
    }

    private List<String> sanitisedImages() {
        if (imageUrls == null) {
            return List.of();
        }
        return imageUrls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .collect(Collectors.toList());
    }
}
