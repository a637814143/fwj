package com.example.demo.analytics;

import java.math.BigDecimal;

/**
 * Describes the contribution (positive or negative) that a single input
 * feature provided to the final predicted price. The value is expressed
 * in "万" (ten-thousand yuan) to match the existing wallet and pricing
 * conventions used by the platform.
 */
public record FeatureContribution(String featureKey, BigDecimal impact) {
}
