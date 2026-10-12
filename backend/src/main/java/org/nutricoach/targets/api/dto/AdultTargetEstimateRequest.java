package org.nutricoach.targets.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.nutricoach.targets.domain.ActivityLevel;
import org.nutricoach.targets.domain.EnergyEquationSet;
import org.nutricoach.targets.domain.Goal;

import java.math.BigDecimal;

public record AdultTargetEstimateRequest(
        @Min(19) @Max(120) int ageYears,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("300")
        BigDecimal heightCentimeters,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("500")
        BigDecimal weightKilograms,
        @NotNull EnergyEquationSet equationSet,
        @NotNull ActivityLevel activityLevel,
        @NotNull Goal goal,
        boolean resistanceTraining,
        @Min(0) @Max(7) int trainingDaysPerWeek,
        @DecimalMin("-15") @DecimalMax("10") BigDecimal goalAdjustmentPercentOverride
) {
}
