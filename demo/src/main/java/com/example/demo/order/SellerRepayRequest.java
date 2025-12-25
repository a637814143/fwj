package com.example.demo.order;

import jakarta.validation.constraints.NotBlank;

public record SellerRepayRequest(
        @NotBlank(message = "卖家账号不能为空")
        String sellerUsername
) {
}
