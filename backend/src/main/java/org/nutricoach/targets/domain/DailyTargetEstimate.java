package org.nutricoach.targets.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DailyTargetEstimate(
        String methodVersion,
        String population,
        BigDecimal maintenanceEnergyKilocalories,
        BigDecimal planningEnergyKilocalories,
        BigDecimal goalAdjustmentPercent,
        String goalAdjustmentClassification,
        BigDecimal proteinGrams,
        BigDecimal proteinFactorGramsPerKilogram,
        BigDecimal fiberGrams,
        BigDecimal carbohydrateMinimumGrams,
        BigDecimal carbohydrateMaximumGrams,
        BigDecimal fatMinimumGrams,
        BigDecimal fatMaximumGrams,
        Map<String, NutrientTarget> micronutrients,
        List<EvidenceReference> evidence,
        List<String> warnings,
        Map<String, String> assumptions
) {
    public DailyTargetEstimate {
        methodVersion = requireText(methodVersion, "methodVersion");
        population = requireText(population, "population");
        Objects.requireNonNull(maintenanceEnergyKilocalories, "maintenanceEnergyKilocalories is required");
        Objects.requireNonNull(planningEnergyKilocalories, "planningEnergyKilocalories is required");
        Objects.requireNonNull(goalAdjustmentPercent, "goalAdjustmentPercent is required");
        goalAdjustmentClassification = requireText(
                goalAdjustmentClassification,
                "goalAdjustmentClassification"
        );
        Objects.requireNonNull(proteinGrams, "proteinGrams is required");
        Objects.requireNonNull(proteinFactorGramsPerKilogram, "proteinFactorGramsPerKilogram is required");
        Objects.requireNonNull(fiberGrams, "fiberGrams is required");
        Objects.requireNonNull(carbohydrateMinimumGrams, "carbohydrateMinimumGrams is required");
        Objects.requireNonNull(carbohydrateMaximumGrams, "carbohydrateMaximumGrams is required");
        Objects.requireNonNull(fatMinimumGrams, "fatMinimumGrams is required");
        Objects.requireNonNull(fatMaximumGrams, "fatMaximumGrams is required");
        micronutrients = Map.copyOf(Objects.requireNonNull(micronutrients, "micronutrients is required"));
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence is required"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings is required"));
        assumptions = Map.copyOf(Objects.requireNonNull(assumptions, "assumptions is required"));
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }
}
