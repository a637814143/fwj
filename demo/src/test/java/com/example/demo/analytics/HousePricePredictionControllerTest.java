package com.example.demo.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HousePricePredictionController.class)
@Import(HousePricePredictionService.class)
class HousePricePredictionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/analytics/price-prediction returns model output")
    void predictReturnsResponse() throws Exception {
        HousePricePredictionRequest request = new HousePricePredictionRequest(
                118.0,
                3.4,
                12,
                1.8,
                4.5,
                78
        );

        mockMvc.perform(post("/api/analytics/price-prediction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictedPrice").exists())
                .andExpect(jsonPath("$.lowerBound").exists())
                .andExpect(jsonPath("$.upperBound").exists())
                .andExpect(jsonPath("$.confidence").isNumber())
                .andExpect(jsonPath("$.contributions").isArray());
    }

    @Test
    @DisplayName("POST /api/analytics/price-prediction validates the request payload")
    void predictValidatesInput() throws Exception {
        HousePricePredictionRequest request = new HousePricePredictionRequest(
                null,
                0.0,
                -1,
                -2.0,
                -3.0,
                150
        );

        mockMvc.perform(post("/api/analytics/price-prediction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }
}
