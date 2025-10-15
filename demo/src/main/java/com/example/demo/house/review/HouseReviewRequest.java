package com.example.demo.house.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HouseReviewRequest(
        @NotBlank(message = "买家账号不能为空") @Size(max = 50, message = "买家账号长度不能超过50个字符") String buyerUsername,
        @NotBlank(message = "买家姓名不能为空") String buyerDisplayName,
        @Min(value = 1, message = "评分不能低于1分") @Max(value = 5, message = "评分不能高于5分") int rating,
        @Size(max = 1000, message = "评价内容不能超过1000个字符") String comment
) {
}
