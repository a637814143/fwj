package com.example.demo.house;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
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
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "分期月付金额必须大于0") BigDecimal installmentMonthlyPayment,
        @NotNull @Min(value = 1, message = "分期月数必须大于0") Integer installmentMonths,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "面积必须大于0") BigDecimal area,
        String description,
        @NotBlank(message = "卖家账号不能为空") @Size(max = 50, message = "卖家账号长度不能超过50个字符") String sellerUsername,
        @NotBlank(message = "卖家姓名不能为空") String sellerName,
        @NotBlank(message = "联系方式不能为空") String contactNumber,
        @NotNull @PastOrPresent(message = "挂牌日期不能是未来日期") LocalDate listingDate,
        @PositiveOrZero(message = "楼层不能为负数") Integer floor,
        List<String> keywords,
        List<String> imageUrls,
        @NotBlank(message = "请上传房屋产权证明链接") String propertyCertificateUrl
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

    private static String sanitizeCertificateUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.trim();
    }

    public SecondHandHouse toEntity() {
        SecondHandHouse house = new SecondHandHouse();
        house.setTitle(title);
        house.setAddress(address);
        house.setPrice(price);
        house.setInstallmentMonthlyPayment(installmentMonthlyPayment);
        house.setInstallmentMonths(installmentMonths);
        house.setArea(area);
        house.setDescription(description);
        house.setSellerUsername(sellerUsername);
        house.setSellerName(sellerName);
        house.setContactNumber(contactNumber);
        house.setListingDate(listingDate);
        house.setFloor(floor);
        house.setKeywords(sanitizeKeywords(keywords));
        house.setImageUrls(sanitizeImageUrls(imageUrls));
        house.setPropertyCertificateUrl(sanitizeCertificateUrl(propertyCertificateUrl));
        return house;
    }
}
