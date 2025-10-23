package com.example.demo.analytics;

import java.math.BigDecimal;
import java.util.List;

public record HousePricePredictionResponse(
        BigDecimal predictedPrice,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        double confidence,
        List<FeatureContribution> contributions
) {
}
