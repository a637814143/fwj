package com.example.demo.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HouseOrderRequest(
        @NotNull(message = "请选择要购买的房源") Long houseId,
        @NotBlank(message = "买家账号不能为空") String buyerUsername
) {
}
