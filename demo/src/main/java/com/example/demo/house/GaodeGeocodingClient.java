package com.example.demo.house;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GaodeGeocodingClient {

    private static final Logger log = LoggerFactory.getLogger(GaodeGeocodingClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final Map<String, Optional<Coordinate>> cache = new ConcurrentHashMap<>();

    public GaodeGeocodingClient(RestClient.Builder restClientBuilder,
                                ObjectMapper objectMapper,
                                @Value("${gaode.api.key:46dff0d2a8f9204d4642f8dd91e10daf}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl("https://restapi.amap.com/v3").build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Optional<Coordinate> geocode(String address, String cityHint) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        String normalizedAddress = address.trim();
        String cacheKey = normalizedAddress + "::" + (cityHint == null ? "" : cityHint.trim());
        return cache.computeIfAbsent(cacheKey, key -> fetchCoordinate(normalizedAddress, cityHint));
    }

    private Optional<Coordinate> fetchCoordinate(String address, String cityHint) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri(uriBuilder -> {
                uriBuilder.path("/geocode/geo")
                        .queryParam("key", apiKey)
                        .queryParam("address", address)
                        .queryParam("output", "JSON")
                        .queryParam("extensions", "base");
                if (cityHint != null && !cityHint.isBlank()) {
                    uriBuilder.queryParam("city", cityHint.replace("市", ""));
                }
                return uriBuilder.build();
            });

            String body = request.retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(body);
            if (!"1".equals(root.path("status").asText())) {
                log.debug("Gaode geocode rejected address {} with status {} info {}", address,
                        root.path("status").asText(), root.path("info").asText());
                return Optional.empty();
            }
            JsonNode geocodes = root.path("geocodes");
            if (!geocodes.isArray() || geocodes.isEmpty()) {
                return Optional.empty();
            }
            String location = geocodes.get(0).path("location").asText();
            if (location == null || location.isBlank() || !location.contains(",")) {
                return Optional.empty();
            }
            String[] parts = location.split(",");
            if (parts.length != 2) {
                return Optional.empty();
            }
            double lng = Double.parseDouble(parts[0]);
            double lat = Double.parseDouble(parts[1]);
            if (!Double.isFinite(lat) || !Double.isFinite(lng)) {
                return Optional.empty();
            }
            return Optional.of(new Coordinate(lat, lng));
        } catch (Exception ex) {
            log.warn("Gaode geocode request failed for address {}: {}", address, ex.getMessage());
            return Optional.empty();
        }
    }

    public record Coordinate(double latitude, double longitude) {
    }
}
