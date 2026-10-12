package org.nutricoach.nutrition.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record RecipeCalculationResult(
        BigDecimal cookedBatchMassGrams,
        BigDecimal portionMassGrams,
        NutritionCalculationResult batch,
        NutritionCalculationResult per100Grams,
        NutritionCalculationResult portion
) {
    public RecipeCalculationResult {
        Objects.requireNonNull(cookedBatchMassGrams, "cookedBatchMassGrams is required");
        Objects.requireNonNull(portionMassGrams, "portionMassGrams is required");
        Objects.requireNonNull(batch, "batch is required");
        Objects.requireNonNull(per100Grams, "per100Grams is required");
        Objects.requireNonNull(portion, "portion is required");
        if (cookedBatchMassGrams.signum() <= 0 || portionMassGrams.signum() <= 0) {
            throw new IllegalArgumentException("batch and portion masses must be positive");
        }
    }
}
