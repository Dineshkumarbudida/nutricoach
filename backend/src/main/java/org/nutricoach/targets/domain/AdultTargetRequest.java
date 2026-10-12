package org.nutricoach.targets.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Inputs for the currently supported adult, non-pregnant target method.
 */
public record AdultTargetRequest(
        int ageYears,
        BigDecimal heightCentimeters,
        BigDecimal weightKilograms,
        EnergyEquationSet equationSet,
        ActivityLevel activityLevel,
        Goal goal,
        boolean resistanceTraining,
        int trainingDaysPerWeek,
        BigDecimal goalAdjustmentPercentOverride
) {
    public AdultTargetRequest {
        if (ageYears < 19 || ageYears > 120) {
            throw new IllegalArgumentException("ageYears must be between 19 and 120 for this method");
        }
        requirePositive(heightCentimeters, "heightCentimeters");
        requirePositive(weightKilograms, "weightKilograms");
        if (heightCentimeters.compareTo(new BigDecimal("300")) > 0) {
            throw new IllegalArgumentException("heightCentimeters must not exceed 300");
        }
        if (weightKilograms.compareTo(new BigDecimal("500")) > 0) {
            throw new IllegalArgumentException("weightKilograms must not exceed 500");
        }
        Objects.requireNonNull(equationSet, "equationSet is required");
        Objects.requireNonNull(activityLevel, "activityLevel is required");
        Objects.requireNonNull(goal, "goal is required");
        if (trainingDaysPerWeek < 0 || trainingDaysPerWeek > 7) {
            throw new IllegalArgumentException("trainingDaysPerWeek must be between 0 and 7");
        }
        if (goalAdjustmentPercentOverride != null
                && (goalAdjustmentPercentOverride.compareTo(new BigDecimal("-15")) < 0
                || goalAdjustmentPercentOverride.compareTo(new BigDecimal("10")) > 0)) {
            throw new IllegalArgumentException(
                    "goalAdjustmentPercentOverride must be between -15 and 10"
            );
        }
    }

    private static void requirePositive(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }
}
