package org.nutricoach.nutrition.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.nutricoach.nutrition.domain.ObservationStatus;

import java.math.BigDecimal;

public record NutrientObservationRequest(
        @NotNull ObservationStatus status,
        @DecimalMin("0") BigDecimal amount,
        @DecimalMin(value = "0", inclusive = false) BigDecimal reportingLimit
) {
}
