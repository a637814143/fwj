package com.example.demo.reputation;

import java.util.List;

public record RecommendationResponse(
        List<RecommendedUser> sellers,
        List<RecommendedUser> buyers
) {
}
