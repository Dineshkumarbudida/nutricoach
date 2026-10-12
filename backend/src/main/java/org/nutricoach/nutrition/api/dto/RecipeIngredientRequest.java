package org.nutricoach.nutrition.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RecipeIngredientRequest(
        @NotNull @Valid FoodSnapshotRequest food,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal massGrams,
        String note
) {
}
