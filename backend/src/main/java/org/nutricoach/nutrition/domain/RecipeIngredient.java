package org.nutricoach.nutrition.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record RecipeIngredient(
        FoodComposition food,
        BigDecimal massGrams,
        String note
) {
    public RecipeIngredient {
        Objects.requireNonNull(food, "food is required");
        Objects.requireNonNull(massGrams, "massGrams is required");
        if (massGrams.signum() <= 0) {
            throw new IllegalArgumentException("massGrams must be positive");
        }
        note = note == null ? "" : note.trim();
    }
}
