package org.nutricoach.nutrition.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * A total that preserves uncertainty instead of converting unknown values to zero.
 *
 * @param knownAmount numeric subtotal from measured and estimated observations
 * @param additionalUpperBound possible amount above knownAmount when completeness is bounded
 */
public record NutrientTotal(
        NutrientCode nutrient,
        NutrientUnit unit,
        BigDecimal knownAmount,
        Completeness completeness,
        BigDecimal additionalUpperBound,
        int measuredComponentCount,
        int estimatedComponentCount,
        int belowLimitComponentCount,
        int missingComponentCount,
        int notApplicableComponentCount
) {
    public NutrientTotal {
        Objects.requireNonNull(nutrient, "nutrient is required");
        Objects.requireNonNull(unit, "unit is required");
        Objects.requireNonNull(knownAmount, "knownAmount is required");
        Objects.requireNonNull(completeness, "completeness is required");

        if (unit != nutrient.canonicalUnit()) {
            throw new IllegalArgumentException("unit must match the nutrient canonical unit");
        }
        if (knownAmount.signum() < 0) {
            throw new IllegalArgumentException("knownAmount must be non-negative");
        }
        if (measuredComponentCount < 0 || estimatedComponentCount < 0
                || belowLimitComponentCount < 0 || missingComponentCount < 0
                || notApplicableComponentCount < 0) {
            throw new IllegalArgumentException("component counts must be non-negative");
        }

        switch (completeness) {
            case COMPLETE -> {
                if (additionalUpperBound == null) {
                    additionalUpperBound = BigDecimal.ZERO;
                }
                if (additionalUpperBound.signum() != 0) {
                    throw new IllegalArgumentException("complete totals cannot have an uncertainty bound");
                }
            }
            case BOUNDED_INCOMPLETE -> {
                Objects.requireNonNull(additionalUpperBound,
                        "additionalUpperBound is required for bounded totals");
                if (additionalUpperBound.signum() <= 0) {
                    throw new IllegalArgumentException("additionalUpperBound must be positive");
                }
            }
            case UNBOUNDED_INCOMPLETE -> {
                if (additionalUpperBound != null) {
                    throw new IllegalArgumentException(
                            "unbounded totals must not claim an upper bound");
                }
            }
        }
    }

    public BigDecimal lowerBound() {
        return knownAmount;
    }

    public Optional<BigDecimal> upperBound() {
        return completeness == Completeness.UNBOUNDED_INCOMPLETE
                ? Optional.empty()
                : Optional.of(knownAmount.add(additionalUpperBound));
    }
}
