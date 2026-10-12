package org.nutricoach.nutrition.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record RecipeCalculationRequest(
        @NotEmpty List<@NotNull @Valid RecipeIngredientRequest> ingredients,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal cookedBatchMassGrams,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal portionMassGrams
) {
}
