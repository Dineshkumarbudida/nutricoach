package org.nutricoach.nutrition;

import org.nutricoach.nutrition.application.NutritionCalculationService;
import org.nutricoach.nutrition.domain.CalculationException;
import org.nutricoach.nutrition.domain.Completeness;
import org.nutricoach.nutrition.domain.FoodComposition;
import org.nutricoach.nutrition.domain.NutrientCode;
import org.nutricoach.nutrition.domain.NutrientObservation;
import org.nutricoach.nutrition.domain.NutrientTotal;
import org.nutricoach.nutrition.domain.NutritionCalculationResult;
import org.nutricoach.nutrition.domain.RecipeCalculationResult;
import org.nutricoach.nutrition.domain.RecipeIngredient;
import org.nutricoach.nutrition.domain.SourceReference;
import org.nutricoach.nutrition.domain.WarningCode;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Dependency-free executable contract tests for the calculation domain.
 * All values are synthetic fixtures, not nutrition facts.
 */
public final class CoreCalculationSmokeTest {
    private final NutritionCalculationService service = new NutritionCalculationService();
    private int assertions;

    public static void main(String[] args) {
        CoreCalculationSmokeTest test = new CoreCalculationSmokeTest();
        test.run();
    }

    private void run() {
        scalesKnownValuesLinearly();
        preservesTrueZero();
        doesNotConvertMissingToZero();
        scalesReportingLimitAsBoundedRange();
        aggregatesRecipeAndAppliesCookedYield();
        propagatesMissingIngredientData();
        rejectsInvalidMass();
        rejectsPortionLargerThanBatch();
        producesIdenticalResultsForIdenticalInputs();
        preservesDecimalPrecisionWithoutDisplayRounding();
        marksOmittedNutrientsAsMissing();
        preservesEstimatedStatusAsWarning();
        treatsNotApplicableAsUnboundedRatherThanZero();
        missingDataDominatesReportingLimitBound();
        deDuplicatesRecipeSources();
        copiesInputMapsAndReturnsImmutableTotals();
        acceptsPortionEqualToBatch();
        rejectsEmptyRecipe();
        validatesObservationShapes();

        System.out.println("PASS: " + assertions + " core assertions");
    }

    private void scalesKnownValuesLinearly() {
        FoodComposition food = food("scale", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("100")),
                NutrientCode.PROTEIN, NutrientObservation.measured(decimal("10")),
                NutrientCode.CARBOHYDRATE, NutrientObservation.measured(decimal("20")),
                NutrientCode.FAT, NutrientObservation.measured(decimal("2")),
                NutrientCode.FIBER, NutrientObservation.measured(decimal("3"))
        ));

        NutritionCalculationResult result = service.scaleFood(food, decimal("250"));
        assertDecimal("250", result.totals().get(NutrientCode.ENERGY).knownAmount());
        assertDecimal("25", result.totals().get(NutrientCode.PROTEIN).knownAmount());
        assertEquals(Completeness.COMPLETE,
                result.totals().get(NutrientCode.FIBER).completeness());
    }

    private void preservesTrueZero() {
        FoodComposition food = food("zero", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(BigDecimal.ZERO),
                NutrientCode.PROTEIN, NutrientObservation.measured(BigDecimal.ZERO),
                NutrientCode.CARBOHYDRATE, NutrientObservation.measured(BigDecimal.ZERO),
                NutrientCode.FAT, NutrientObservation.measured(BigDecimal.ZERO),
                NutrientCode.FIBER, NutrientObservation.measured(BigDecimal.ZERO)
        ));

        NutrientTotal energy = service.scaleFood(food, decimal("75"))
                .totals().get(NutrientCode.ENERGY);
        assertDecimal("0", energy.knownAmount());
        assertEquals(Completeness.COMPLETE, energy.completeness());
        assertEquals(1, energy.measuredComponentCount());
    }

    private void doesNotConvertMissingToZero() {
        FoodComposition food = food("missing", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("100")),
                NutrientCode.PROTEIN, NutrientObservation.missing()
        ));

        NutrientTotal protein = service.scaleFood(food, decimal("100"))
                .totals().get(NutrientCode.PROTEIN);
        assertDecimal("0", protein.knownAmount());
        assertEquals(Completeness.UNBOUNDED_INCOMPLETE, protein.completeness());
        assertTrue(protein.upperBound().isEmpty(), "missing data must not claim an upper bound");
        assertEquals(1, protein.missingComponentCount());
    }

    private void scalesReportingLimitAsBoundedRange() {
        FoodComposition food = food("limit", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("10")),
                NutrientCode.PROTEIN, NutrientObservation.belowReportingLimit(decimal("0.5"))
        ));

        NutrientTotal protein = service.scaleFood(food, decimal("200"))
                .totals().get(NutrientCode.PROTEIN);
        assertEquals(Completeness.BOUNDED_INCOMPLETE, protein.completeness());
        assertDecimal("0", protein.lowerBound());
        assertDecimal("1.0", protein.upperBound().orElseThrow());
    }

    private void aggregatesRecipeAndAppliesCookedYield() {
        FoodComposition first = food("ingredient-a", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("100")),
                NutrientCode.PROTEIN, NutrientObservation.measured(decimal("10")),
                NutrientCode.CARBOHYDRATE, NutrientObservation.measured(decimal("15")),
                NutrientCode.FAT, NutrientObservation.measured(decimal("2")),
                NutrientCode.FIBER, NutrientObservation.measured(decimal("3"))
        ));
        FoodComposition second = food("ingredient-b", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("200")),
                NutrientCode.PROTEIN, NutrientObservation.measured(decimal("20")),
                NutrientCode.CARBOHYDRATE, NutrientObservation.measured(decimal("30")),
                NutrientCode.FAT, NutrientObservation.measured(decimal("4")),
                NutrientCode.FIBER, NutrientObservation.measured(decimal("6"))
        ));

        RecipeCalculationResult result = service.calculateRecipe(
                List.of(
                        new RecipeIngredient(first, decimal("100"), "synthetic"),
                        new RecipeIngredient(second, decimal("50"), "synthetic")
                ),
                decimal("120"),
                decimal("60")
        );

        assertDecimal("200", result.batch().totals().get(NutrientCode.ENERGY).knownAmount());
        assertDecimal("166.6666666666666666666666666666667",
                result.per100Grams().totals().get(NutrientCode.ENERGY).knownAmount());
        assertDecimal("100.0", result.portion().totals().get(NutrientCode.ENERGY).knownAmount());
    }

    private void propagatesMissingIngredientData() {
        FoodComposition known = food("known", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("100")),
                NutrientCode.PROTEIN, NutrientObservation.measured(decimal("10"))
        ));
        FoodComposition incomplete = food("incomplete", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("20")),
                NutrientCode.PROTEIN, NutrientObservation.missing()
        ));

        RecipeCalculationResult result = service.calculateRecipe(
                List.of(
                        new RecipeIngredient(known, decimal("100"), ""),
                        new RecipeIngredient(incomplete, decimal("100"), "")
                ),
                decimal("200"),
                decimal("100")
        );

        NutrientTotal protein = result.portion().totals().get(NutrientCode.PROTEIN);
        assertDecimal("5.0", protein.knownAmount());
        assertEquals(Completeness.UNBOUNDED_INCOMPLETE, protein.completeness());
        assertEquals(1, protein.missingComponentCount());
    }

    private void rejectsInvalidMass() {
        FoodComposition food = food("invalid-mass", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("1"))
        ));
        assertThrows("INVALID_QUANTITY", () -> service.scaleFood(food, BigDecimal.ZERO));
    }

    private void rejectsPortionLargerThanBatch() {
        FoodComposition food = food("batch", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("1"))
        ));
        assertThrows("PORTION_EXCEEDS_BATCH", () -> service.calculateRecipe(
                List.of(new RecipeIngredient(food, decimal("100"), "")),
                decimal("100"),
                decimal("101")
        ));
    }

    private void producesIdenticalResultsForIdenticalInputs() {
        FoodComposition food = food("deterministic", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("123.456")),
                NutrientCode.PROTEIN, NutrientObservation.estimated(decimal("7.89"))
        ));

        NutritionCalculationResult first = service.scaleFood(food, decimal("37.25"));
        NutritionCalculationResult second = service.scaleFood(food, decimal("37.25"));
        assertEquals(first, second);
        assertEquals(NutritionCalculationService.CONTRACT_VERSION,
                first.calculationContractVersion());
    }

    private void preservesDecimalPrecisionWithoutDisplayRounding() {
        FoodComposition food = food("precision", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("1"))
        ));

        BigDecimal result = service.scaleFood(food, decimal("33.333333333333333333"))
                .totals().get(NutrientCode.ENERGY).knownAmount();
        assertDecimal("0.33333333333333333333", result);
    }


    private void marksOmittedNutrientsAsMissing() {
        FoodComposition food = food("omitted", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("50"))
        ));

        NutrientTotal fiber = service.scaleFood(food, decimal("100"))
                .totals().get(NutrientCode.FIBER);
        assertEquals(Completeness.UNBOUNDED_INCOMPLETE, fiber.completeness());
        assertEquals(1, fiber.missingComponentCount());
        assertDecimal("0", fiber.knownAmount());
    }

    private void preservesEstimatedStatusAsWarning() {
        FoodComposition food = food("estimated", mapOf(
                NutrientCode.ENERGY, NutrientObservation.estimated(decimal("80")),
                NutrientCode.PROTEIN, NutrientObservation.measured(decimal("4")),
                NutrientCode.CARBOHYDRATE, NutrientObservation.measured(decimal("8")),
                NutrientCode.FAT, NutrientObservation.measured(decimal("2")),
                NutrientCode.FIBER, NutrientObservation.measured(decimal("1"))
        ));

        NutritionCalculationResult result = service.scaleFood(food, decimal("50"));
        NutrientTotal energy = result.totals().get(NutrientCode.ENERGY);
        assertDecimal("40.0", energy.knownAmount());
        assertEquals(Completeness.COMPLETE, energy.completeness());
        assertEquals(1, energy.estimatedComponentCount());
        assertTrue(result.warnings().stream().anyMatch(warning ->
                        warning.code() == WarningCode.ESTIMATED_SOURCE_VALUE
                                && warning.nutrient() == NutrientCode.ENERGY),
                "estimated values must produce a nutrient-specific warning");
    }

    private void treatsNotApplicableAsUnboundedRatherThanZero() {
        FoodComposition food = food("not-applicable", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("10")),
                NutrientCode.PROTEIN, NutrientObservation.notApplicable()
        ));

        NutrientTotal protein = service.scaleFood(food, decimal("100"))
                .totals().get(NutrientCode.PROTEIN);
        assertEquals(Completeness.UNBOUNDED_INCOMPLETE, protein.completeness());
        assertEquals(1, protein.notApplicableComponentCount());
        assertTrue(protein.upperBound().isEmpty(),
                "not-applicable must not be interpreted as a numeric zero");
    }

    private void missingDataDominatesReportingLimitBound() {
        FoodComposition bounded = food("bounded", mapOf(
                NutrientCode.PROTEIN, NutrientObservation.belowReportingLimit(decimal("0.2"))
        ));
        FoodComposition missing = food("missing-bound", mapOf(
                NutrientCode.PROTEIN, NutrientObservation.missing()
        ));

        NutrientTotal protein = service.calculateRecipe(
                        List.of(
                                new RecipeIngredient(bounded, decimal("100"), ""),
                                new RecipeIngredient(missing, decimal("100"), "")
                        ),
                        decimal("200"),
                        decimal("100")
                )
                .portion().totals().get(NutrientCode.PROTEIN);

        assertEquals(Completeness.UNBOUNDED_INCOMPLETE, protein.completeness());
        assertEquals(1, protein.belowLimitComponentCount());
        assertEquals(1, protein.missingComponentCount());
        assertTrue(protein.upperBound().isEmpty(),
                "a reporting limit cannot bound a separate fully missing component");
    }

    private void deDuplicatesRecipeSources() {
        SourceReference shared = new SourceReference("synthetic-test", "1", "shared", "TEST-ONLY");
        FoodComposition first = new FoodComposition(
                "source-a", "1", "Synthetic A", decimal("100"),
                mapOf(NutrientCode.ENERGY, NutrientObservation.measured(decimal("10"))), shared);
        FoodComposition second = new FoodComposition(
                "source-b", "1", "Synthetic B", decimal("100"),
                mapOf(NutrientCode.ENERGY, NutrientObservation.measured(decimal("20"))), shared);

        RecipeCalculationResult result = service.calculateRecipe(
                List.of(
                        new RecipeIngredient(first, decimal("100"), ""),
                        new RecipeIngredient(second, decimal("100"), "")
                ),
                decimal("200"),
                decimal("100")
        );

        assertEquals(1, result.batch().sources().size());
        assertEquals(shared, result.batch().sources().getFirst());
    }

    private void copiesInputMapsAndReturnsImmutableTotals() {
        EnumMap<NutrientCode, NutrientObservation> input = new EnumMap<>(NutrientCode.class);
        input.put(NutrientCode.ENERGY, NutrientObservation.measured(decimal("10")));
        FoodComposition food = food("immutable", input);
        input.put(NutrientCode.ENERGY, NutrientObservation.measured(decimal("999")));

        NutritionCalculationResult result = service.scaleFood(food, decimal("100"));
        assertDecimal("10", result.totals().get(NutrientCode.ENERGY).knownAmount());
        assertThrowsType(UnsupportedOperationException.class, () ->
                result.totals().put(NutrientCode.ENERGY,
                        result.totals().get(NutrientCode.ENERGY)));
    }

    private void acceptsPortionEqualToBatch() {
        FoodComposition food = food("full-batch", mapOf(
                NutrientCode.ENERGY, NutrientObservation.measured(decimal("75"))
        ));
        RecipeCalculationResult result = service.calculateRecipe(
                List.of(new RecipeIngredient(food, decimal("100"), "")),
                decimal("90"),
                decimal("90")
        );
        assertDecimal("75", result.portion().totals().get(NutrientCode.ENERGY).knownAmount());
        assertDecimal("90", result.portion().basisMassGrams());
    }

    private void rejectsEmptyRecipe() {
        assertThrows("EMPTY_RECIPE", () -> service.calculateRecipe(
                List.of(), decimal("100"), decimal("50")));
    }

    private void validatesObservationShapes() {
        assertThrowsType(IllegalArgumentException.class, () ->
                new NutrientObservation(
                        org.nutricoach.nutrition.domain.ObservationStatus.MEASURED,
                        null,
                        null));
        assertThrowsType(IllegalArgumentException.class, () ->
                NutrientObservation.belowReportingLimit(BigDecimal.ZERO));
        assertThrowsType(IllegalArgumentException.class, () ->
                NutrientObservation.measured(decimal("-1")));
    }

    private FoodComposition food(
            String id,
            Map<NutrientCode, NutrientObservation> nutrients
    ) {
        return new FoodComposition(
                id,
                "1",
                "Synthetic " + id,
                decimal("100"),
                nutrients,
                new SourceReference("synthetic-test", "1", id, "TEST-ONLY")
        );
    }

    private static Map<NutrientCode, NutrientObservation> mapOf(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("entries must be key-value pairs");
        }
        EnumMap<NutrientCode, NutrientObservation> result = new EnumMap<>(NutrientCode.class);
        for (int index = 0; index < entries.length; index += 2) {
            result.put((NutrientCode) entries[index], (NutrientObservation) entries[index + 1]);
        }
        return result;
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private void assertDecimal(String expected, BigDecimal actual) {
        assertions++;
        BigDecimal expectedDecimal = decimal(expected);
        if (expectedDecimal.compareTo(actual) != 0) {
            throw new AssertionError("Expected " + expectedDecimal + " but was " + actual);
        }
    }

    private void assertEquals(Object expected, Object actual) {
        assertions++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private void assertTrue(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private void assertThrowsType(Class<? extends Throwable> expectedType, Runnable action) {
        assertions++;
        try {
            action.run();
            throw new AssertionError("Expected " + expectedType.getSimpleName());
        } catch (Throwable throwable) {
            if (!expectedType.isInstance(throwable)) {
                throw new AssertionError(
                        "Expected " + expectedType.getSimpleName()
                                + " but was " + throwable.getClass().getSimpleName(), throwable);
            }
        }
    }

    private void assertThrows(String expectedCode, Runnable action) {
        assertions++;
        try {
            action.run();
            throw new AssertionError("Expected CalculationException with code " + expectedCode);
        } catch (CalculationException exception) {
            if (!expectedCode.equals(exception.code())) {
                throw new AssertionError(
                        "Expected code " + expectedCode + " but was " + exception.code());
            }
        }
    }
}
