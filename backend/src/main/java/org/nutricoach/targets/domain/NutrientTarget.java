package org.nutricoach.targets.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record NutrientTarget(
        String code,
        String displayName,
        BigDecimal amount,
        String unit,
        String referenceType,
        BigDecimal upperOrReductionThreshold,
        String sourceId
) {
    public NutrientTarget {
        code = requireText(code, "code");
        displayName = requireText(displayName, "displayName");
        Objects.requireNonNull(amount, "amount is required");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        unit = requireText(unit, "unit");
        referenceType = requireText(referenceType, "referenceType");
        sourceId = requireText(sourceId, "sourceId");
        if (upperOrReductionThreshold != null && upperOrReductionThreshold.signum() < 0) {
            throw new IllegalArgumentException("upperOrReductionThreshold must not be negative");
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }
}
