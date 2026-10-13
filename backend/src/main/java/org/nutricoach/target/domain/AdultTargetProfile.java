package org.nutricoach.target.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record AdultTargetProfile(
        int ageYears,
        BigDecimal heightCm,
        BigDecimal weightKg,
        Sex sex,
        ActivityCategory activityCategory,
        Goal goal,
        boolean resistanceTraining
) {
    public AdultTargetProfile {
        if (ageYears < 19 || ageYears > 120) throw new IllegalArgumentException("ageYears must be 19..120");
        requirePositive(heightCm, "heightCm");
        requirePositive(weightKg, "weightKg");
        Objects.requireNonNull(sex, "sex is required");
        Objects.requireNonNull(activityCategory, "activityCategory is required");
        Objects.requireNonNull(goal, "goal is required");
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) throw new IllegalArgumentException(field + " must be positive");
    }

    public enum Sex { MALE, FEMALE }
    public enum ActivityCategory { INACTIVE, LOW_ACTIVE, ACTIVE, VERY_ACTIVE }
    public enum Goal { GENERAL_HEALTH, MAINTENANCE, FAT_LOSS, MUSCLE_GAIN, RECOMPOSITION }
}
