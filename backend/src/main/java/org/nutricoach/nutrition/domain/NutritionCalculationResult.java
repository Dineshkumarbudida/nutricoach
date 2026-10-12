package org.nutricoach.nutrition.domain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record NutritionCalculationResult(
        String calculationType,
        String calculationContractVersion,
        BigDecimal basisMassGrams,
        Map<NutrientCode, NutrientTotal> totals,
        List<SourceReference> sources,
        List<CalculationWarning> warnings,
        Map<String, String> assumptions
) {
    public NutritionCalculationResult {
        Objects.requireNonNull(calculationType, "calculationType is required");
        calculationType = calculationType.trim();
        if (calculationType.isEmpty()) {
            throw new IllegalArgumentException("calculationType must not be blank");
        }
        Objects.requireNonNull(calculationContractVersion, "calculationContractVersion is required");
        calculationContractVersion = calculationContractVersion.trim();
        if (calculationContractVersion.isEmpty()) {
            throw new IllegalArgumentException("calculationContractVersion must not be blank");
        }
        Objects.requireNonNull(basisMassGrams, "basisMassGrams is required");
        if (basisMassGrams.signum() <= 0) {
            throw new IllegalArgumentException("basisMassGrams must be positive");
        }
        Objects.requireNonNull(totals, "totals is required");
        Objects.requireNonNull(sources, "sources is required");
        Objects.requireNonNull(warnings, "warnings is required");
        Objects.requireNonNull(assumptions, "assumptions is required");

        EnumMap<NutrientCode, NutrientTotal> copiedTotals = new EnumMap<>(NutrientCode.class);
        copiedTotals.putAll(totals);
        totals = Collections.unmodifiableMap(copiedTotals);
        sources = List.copyOf(sources);
        warnings = List.copyOf(warnings);
        assumptions = Map.copyOf(assumptions);
    }
}
