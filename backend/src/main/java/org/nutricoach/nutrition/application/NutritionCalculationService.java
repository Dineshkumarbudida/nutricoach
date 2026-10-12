package org.nutricoach.nutrition.application;

import org.nutricoach.nutrition.domain.CalculationException;
import org.nutricoach.nutrition.domain.CalculationWarning;
import org.nutricoach.nutrition.domain.Completeness;
import org.nutricoach.nutrition.domain.FoodComposition;
import org.nutricoach.nutrition.domain.NutrientCode;
import org.nutricoach.nutrition.domain.NutrientObservation;
import org.nutricoach.nutrition.domain.NutrientTotal;
import org.nutricoach.nutrition.domain.NutritionCalculationResult;
import org.nutricoach.nutrition.domain.ObservationStatus;
import org.nutricoach.nutrition.domain.RecipeCalculationResult;
import org.nutricoach.nutrition.domain.RecipeIngredient;
import org.nutricoach.nutrition.domain.SourceReference;
import org.nutricoach.nutrition.domain.WarningCode;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Authoritative deterministic nutrition arithmetic.
 *
 * <p>This service has no dependency on Spring, persistence, HTTP, or an AI model.
 * All inputs are immutable snapshots so a result can be reproduced later.</p>
 */
public final class NutritionCalculationService {
    public static final String CONTRACT_VERSION = "nutrition-core/1.0.0";

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public NutritionCalculationResult scaleFood(
            FoodComposition food,
            BigDecimal portionMassGrams
    ) {
        Objects.requireNonNull(food, "food is required");
        requirePositive(portionMassGrams, "portionMassGrams");

        BigDecimal factor = divide(portionMassGrams, food.referenceMassGrams());
        EnumMap<NutrientCode, NutrientAccumulator> accumulators = newAccumulators();

        for (NutrientCode nutrient : NutrientCode.values()) {
            accumulators.get(nutrient).add(food.observationFor(nutrient), factor);
        }

        Map<NutrientCode, NutrientTotal> totals = finish(accumulators);
        List<CalculationWarning> warnings = warningsFor(totals);

        Map<String, String> assumptions = new LinkedHashMap<>();
        assumptions.put("scaling", "Linear mass scaling from the food reference basis.");
        assumptions.put("rounding", "No display rounding was applied by the calculation core.");
        assumptions.put("identity", "The caller supplied an immutable food and source version snapshot.");

        return new NutritionCalculationResult(
                "FOOD_PORTION",
                CONTRACT_VERSION,
                portionMassGrams,
                totals,
                List.of(food.source()),
                warnings,
                assumptions
        );
    }

    public RecipeCalculationResult calculateRecipe(
            List<RecipeIngredient> ingredients,
            BigDecimal cookedBatchMassGrams,
            BigDecimal portionMassGrams
    ) {
        Objects.requireNonNull(ingredients, "ingredients is required");
        if (ingredients.isEmpty()) {
            throw new CalculationException("EMPTY_RECIPE", "A recipe must contain at least one ingredient");
        }
        requirePositive(cookedBatchMassGrams, "cookedBatchMassGrams");
        requirePositive(portionMassGrams, "portionMassGrams");
        if (portionMassGrams.compareTo(cookedBatchMassGrams) > 0) {
            throw new CalculationException(
                    "PORTION_EXCEEDS_BATCH",
                    "portionMassGrams must not exceed cookedBatchMassGrams"
            );
        }

        EnumMap<NutrientCode, NutrientAccumulator> accumulators = newAccumulators();
        Set<SourceReference> sources = new LinkedHashSet<>();

        for (RecipeIngredient ingredient : ingredients) {
            Objects.requireNonNull(ingredient, "ingredients must not contain null elements");
            FoodComposition food = ingredient.food();
            BigDecimal factor = divide(ingredient.massGrams(), food.referenceMassGrams());
            sources.add(food.source());

            for (NutrientCode nutrient : NutrientCode.values()) {
                accumulators.get(nutrient).add(food.observationFor(nutrient), factor);
            }
        }

        Map<NutrientCode, NutrientTotal> batchTotals = finish(accumulators);
        List<CalculationWarning> batchWarnings = new ArrayList<>(warningsFor(batchTotals));
        batchWarnings.add(new CalculationWarning(
                WarningCode.COOKING_EFFECTS_NOT_MODELLED,
                null,
                "The calculation uses declared ingredient amounts and cooked batch mass. "
                        + "Unspecified nutrient retention, discarded liquid, vessel residue, bones, shells, "
                        + "and other losses are not guessed."
        ));

        Map<String, String> batchAssumptions = new LinkedHashMap<>();
        batchAssumptions.put("aggregation", "Batch totals are the sum of versioned ingredient contributions.");
        batchAssumptions.put("yield", "The declared final cooked batch mass controls concentration per gram.");
        batchAssumptions.put("cookingEffects", "Only explicitly represented cooking effects are included.");
        batchAssumptions.put("rounding", "No display rounding was applied by the calculation core.");

        NutritionCalculationResult batch = new NutritionCalculationResult(
                "RECIPE_BATCH",
                CONTRACT_VERSION,
                cookedBatchMassGrams,
                batchTotals,
                List.copyOf(sources),
                batchWarnings,
                batchAssumptions
        );

        BigDecimal per100Factor = divide(ONE_HUNDRED, cookedBatchMassGrams);
        NutritionCalculationResult per100 = scaledResult(
                "RECIPE_PER_100_GRAMS",
                ONE_HUNDRED,
                batch,
                per100Factor
        );

        BigDecimal portionFactor = divide(portionMassGrams, cookedBatchMassGrams);
        NutritionCalculationResult portion = scaledResult(
                "RECIPE_PORTION",
                portionMassGrams,
                batch,
                portionFactor
        );

        return new RecipeCalculationResult(
                cookedBatchMassGrams,
                portionMassGrams,
                batch,
                per100,
                portion
        );
    }

    private NutritionCalculationResult scaledResult(
            String calculationType,
            BigDecimal basisMassGrams,
            NutritionCalculationResult batch,
            BigDecimal factor
    ) {
        EnumMap<NutrientCode, NutrientTotal> scaledTotals = new EnumMap<>(NutrientCode.class);
        for (Map.Entry<NutrientCode, NutrientTotal> entry : batch.totals().entrySet()) {
            scaledTotals.put(entry.getKey(), scaleTotal(entry.getValue(), factor));
        }

        Map<String, String> assumptions = new LinkedHashMap<>(batch.assumptions());
        assumptions.put("derivedFrom", "Derived from the recipe batch without changing completeness semantics.");

        return new NutritionCalculationResult(
                calculationType,
                CONTRACT_VERSION,
                basisMassGrams,
                scaledTotals,
                batch.sources(),
                batch.warnings(),
                assumptions
        );
    }

    private NutrientTotal scaleTotal(NutrientTotal total, BigDecimal factor) {
        BigDecimal scaledKnown = multiply(total.knownAmount(), factor);
        BigDecimal scaledUpper = total.additionalUpperBound() == null
                ? null
                : multiply(total.additionalUpperBound(), factor);

        return new NutrientTotal(
                total.nutrient(),
                total.unit(),
                scaledKnown,
                total.completeness(),
                scaledUpper,
                total.measuredComponentCount(),
                total.estimatedComponentCount(),
                total.belowLimitComponentCount(),
                total.missingComponentCount(),
                total.notApplicableComponentCount()
        );
    }

    private EnumMap<NutrientCode, NutrientAccumulator> newAccumulators() {
        EnumMap<NutrientCode, NutrientAccumulator> result = new EnumMap<>(NutrientCode.class);
        for (NutrientCode nutrient : NutrientCode.values()) {
            result.put(nutrient, new NutrientAccumulator(nutrient));
        }
        return result;
    }

    private Map<NutrientCode, NutrientTotal> finish(
            EnumMap<NutrientCode, NutrientAccumulator> accumulators
    ) {
        EnumMap<NutrientCode, NutrientTotal> totals = new EnumMap<>(NutrientCode.class);
        for (Map.Entry<NutrientCode, NutrientAccumulator> entry : accumulators.entrySet()) {
            totals.put(entry.getKey(), entry.getValue().finish());
        }
        return Collections.unmodifiableMap(totals);
    }

    private List<CalculationWarning> warningsFor(Map<NutrientCode, NutrientTotal> totals) {
        List<CalculationWarning> warnings = new ArrayList<>();
        for (NutrientTotal total : totals.values()) {
            if (total.estimatedComponentCount() > 0) {
                warnings.add(new CalculationWarning(
                        WarningCode.ESTIMATED_SOURCE_VALUE,
                        total.nutrient(),
                        total.nutrient() + " includes one or more estimated source values."
                ));
            }
            if (total.belowLimitComponentCount() > 0) {
                warnings.add(new CalculationWarning(
                        WarningCode.BELOW_REPORTING_LIMIT,
                        total.nutrient(),
                        total.nutrient() + " includes values below a declared reporting limit."
                ));
            }
            if (total.missingComponentCount() > 0) {
                warnings.add(new CalculationWarning(
                        WarningCode.MISSING_NUTRIENT_DATA,
                        total.nutrient(),
                        total.nutrient() + " is an incomplete known subtotal because source data are missing."
                ));
            }
            if (total.notApplicableComponentCount() > 0) {
                warnings.add(new CalculationWarning(
                        WarningCode.NOT_APPLICABLE_VALUE,
                        total.nutrient(),
                        total.nutrient() + " contains a source value marked not applicable; it was not treated as zero."
                ));
            }
        }
        return List.copyOf(warnings);
    }

    private static BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        return numerator.divide(denominator, MC);
    }

    private static BigDecimal multiply(BigDecimal left, BigDecimal right) {
        return left.multiply(right, MC);
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new CalculationException("INVALID_QUANTITY", field + " must be positive");
        }
    }

    private static final class NutrientAccumulator {
        private final NutrientCode nutrient;
        private BigDecimal knownAmount = BigDecimal.ZERO;
        private BigDecimal boundedAdditionalUpper = BigDecimal.ZERO;
        private int measuredCount;
        private int estimatedCount;
        private int belowCount;
        private int missingCount;
        private int notApplicableCount;

        private NutrientAccumulator(NutrientCode nutrient) {
            this.nutrient = nutrient;
        }

        private void add(NutrientObservation observation, BigDecimal factor) {
            Objects.requireNonNull(observation, "observation is required");
            Objects.requireNonNull(factor, "factor is required");

            ObservationStatus status = observation.status();
            switch (status) {
                case MEASURED -> {
                    knownAmount = knownAmount.add(multiply(observation.amount(), factor), MC);
                    measuredCount++;
                }
                case ESTIMATED -> {
                    knownAmount = knownAmount.add(multiply(observation.amount(), factor), MC);
                    estimatedCount++;
                }
                case BELOW_REPORTING_LIMIT -> {
                    boundedAdditionalUpper = boundedAdditionalUpper.add(
                            multiply(observation.reportingLimit(), factor), MC);
                    belowCount++;
                }
                case MISSING -> missingCount++;
                case NOT_APPLICABLE -> notApplicableCount++;
            }
        }

        private NutrientTotal finish() {
            Completeness completeness;
            BigDecimal upper;

            if (missingCount > 0 || notApplicableCount > 0) {
                completeness = Completeness.UNBOUNDED_INCOMPLETE;
                upper = null;
            } else if (belowCount > 0) {
                completeness = Completeness.BOUNDED_INCOMPLETE;
                upper = boundedAdditionalUpper;
            } else {
                completeness = Completeness.COMPLETE;
                upper = BigDecimal.ZERO;
            }

            return new NutrientTotal(
                    nutrient,
                    nutrient.canonicalUnit(),
                    knownAmount,
                    completeness,
                    upper,
                    measuredCount,
                    estimatedCount,
                    belowCount,
                    missingCount,
                    notApplicableCount
            );
        }
    }
}
