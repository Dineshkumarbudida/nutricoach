package org.nutricoach.nutrition.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A nutrient observation on a food's declared reference basis.
 *
 * <p>A numeric zero with status MEASURED or ESTIMATED is a true zero. Missing,
 * not-applicable, and below-reporting-limit values intentionally carry no
 * numeric amount and must never be silently converted to zero.</p>
 */
public record NutrientObservation(
        ObservationStatus status,
        BigDecimal amount,
        BigDecimal reportingLimit
) {
    public NutrientObservation {
        Objects.requireNonNull(status, "status is required");

        switch (status) {
            case MEASURED, ESTIMATED -> {
                requireNonNegative(amount, "amount");
                if (reportingLimit != null) {
                    throw new IllegalArgumentException("reportingLimit must be null for numeric observations");
                }
            }
            case BELOW_REPORTING_LIMIT -> {
                if (amount != null) {
                    throw new IllegalArgumentException("amount must be null below a reporting limit");
                }
                requirePositive(reportingLimit, "reportingLimit");
            }
            case MISSING, NOT_APPLICABLE -> {
                if (amount != null || reportingLimit != null) {
                    throw new IllegalArgumentException(
                            "amount and reportingLimit must be null for " + status);
                }
            }
        }
    }

    public static NutrientObservation measured(BigDecimal amount) {
        return new NutrientObservation(ObservationStatus.MEASURED, amount, null);
    }

    public static NutrientObservation estimated(BigDecimal amount) {
        return new NutrientObservation(ObservationStatus.ESTIMATED, amount, null);
    }

    public static NutrientObservation belowReportingLimit(BigDecimal reportingLimit) {
        return new NutrientObservation(ObservationStatus.BELOW_REPORTING_LIMIT, null, reportingLimit);
    }

    public static NutrientObservation missing() {
        return new NutrientObservation(ObservationStatus.MISSING, null, null);
    }

    public static NutrientObservation notApplicable() {
        return new NutrientObservation(ObservationStatus.NOT_APPLICABLE, null, null);
    }

    private static void requireNonNegative(BigDecimal value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }
}
