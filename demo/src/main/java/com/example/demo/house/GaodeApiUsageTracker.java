package com.example.demo.house;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GaodeApiUsageTracker {

    private final EnumMap<ApiType, UsageState> states = new EnumMap<>(ApiType.class);

    public GaodeApiUsageTracker() {
        for (ApiType type : ApiType.values()) {
            states.put(type, new UsageState());
        }
    }

    public void record(ApiType type,
                       boolean success,
                       String status,
                       String info,
                       String infoCode,
                       Integer count) {
        Objects.requireNonNull(type, "type must not be null");
        UsageState state = states.get(type);
        if (state == null) {
            return;
        }
        state.record(success, status, info, infoCode, count);
    }

    public UsageSnapshot snapshot(boolean enabled) {
        Map<String, ApiUsage> usage = new LinkedHashMap<>();
        for (Map.Entry<ApiType, UsageState> entry : states.entrySet()) {
            usage.put(entry.getKey().name(), entry.getValue().toUsage());
        }
        return new UsageSnapshot(enabled, usage);
    }

    public enum ApiType {
        GEOCODE,
        PLACE_TEXT,
        INPUT_TIPS
    }

    public record UsageSnapshot(boolean enabled, Map<String, ApiUsage> endpoints) {
        public UsageSnapshot {
            Objects.requireNonNull(endpoints, "endpoints must not be null");
        }
    }

    public record ApiUsage(int totalRequests,
                           int totalFailures,
                           Integer lastCount,
                           String lastStatus,
                           String lastInfo,
                           String lastInfoCode,
                           OffsetDateTime lastUpdated) {
    }

    private static final class UsageState {
        private final AtomicInteger totalRequests = new AtomicInteger();
        private final AtomicInteger totalFailures = new AtomicInteger();
        private volatile ApiUsage lastUsage = new ApiUsage(0, 0, null, null, null, null, null);

        private void record(boolean success,
                            String status,
                            String info,
                            String infoCode,
                            Integer count) {
            int requests = totalRequests.incrementAndGet();
            int failures = success ? totalFailures.get() : totalFailures.incrementAndGet();
            String sanitizedStatus = sanitize(status);
            String sanitizedInfo = sanitize(info);
            String sanitizedInfoCode = sanitize(infoCode);
            Integer sanitizedCount = count == null || count < 0 ? null : count;
            lastUsage = new ApiUsage(
                    requests,
                    failures,
                    sanitizedCount,
                    sanitizedStatus,
                    sanitizedInfo,
                    sanitizedInfoCode,
                    OffsetDateTime.now()
            );
        }

        private ApiUsage toUsage() {
            ApiUsage usage = lastUsage;
            if (usage == null) {
                return new ApiUsage(totalRequests.get(), totalFailures.get(), null, null, null, null, null);
            }
            return usage;
        }

        private String sanitize(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }
}
