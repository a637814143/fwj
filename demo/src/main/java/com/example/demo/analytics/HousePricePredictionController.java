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

    // 提交房价预测请求并返回模型结果
    @PostMapping("/price-prediction")
    public HousePricePredictionResponse predict(@Valid @RequestBody HousePricePredictionRequest request) {
        return predictionService.predict(request);
    }
}
