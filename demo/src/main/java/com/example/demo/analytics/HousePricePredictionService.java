package com.example.demo.analytics;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class HousePricePredictionService {

    private static final BigDecimal MIN_PRICE = BigDecimal.valueOf(5);

    public HousePricePredictionResponse predict(HousePricePredictionRequest request) {
        double areaImpact = request.area() * 0.08;
        double roomImpact = request.averageRooms() * 4.6;
        double ageImpact = -request.propertyAge() * 0.12;
        double subwayImpact = -request.distanceToSubway() * 1.5;
        double crimeImpact = -normalizeCrime(request.crimeRate()) * 0.3;
        double schoolImpact = normalizeSchool(request.schoolScore()) * 0.35;

        double baseline = 32.5;
        double rawPrice = baseline + areaImpact + roomImpact + ageImpact + subwayImpact + crimeImpact + schoolImpact;
        double adjustedPrice = Math.max(rawPrice, MIN_PRICE.doubleValue());

        BigDecimal predicted = scale(adjustedPrice);
        double confidence = computeConfidence(request);
        BigDecimal lower = scale(adjustedPrice * (1 - (1 - confidence) * 0.55));
        BigDecimal upper = scale(adjustedPrice * (1 + (1 - confidence) * 0.45));

        List<FeatureContribution> contributions = buildContributions(areaImpact, roomImpact, ageImpact,
                subwayImpact, crimeImpact, schoolImpact);

        return new HousePricePredictionResponse(predicted, lower, upper, roundConfidence(confidence), contributions);
    }

    private List<FeatureContribution> buildContributions(double areaImpact,
                                                         double roomImpact,
                                                         double ageImpact,
                                                         double subwayImpact,
                                                         double crimeImpact,
                                                         double schoolImpact) {
        List<FeatureContribution> items = new ArrayList<>();
        items.add(new FeatureContribution("area", scale(areaImpact)));
        items.add(new FeatureContribution("rooms", scale(roomImpact)));
        items.add(new FeatureContribution("age", scale(ageImpact)));
        items.add(new FeatureContribution("subway", scale(subwayImpact)));
        items.add(new FeatureContribution("crime", scale(crimeImpact)));
        items.add(new FeatureContribution("school", scale(schoolImpact)));
        return items;
    }

    private double normalizeCrime(double crimeRate) {
        double clamped = Math.max(0, Math.min(crimeRate, 30));
        return clamped / 10.0;
    }

    private double normalizeSchool(int score) {
        return Math.max(0, Math.min(score, 100)) / 10.0;
    }

    private double computeConfidence(HousePricePredictionRequest request) {
        double base = 0.62;
        base += Math.min(request.averageRooms() / 10.0, 0.2);
        base += Math.min(request.schoolScore() / 100.0, 0.2);
        base -= Math.min(request.propertyAge() / 200.0, 0.12);
        base -= Math.min(request.distanceToSubway() / 30.0, 0.1);
        base -= Math.min(request.crimeRate() / 100.0, 0.1);
        return Math.max(0.5, Math.min(base, 0.9));
    }

    private double roundConfidence(double confidence) {
        return BigDecimal.valueOf(confidence)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
