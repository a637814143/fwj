package com.example.demo.analytics;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class HousePricePredictionController {

    private final HousePricePredictionService predictionService;

    public HousePricePredictionController(HousePricePredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping("/price-prediction")
    public HousePricePredictionResponse predict(@Valid @RequestBody HousePricePredictionRequest request) {
        return predictionService.predict(request);
    }
}
