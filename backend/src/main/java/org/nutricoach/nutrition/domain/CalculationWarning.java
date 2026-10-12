package org.nutricoach.nutrition.domain;

import java.util.Objects;

public record CalculationWarning(
        WarningCode code,
        NutrientCode nutrient,
        String message
) {
    public CalculationWarning {
        Objects.requireNonNull(code, "code is required");
        Objects.requireNonNull(message, "message is required");
        message = message.trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
