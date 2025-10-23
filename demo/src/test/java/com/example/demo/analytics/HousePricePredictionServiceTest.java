package com.example.demo.analytics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HousePricePredictionServiceTest {

    private final HousePricePredictionService service = new HousePricePredictionService();

    @Test
    void predictsPriceWithReasonableBounds() {
        HousePricePredictionRequest request = new HousePricePredictionRequest(
                120.0,
                3.5,
                8,
                1.2,
                3.0,
                85
        );

        HousePricePredictionResponse response = service.predict(request);

        assertThat(response.predictedPrice()).isNotNull();
        assertThat(response.predictedPrice().doubleValue()).isGreaterThan(5.0);
        assertThat(response.lowerBound()).isLessThanOrEqualTo(response.predictedPrice());
        assertThat(response.upperBound()).isGreaterThanOrEqualTo(response.predictedPrice());
        assertThat(response.confidence()).isBetween(0.5, 0.9);
        assertThat(response.contributions()).hasSize(6);
    }

    @Test
    void contributionsReflectNegativeImpacts() {
        HousePricePredictionRequest request = new HousePricePredictionRequest(
                80.0,
                2.5,
                40,
                6.0,
                25.0,
                45
        );

        HousePricePredictionResponse response = service.predict(request);

        assertThat(response.predictedPrice().doubleValue()).isGreaterThan(5.0);
        assertThat(response.contributions()).anyMatch(item -> item.featureKey().equals("crime")
                && item.impact().doubleValue() < 0);
        assertThat(response.contributions()).anyMatch(item -> item.featureKey().equals("school")
                && item.impact().doubleValue() > 0);
    }
}
