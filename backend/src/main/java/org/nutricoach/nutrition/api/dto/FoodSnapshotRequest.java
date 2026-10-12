package org.nutricoach.nutrition.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.nutricoach.nutrition.domain.NutrientCode;

import java.math.BigDecimal;
import java.util.Map;

public record FoodSnapshotRequest(
        @NotBlank String foodId,
        @NotBlank String version,
        @NotBlank String displayName,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal referenceMassGrams,
        @NotEmpty Map<NutrientCode, @NotNull @Valid NutrientObservationRequest> nutrients,
        @NotNull @Valid SourceReferenceRequest source
) {
}
