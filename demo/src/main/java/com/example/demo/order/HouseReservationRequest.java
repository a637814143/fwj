package com.example.demo.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HouseReservationRequest(
        @NotNull(message = "请选择要预定的房源") Long houseId,
        @NotBlank(message = "买家账号不能为空") String buyerUsername
) {
}
