package com.example.demo.analytics;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload describing the core features used by the heuristic
 * house price prediction model. The values roughly correspond to
 * factors that influenced the Boston housing dataset while being
 * adapted to the application's domain terminology.
 */
public record HousePricePredictionRequest(
        @NotNull(message = "预测需要提供房屋面积")
        @DecimalMin(value = "10", message = "房屋面积必须大于 10 平方米")
        Double area,

        @NotNull(message = "请提供平均卧室数量")
        @DecimalMin(value = "0.5", message = "平均卧室数量必须大于 0")
        Double averageRooms,

        @NotNull(message = "请提供房龄信息")
        @Min(value = 0, message = "房龄不能为负数")
        @Max(value = 120, message = "房龄不能超过 120 年")
        Integer propertyAge,

        @NotNull(message = "请提供距离地铁的距离")
        @DecimalMin(value = "0", message = "地铁距离不能为负数")
        Double distanceToSubway,

        @NotNull(message = "请提供社区治安指数")
        @DecimalMin(value = "0", message = "治安指数不能为负数")
        Double crimeRate,

        @NotNull(message = "请提供学校评分")
        @Min(value = 0, message = "学校评分不能为负数")
        @Max(value = 100, message = "学校评分不能超过 100 分")
        Integer schoolScore
) {
}
