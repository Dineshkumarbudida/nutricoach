package org.nutricoach.nutrition.domain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable, versioned nutrient snapshot for a food on a mass basis.
 */
public record FoodComposition(
        String foodId,
        String version,
        String displayName,
        BigDecimal referenceMassGrams,
        Map<NutrientCode, NutrientObservation> nutrients,
        SourceReference source
) {
    public FoodComposition {
        foodId = requireText(foodId, "foodId");
        version = requireText(version, "version");
        displayName = requireText(displayName, "displayName");
        requirePositive(referenceMassGrams, "referenceMassGrams");
        Objects.requireNonNull(nutrients, "nutrients is required");
        Objects.requireNonNull(source, "source is required");

        EnumMap<NutrientCode, NutrientObservation> copied = new EnumMap<>(NutrientCode.class);
        copied.putAll(nutrients);
        if (copied.containsValue(null)) {
            throw new IllegalArgumentException("nutrients must not contain null values");
        }
        nutrients = Collections.unmodifiableMap(copied);
    }

    public NutrientObservation observationFor(NutrientCode nutrient) {
        Objects.requireNonNull(nutrient, "nutrient is required");
        return nutrients.getOrDefault(nutrient, NutrientObservation.missing());
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }

    private static void requirePositive(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }
}
