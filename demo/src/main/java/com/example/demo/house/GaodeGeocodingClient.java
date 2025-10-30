package com.example.demo.house;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class GaodeGeocodingClient {

    private static final Logger log = LoggerFactory.getLogger(GaodeGeocodingClient.class);

    private static final double CHINA_LAT_MIN = 17.0d;
    private static final double CHINA_LAT_MAX = 54.5d;
    private static final double CHINA_LNG_MIN = 73.0d;
    private static final double CHINA_LNG_MAX = 136.5d;
    private static final String DEFAULT_CLIENT_IP = "114.114.114.114";
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|1?\\d{1,2})){3}$"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GaodeMapSettings settings;
    private final String outboundClientIp;
    private final Map<String, Optional<Coordinate>> cache = new ConcurrentHashMap<>();
    private final Map<String, Optional<PlaceSuggestion>> searchCache = new ConcurrentHashMap<>();
    private final Map<String, List<PlaceSuggestion>> inputTipsCache = new ConcurrentHashMap<>();

    public GaodeGeocodingClient(RestClient.Builder restClientBuilder,
                                ObjectMapper objectMapper,
                                GaodeMapSettings settings) {
        this.objectMapper = objectMapper;
        this.settings = settings;
        this.outboundClientIp = resolveClientIp(settings.clientIp().orElse(null));
        RestClient.Builder builder = restClientBuilder
                .baseUrl("https://restapi.amap.com/v3")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9")
                .defaultHeader(HttpHeaders.USER_AGENT, "FWJ-SecondHandHouse/1.0");
        if (!outboundClientIp.isBlank()) {
            builder = builder
                    .defaultHeader("X-Forwarded-For", outboundClientIp)
                    .defaultHeader("X-Real-IP", outboundClientIp);
        }
        this.restClient = builder.build();
    }

    public boolean isEnabled() {
        return settings != null && settings.hasApiKey();
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
                        .queryParam("key", settings.apiKey())
                        .queryParam("address", address)
                        .queryParam("output", "JSON")
                        .queryParam("extensions", "base");
                if (cityHint != null && !cityHint.isBlank()) {
                    uriBuilder.queryParam("city", normalizeCityQuery(cityHint));
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
            if (!isWithinChina(lat, lng)) {
                log.debug("Gaode geocode returned coordinate outside China bounds lat {} lng {} for {}", lat, lng, address);
                return Optional.empty();
            }
            return Optional.of(new Coordinate(lat, lng));
        } catch (Exception ex) {
            log.warn("Gaode geocode request failed for address {}: {}", address, ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<PlaceSuggestion> searchPlace(String keyword, String cityHint) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        if (keyword == null || keyword.isBlank()) {
            return Optional.empty();
        }
        String normalizedKeyword = keyword.trim();
        String cacheKey = normalizedKeyword + "::" + (cityHint == null ? "" : cityHint.trim());
        return searchCache.computeIfAbsent(cacheKey, key -> fetchPlace(normalizedKeyword, cityHint));
    }

    public List<PlaceSuggestion> suggestPlaces(String keyword, String cityHint) {
        if (!isEnabled()) {
            return List.of();
        }
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String normalizedKeyword = keyword.trim();
        String cacheKey = normalizedKeyword + "::" + (cityHint == null ? "" : cityHint.trim());
        return inputTipsCache.computeIfAbsent(cacheKey, key -> fetchInputTips(normalizedKeyword, cityHint));
    }

    private Optional<PlaceSuggestion> fetchPlace(String keyword, String cityHint) {
        List<String> cityCandidates = buildCityCandidates(cityHint);

        for (String candidate : cityCandidates) {
            Optional<PlaceSuggestion> result = requestPlace(keyword, candidate, true);
            if (result.isPresent()) {
                return result;
            }
        }

        for (String candidate : cityCandidates) {
            Optional<PlaceSuggestion> result = requestPlace(keyword, candidate, false);
            if (result.isPresent()) {
                return result;
            }
        }

        Optional<PlaceSuggestion> nationwideResult = requestPlace(keyword, null, false);
        if (nationwideResult.isPresent()) {
            return nationwideResult;
        }

        return geocodeFallback(keyword, cityCandidates);
    }

    private List<String> buildCityCandidates(String cityHint) {
        List<String> candidates = new ArrayList<>();
        if (cityHint == null) {
            return candidates;
        }
        String trimmed = cityHint.trim();
        if (trimmed.isEmpty()) {
            return candidates;
        }
        String normalized = normalizeCityQuery(trimmed);
        if (!normalized.isBlank()) {
            candidates.add(normalized);
        }
        if (!trimmed.equals(normalized)) {
            candidates.add(trimmed);
        } else if (!candidates.contains(trimmed)) {
            candidates.add(trimmed);
        }
        return candidates;
    }

    private Optional<PlaceSuggestion> requestPlace(String keyword, String city, boolean limitToCity) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri(uriBuilder -> {
                uriBuilder.path("/place/text")
                        .queryParam("key", settings.apiKey())
                        .queryParam("keywords", keyword)
                        .queryParam("page", 1)
                        .queryParam("offset", 20)
                        .queryParam("output", "JSON")
                        .queryParam("language", "zh_cn");
                if (city != null && !city.isBlank()) {
                    uriBuilder.queryParam("city", city);
                    if (limitToCity) {
                        uriBuilder.queryParam("citylimit", true);
                    }
                }
                return uriBuilder.build();
            });

            String body = request.retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(body);
            if (!"1".equals(root.path("status").asText())) {
                log.debug("Gaode place search rejected keyword {} with status {} info {}", keyword,
                        root.path("status").asText(), root.path("info").asText());
                return Optional.empty();
            }
            JsonNode pois = root.path("pois");
            if (!pois.isArray() || pois.isEmpty()) {
                return Optional.empty();
            }
            for (JsonNode poi : pois) {
                String location = poi.path("location").asText();
                if (location == null || location.isBlank() || !location.contains(",")) {
                    continue;
                }
                String[] parts = location.split(",");
                if (parts.length != 2) {
                    continue;
                }
                double lng = Double.parseDouble(parts[0]);
                double lat = Double.parseDouble(parts[1]);
                if (!Double.isFinite(lat) || !Double.isFinite(lng)) {
                    continue;
                }
                if (!isWithinChina(lat, lng)) {
                    log.debug("Gaode place search returned coordinate outside China bounds lat {} lng {} for keyword {}", lat, lng,
                            keyword);
                    continue;
                }
                String name = poi.path("name").asText(null);
                String address = poi.path("address").asText(null);
                if (address == null || address.isBlank()) {
                    String province = poi.path("pname").asText("");
                    String cityName = poi.path("cityname").asText("");
                    String adName = poi.path("adname").asText("");
                    address = String.join("",
                            province == null ? "" : province,
                            cityName == null ? "" : cityName,
                            adName == null ? "" : adName,
                            poi.path("address").asText(""));
                }
                if (address == null || address.isBlank()) {
                    address = keyword;
                }
                if (name == null || name.isBlank()) {
                    name = keyword;
                }
                return Optional.of(new PlaceSuggestion(name, address, lat, lng));
            }
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Gaode place search failed for keyword {}: {}", keyword, ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<PlaceSuggestion> geocodeFallback(String keyword, List<String> cityCandidates) {
        for (String candidate : cityCandidates) {
            Optional<Coordinate> coordinate = geocode(keyword, candidate);
            if (coordinate.isPresent()) {
                Coordinate value = coordinate.get();
                return Optional.of(new PlaceSuggestion(keyword, keyword, value.latitude(), value.longitude()));
            }
        }
        Optional<Coordinate> fallback = geocode(keyword, null);
        if (fallback.isPresent()) {
            Coordinate value = fallback.get();
            return Optional.of(new PlaceSuggestion(keyword, keyword, value.latitude(), value.longitude()));
        }
        return Optional.empty();
    }

    private List<PlaceSuggestion> fetchInputTips(String keyword, String cityHint) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri(uriBuilder -> {
                uriBuilder.path("/assistant/inputtips")
                        .queryParam("key", settings.apiKey())
                        .queryParam("keywords", keyword)
                        .queryParam("output", "JSON")
                        .queryParam("datatype", "all");
                if (cityHint != null && !cityHint.isBlank()) {
                    uriBuilder.queryParam("city", normalizeCityQuery(cityHint));
                }
                return uriBuilder.build();
            });

            String body = request.retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(body);
            if (!"1".equals(root.path("status").asText())) {
                log.debug("Gaode input tips rejected keyword {} with status {} info {}", keyword,
                        root.path("status").asText(), root.path("info").asText());
                return List.of();
            }
            JsonNode tips = root.path("tips");
            if (!tips.isArray() || tips.isEmpty()) {
                return List.of();
            }
            List<PlaceSuggestion> results = new ArrayList<>();
            for (JsonNode tip : tips) {
                String location = tip.path("location").asText();
                if (location == null || location.isBlank() || !location.contains(",")) {
                    continue;
                }
                String[] parts = location.split(",");
                if (parts.length != 2) {
                    continue;
                }
                double lng = Double.parseDouble(parts[0]);
                double lat = Double.parseDouble(parts[1]);
                if (!Double.isFinite(lat) || !Double.isFinite(lng)) {
                    continue;
                }
                if (!isWithinChina(lat, lng)) {
                    log.debug("Gaode input tips returned coordinate outside China bounds lat {} lng {} for keyword {}", lat,
                            lng, keyword);
                    continue;
                }
                String name = tip.path("name").asText(null);
                String address = tip.path("address").asText(null);
                if (address == null || address.isBlank()) {
                    String district = tip.path("district").asText("");
                    address = district == null ? "" : district;
                }
                if (address == null || address.isBlank()) {
                    address = keyword;
                }
                if (name == null || name.isBlank()) {
                    name = keyword;
                }
                results.add(new PlaceSuggestion(name, address, lat, lng));
            }
            return results.isEmpty() ? List.of() : List.copyOf(results);
        } catch (Exception ex) {
            log.warn("Gaode input tips failed for keyword {}: {}", keyword, ex.getMessage());
            return List.of();
        }
    }

    private String normalizeCityQuery(String cityHint) {
        String sanitized = cityHint.trim();
        sanitized = sanitized.replaceAll("(自治州|地区|盟)", "");
        if (sanitized.endsWith("市") || sanitized.endsWith("县") || sanitized.endsWith("区") || sanitized.endsWith("旗")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        return sanitized;
    }

    private String resolveClientIp(String configuredIp) {
        String candidate = configuredIp == null ? "" : configuredIp.trim();
        if (candidate.isEmpty()) {
            return DEFAULT_CLIENT_IP;
        }
        if (IPV4_PATTERN.matcher(candidate).matches()) {
            return candidate;
        }
        log.warn("Configured Gaode client IP {} is invalid, falling back to default {}", candidate, DEFAULT_CLIENT_IP);
        return DEFAULT_CLIENT_IP;
    }

    private boolean isWithinChina(double latitude, double longitude) {
        return latitude >= CHINA_LAT_MIN
                && latitude <= CHINA_LAT_MAX
                && longitude >= CHINA_LNG_MIN
                && longitude <= CHINA_LNG_MAX;
    }

    public record Coordinate(double latitude, double longitude) {
    }

    public record PlaceSuggestion(String name, String address, double latitude, double longitude) {
    }
}
