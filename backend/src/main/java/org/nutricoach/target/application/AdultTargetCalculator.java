package org.nutricoach.target.application;

import org.nutricoach.target.domain.AdultTargetProfile;
import org.nutricoach.target.domain.TargetEstimate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class AdultTargetCalculator {
    public static final String ENERGY_METHOD_VERSION = "DRI-EER-2023-ADULT";
    public static final String PROTEIN_METHOD_VERSION = "NUTRICOACH-PROTEIN-ADULT-2026.10";
    public static final String FIBER_METHOD_VERSION = "DRI-FIBER-14G-PER-1000KCAL";

    public TargetEstimate estimate(AdultTargetProfile profile) {
        BigDecimal energy = eer(profile);
        BigDecimal adjustment = switch (profile.goal()) {
            case FAT_LOSS -> new BigDecimal("0.90");
            case MUSCLE_GAIN -> new BigDecimal("1.05");
            case RECOMPOSITION, GENERAL_HEALTH, MAINTENANCE -> BigDecimal.ONE;
        };
        BigDecimal planning = round10(energy.multiply(adjustment));

        ProteinRange protein = protein(profile);
        BigDecimal fiber = planning.multiply(new BigDecimal("0.014"))
                .setScale(1, RoundingMode.HALF_UP);

        return new TargetEstimate(
                round10(energy),
                planning,
                protein.defaultGrams,
                protein.lowerGrams,
                protein.upperGrams,
                fiber,
                ENERGY_METHOD_VERSION,
                PROTEIN_METHOD_VERSION,
                FIBER_METHOD_VERSION,
                List.of(
                        "Health Canada: Equations to Estimate Energy Requirement",
                        "Health Canada: Reference Values for Macronutrients",
                        "Jager et al.: International Society of Sports Nutrition Position Stand: protein and exercise",
                        "Morton et al.: protein supplementation and resistance training meta-analysis"
                ),
                List.of(
                        "Estimated energy requirement is a population-based estimate; observed weight trend is an important real-world adequacy signal.",
                        "Goal adjustments are NutriCoach planning heuristics, not measurements of metabolism.",
                        "Protein ranges apply to generally healthy adults; clinical nutrition requires separate validated rules.",
                        "Fiber target uses the 14 g per 1,000 kcal reference."
                )
        );
    }

    private BigDecimal eer(AdultTargetProfile p) {
        BigDecimal a = BigDecimal.valueOf(p.ageYears());
        BigDecimal h = p.heightCm();
        BigDecimal w = p.weightKg();

        return switch (p.sex()) {
            case MALE -> switch (p.activityCategory()) {
                case INACTIVE -> linear(new BigDecimal("753.07"), new BigDecimal("-10.83"), a, new BigDecimal("6.50"), h, new BigDecimal("14.10"), w);
                case LOW_ACTIVE -> linear(new BigDecimal("581.47"), new BigDecimal("-10.83"), a, new BigDecimal("8.30"), h, new BigDecimal("14.94"), w);
                case ACTIVE -> linear(new BigDecimal("1004.82"), new BigDecimal("-10.83"), a, new BigDecimal("6.52"), h, new BigDecimal("15.91"), w);
                case VERY_ACTIVE -> linear(new BigDecimal("-517.88"), new BigDecimal("-10.83"), a, new BigDecimal("15.61"), h, new BigDecimal("19.11"), w);
            };
            case FEMALE -> switch (p.activityCategory()) {
                case INACTIVE -> linear(new BigDecimal("584.90"), new BigDecimal("-7.01"), a, new BigDecimal("5.72"), h, new BigDecimal("11.71"), w);
                case LOW_ACTIVE -> linear(new BigDecimal("575.77"), new BigDecimal("-7.01"), a, new BigDecimal("6.60"), h, new BigDecimal("12.14"), w);
                case ACTIVE -> linear(new BigDecimal("710.25"), new BigDecimal("-7.01"), a, new BigDecimal("6.54"), h, new BigDecimal("12.34"), w);
                case VERY_ACTIVE -> linear(new BigDecimal("511.83"), new BigDecimal("-7.01"), a, new BigDecimal("9.07"), h, new BigDecimal("12.56"), w);
            };
        };
    }

    private BigDecimal linear(BigDecimal c, BigDecimal aa, BigDecimal a, BigDecimal hh, BigDecimal h, BigDecimal ww, BigDecimal w) {
        return c.add(aa.multiply(a)).add(hh.multiply(h)).add(ww.multiply(w));
    }

    private ProteinRange protein(AdultTargetProfile p) {
        if (!p.resistanceTraining() && (p.goal() == AdultTargetProfile.Goal.GENERAL_HEALTH
                || p.goal() == AdultTargetProfile.Goal.MAINTENANCE)) {
            return range(p.weightKg(), new BigDecimal("0.8"), new BigDecimal("0.8"), new BigDecimal("0.8"));
        }
        if (p.resistanceTraining()
                || p.goal() == AdultTargetProfile.Goal.MUSCLE_GAIN
                || p.goal() == AdultTargetProfile.Goal.RECOMPOSITION
                || p.goal() == AdultTargetProfile.Goal.FAT_LOSS) {
            return range(p.weightKg(), new BigDecimal("1.4"), new BigDecimal("1.6"), new BigDecimal("2.0"));
        }
        return range(p.weightKg(), new BigDecimal("0.8"), new BigDecimal("0.8"), new BigDecimal("1.2"));
    }

    private ProteinRange range(BigDecimal weight, BigDecimal lower, BigDecimal standard, BigDecimal upper) {
        return new ProteinRange(weight.multiply(standard), weight.multiply(lower), weight.multiply(upper));
    }

    private BigDecimal round10(BigDecimal value) {
        return value.divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP).multiply(BigDecimal.TEN);
    }

    private record ProteinRange(BigDecimal defaultGrams, BigDecimal lowerGrams, BigDecimal upperGrams) {}
}
