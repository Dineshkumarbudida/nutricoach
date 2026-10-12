package org.nutricoach.targets.application;

import org.nutricoach.targets.domain.ActivityLevel;
import org.nutricoach.targets.domain.AdultTargetRequest;
import org.nutricoach.targets.domain.DailyTargetEstimate;
import org.nutricoach.targets.domain.EnergyEquationSet;
import org.nutricoach.targets.domain.EvidenceReference;
import org.nutricoach.targets.domain.Goal;
import org.nutricoach.targets.domain.NutrientTarget;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic adult target estimation with inspectable evidence and heuristics.
 *
 * <p>The service currently supports generally healthy, non-pregnant adults aged
 * 19 years and older. It estimates population-average needs; it is not a clinical
 * measurement or medical prescription.</p>
 */
public final class DailyTargetService {
    public static final String METHOD_VERSION = "adult-targets/1.0.0";
    public static final String EVIDENCE_REVIEW_DATE = "2026-10-12";

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");

    public DailyTargetEstimate estimate(AdultTargetRequest request) {
        BigDecimal maintenance = estimateMaintenanceEnergy(request);
        BigDecimal goalAdjustment = request.goalAdjustmentPercentOverride() == null
                ? defaultGoalAdjustment(request.goal())
                : request.goalAdjustmentPercentOverride();
        BigDecimal planning = maintenance.multiply(
                BigDecimal.ONE.add(goalAdjustment.divide(ONE_HUNDRED, MC), MC),
                MC
        );

        BigDecimal proteinFactor = proteinFactor(request);
        BigDecimal protein = request.weightKilograms().multiply(proteinFactor, MC);
        BigDecimal fiber = planning.multiply(new BigDecimal("14"), MC).divide(ONE_THOUSAND, MC);

        BigDecimal carbohydrateMinimum = energyPercentToGrams(planning, "45", "4");
        BigDecimal carbohydrateMaximum = energyPercentToGrams(planning, "65", "4");
        BigDecimal fatMinimum = energyPercentToGrams(planning, "20", "9");
        BigDecimal fatMaximum = energyPercentToGrams(planning, "35", "9");

        List<String> warnings = new ArrayList<>();
        warnings.add(
                "Energy equations estimate population-average requirements and do not measure an individual's metabolism."
        );
        if (goalAdjustment.signum() != 0) {
            warnings.add(
                    "The goal adjustment is a conservative NutriCoach planning heuristic, not part of the DRI EER equation."
            );
        }
        if (proteinFactor.compareTo(new BigDecimal("0.8")) > 0) {
            warnings.add(
                    "The higher protein factor is intended only for generally healthy exercising adults; clinical contexts require professional review."
            );
        }
        warnings.add(
                "Nutrient reference values describe average intake over time and should not be interpreted as a diagnosis from one day."
        );

        Map<String, String> assumptions = new LinkedHashMap<>();
        assumptions.put("population", "Generally healthy, non-pregnant adult aged 19 years or older.");
        assumptions.put("energy", "2023 DRI adult EER equation for the selected equation set and activity category.");
        assumptions.put("goalAdjustment", "Editable planning heuristic constrained to -15% through +10%.");
        assumptions.put("protein", proteinAssumption(request, proteinFactor));
        assumptions.put("fiber", "14 grams per 1,000 planning kilocalories.");
        assumptions.put("macroRanges", "Adult AMDR: carbohydrate 45-65% and fat 20-35% of energy.");
        assumptions.put("rounding", "Returned headline targets are rounded to one decimal; source rules remain versioned.");

        return new DailyTargetEstimate(
                METHOD_VERSION,
                "GENERALLY_HEALTHY_NONPREGNANT_ADULT_19_PLUS",
                oneDecimal(maintenance),
                oneDecimal(planning),
                oneDecimal(goalAdjustment),
                goalAdjustment.signum() == 0 ? "NONE" : "PRODUCT_HEURISTIC",
                oneDecimal(protein),
                oneDecimal(proteinFactor),
                oneDecimal(fiber),
                oneDecimal(carbohydrateMinimum),
                oneDecimal(carbohydrateMaximum),
                oneDecimal(fatMinimum),
                oneDecimal(fatMaximum),
                micronutrientTargets(request),
                evidenceReferences(),
                warnings,
                assumptions
        );
    }

    private BigDecimal estimateMaintenanceEnergy(AdultTargetRequest request) {
        BigDecimal age = BigDecimal.valueOf(request.ageYears());
        BigDecimal height = request.heightCentimeters();
        BigDecimal weight = request.weightKilograms();

        Coefficients coefficients = coefficients(request.equationSet(), request.activityLevel());
        return coefficients.constant()
                .subtract(coefficients.age().multiply(age, MC), MC)
                .add(coefficients.height().multiply(height, MC), MC)
                .add(coefficients.weight().multiply(weight, MC), MC);
    }

    private Coefficients coefficients(EnergyEquationSet equationSet, ActivityLevel activityLevel) {
        if (equationSet == EnergyEquationSet.MALE) {
            return switch (activityLevel) {
                case INACTIVE -> c("753.07", "10.83", "6.50", "14.10");
                case LOW_ACTIVE -> c("581.47", "10.83", "8.30", "14.94");
                case ACTIVE -> c("1004.82", "10.83", "6.52", "15.91");
                case VERY_ACTIVE -> c("-517.88", "10.83", "15.61", "19.11");
            };
        }
        return switch (activityLevel) {
            case INACTIVE -> c("584.90", "7.01", "5.72", "11.71");
            case LOW_ACTIVE -> c("575.77", "7.01", "6.60", "12.14");
            case ACTIVE -> c("710.25", "7.01", "6.54", "12.34");
            case VERY_ACTIVE -> c("511.83", "7.01", "9.07", "12.56");
        };
    }

    private static Coefficients c(String constant, String age, String height, String weight) {
        return new Coefficients(
                new BigDecimal(constant),
                new BigDecimal(age),
                new BigDecimal(height),
                new BigDecimal(weight)
        );
    }

    private BigDecimal defaultGoalAdjustment(Goal goal) {
        return switch (goal) {
            case FAT_LOSS -> new BigDecimal("-10");
            case MUSCLE_GAIN -> new BigDecimal("5");
            case GENERAL_HEALTH, WEIGHT_MAINTENANCE, BODY_RECOMPOSITION -> BigDecimal.ZERO;
        };
    }

    private BigDecimal proteinFactor(AdultTargetRequest request) {
        boolean exercise = request.trainingDaysPerWeek() >= 3
                || request.activityLevel() == ActivityLevel.ACTIVE
                || request.activityLevel() == ActivityLevel.VERY_ACTIVE;
        boolean resistanceGoal = request.resistanceTraining()
                && (request.goal() == Goal.MUSCLE_GAIN
                || request.goal() == Goal.BODY_RECOMPOSITION
                || request.goal() == Goal.FAT_LOSS);

        if (resistanceGoal) {
            return new BigDecimal("1.6");
        }
        if (exercise) {
            return new BigDecimal("1.4");
        }
        return new BigDecimal("0.8");
    }

    private String proteinAssumption(AdultTargetRequest request, BigDecimal factor) {
        if (factor.compareTo(new BigDecimal("1.6")) == 0) {
            return "1.6 g/kg/day for resistance-training goals, aligned with the pooled breakpoint reported by Morton et al. (2018).";
        }
        if (factor.compareTo(new BigDecimal("1.4")) == 0) {
            return "1.4 g/kg/day, the lower end of the ISSN range for generally healthy exercising adults.";
        }
        return "0.8 g/kg/day adult RDA for a generally healthy, non-training context.";
    }

    private BigDecimal energyPercentToGrams(BigDecimal energy, String percent, String kcalPerGram) {
        return energy.multiply(new BigDecimal(percent), MC)
                .divide(ONE_HUNDRED, MC)
                .divide(new BigDecimal(kcalPerGram), MC);
    }

    private Map<String, NutrientTarget> micronutrientTargets(AdultTargetRequest request) {
        boolean male = request.equationSet() == EnergyEquationSet.MALE;
        int age = request.ageYears();
        LinkedHashMap<String, NutrientTarget> result = new LinkedHashMap<>();

        add(result, "CALCIUM", "Calcium", calcium(age, male), "mg", "RDA", null, "dri-elements");
        add(result, "IRON", "Iron", iron(age, male), "mg", "RDA", "45", "dri-elements");
        add(result, "MAGNESIUM", "Magnesium", magnesium(age, male), "mg", "RDA", null, "dri-elements");
        add(result, "PHOSPHORUS", "Phosphorus", "700", "mg", "RDA", "4000", "dri-elements");
        add(result, "POTASSIUM", "Potassium", male ? "3400" : "2600", "mg", "AI", null, "dri-elements");
        add(result, "SODIUM", "Sodium", "1500", "mg", "AI", "2300", "dri-elements");
        add(result, "ZINC", "Zinc", male ? "11" : "8", "mg", "RDA", male ? "40" : "40", "dri-elements");
        add(result, "SELENIUM", "Selenium", "55", "mcg", "RDA", "400", "dri-elements");
        add(result, "COPPER", "Copper", "900", "mcg", "RDA", "10000", "dri-elements");
        add(result, "IODINE", "Iodine", "150", "mcg", "RDA", "1100", "dri-elements");
        add(result, "MANGANESE", "Manganese", male ? "2.3" : "1.8", "mg", "AI", male ? "11" : "11", "dri-elements");

        add(result, "VITAMIN_A_RAE", "Vitamin A", male ? "900" : "700", "mcg RAE", "RDA", "3000", "dri-vitamins");
        add(result, "VITAMIN_C", "Vitamin C", male ? "90" : "75", "mg", "RDA", "2000", "dri-vitamins");
        add(result, "VITAMIN_D", "Vitamin D", age > 70 ? "20" : "15", "mcg", "RDA", "100", "dri-vitamins");
        add(result, "VITAMIN_E", "Vitamin E", "15", "mg", "RDA", "1000", "dri-vitamins");
        add(result, "VITAMIN_K", "Vitamin K", male ? "120" : "90", "mcg", "AI", null, "dri-vitamins");
        add(result, "THIAMIN", "Thiamin", male ? "1.2" : "1.1", "mg", "RDA", null, "dri-vitamins");
        add(result, "RIBOFLAVIN", "Riboflavin", male ? "1.3" : "1.1", "mg", "RDA", null, "dri-vitamins");
        add(result, "NIACIN", "Niacin", male ? "16" : "14", "mg NE", "RDA", "35", "dri-vitamins");
        add(result, "VITAMIN_B6", "Vitamin B6", vitaminB6(age, male), "mg", "RDA", "100", "dri-vitamins");
        add(result, "FOLATE_DFE", "Folate", "400", "mcg DFE", "RDA", "1000", "dri-vitamins");
        add(result, "VITAMIN_B12", "Vitamin B12", "2.4", "mcg", "RDA", null, "dri-vitamins");
        add(result, "CHOLINE", "Choline", male ? "550" : "425", "mg", "AI", "3500", "dri-vitamins");
        add(result, "PANTOTHENIC_ACID", "Pantothenic acid", "5", "mg", "AI", null, "dri-vitamins");
        add(result, "BIOTIN", "Biotin", "30", "mcg", "AI", null, "dri-vitamins");

        return result;
    }

    private String calcium(int age, boolean male) {
        if (male) {
            return age > 70 ? "1200" : "1000";
        }
        return age > 50 ? "1200" : "1000";
    }

    private String iron(int age, boolean male) {
        return !male && age <= 50 ? "18" : "8";
    }

    private String magnesium(int age, boolean male) {
        if (male) {
            return age <= 30 ? "400" : "420";
        }
        return age <= 30 ? "310" : "320";
    }

    private String vitaminB6(int age, boolean male) {
        if (age <= 50) {
            return "1.3";
        }
        return male ? "1.7" : "1.5";
    }

    private void add(
            Map<String, NutrientTarget> targets,
            String code,
            String name,
            BigDecimal amount,
            String unit,
            String referenceType,
            BigDecimal threshold,
            String sourceId
    ) {
        targets.put(code, new NutrientTarget(
                code,
                name,
                amount,
                unit,
                referenceType,
                threshold,
                sourceId
        ));
    }

    private void add(
            Map<String, NutrientTarget> targets,
            String code,
            String name,
            String amount,
            String unit,
            String referenceType,
            String threshold,
            String sourceId
    ) {
        add(
                targets,
                code,
                name,
                new BigDecimal(amount),
                unit,
                referenceType,
                threshold == null ? null : new BigDecimal(threshold),
                sourceId
        );
    }

    private List<EvidenceReference> evidenceReferences() {
        return List.of(
                new EvidenceReference(
                        "dri-eer-2023",
                        "Dietary Reference Intakes: equations to estimate energy requirement",
                        "OFFICIAL_GUIDANCE",
                        "https://www.canada.ca/en/health-canada/services/food-nutrition/healthy-eating/dietary-reference-intakes/tables/equations-estimate-energy-requirement.html",
                        "Adults aged 19 years and older; population-average energy requirement estimate.",
                        EVIDENCE_REVIEW_DATE
                ),
                new EvidenceReference(
                        "dri-macronutrients",
                        "Dietary Reference Intakes: reference values for macronutrients",
                        "OFFICIAL_GUIDANCE",
                        "https://www.canada.ca/en/health-canada/services/food-nutrition/healthy-eating/dietary-reference-intakes/tables/reference-values-macronutrients.html",
                        "Generally healthy populations; protein RDA, fiber basis, and adult AMDR.",
                        EVIDENCE_REVIEW_DATE
                ),
                new EvidenceReference(
                        "dri-elements",
                        "Dietary Reference Intakes: reference values for elements",
                        "OFFICIAL_GUIDANCE",
                        "https://www.canada.ca/en/health-canada/services/food-nutrition/healthy-eating/dietary-reference-intakes/tables/reference-values-elements.html",
                        "Generally healthy populations; age- and equation-set-specific mineral references.",
                        EVIDENCE_REVIEW_DATE
                ),
                new EvidenceReference(
                        "dri-vitamins",
                        "Dietary Reference Intakes: reference values for vitamins",
                        "OFFICIAL_GUIDANCE",
                        "https://www.canada.ca/en/health-canada/services/food-nutrition/healthy-eating/dietary-reference-intakes/tables/reference-values-vitamins.html",
                        "Generally healthy populations; age- and equation-set-specific vitamin references.",
                        EVIDENCE_REVIEW_DATE
                ),
                new EvidenceReference(
                        "issn-protein-2017",
                        "International Society of Sports Nutrition Position Stand: protein and exercise",
                        "POSITION_STAND",
                        "https://pubmed.ncbi.nlm.nih.gov/28642676/",
                        "Generally healthy exercising adults; 1.4-2.0 g/kg/day range.",
                        EVIDENCE_REVIEW_DATE
                ),
                new EvidenceReference(
                        "morton-protein-2018",
                        "Protein supplementation and resistance-training gains: systematic review and meta-regression",
                        "SYSTEMATIC_REVIEW_META_ANALYSIS",
                        "https://pubmed.ncbi.nlm.nih.gov/28698222/",
                        "Healthy adults undertaking resistance training; pooled breakpoint near 1.6 g/kg/day.",
                        EVIDENCE_REVIEW_DATE
                )
        );
    }

    private BigDecimal oneDecimal(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP);
    }

    private record Coefficients(
            BigDecimal constant,
            BigDecimal age,
            BigDecimal height,
            BigDecimal weight
    ) {
    }
}
