package org.nutricoach.nutrition;

import org.junit.jupiter.api.Test;
import org.nutricoach.nutrition.application.NutritionCalculationService;
import org.nutricoach.nutrition.domain.Completeness;
import org.nutricoach.nutrition.domain.FoodComposition;
import org.nutricoach.nutrition.domain.NutrientCode;
import org.nutricoach.nutrition.domain.NutrientObservation;
import org.nutricoach.nutrition.domain.RecipeIngredient;
import org.nutricoach.nutrition.domain.SourceReference;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NutritionCalculationServiceTest {
    private final NutritionCalculationService service = new NutritionCalculationService();

    @Test
    void missingValuesRemainIncomplete() {
        EnumMap<NutrientCode, NutrientObservation> nutrients = new EnumMap<>(NutrientCode.class);
        nutrients.put(NutrientCode.ENERGY, NutrientObservation.measured(new BigDecimal("100")));
        nutrients.put(NutrientCode.PROTEIN, NutrientObservation.missing());

        FoodComposition food = food("missing", nutrients);
        var total = service.scaleFood(food, new BigDecimal("200"))
                .totals().get(NutrientCode.PROTEIN);

        assertEquals(0, total.knownAmount().compareTo(BigDecimal.ZERO));
        assertEquals(Completeness.UNBOUNDED_INCOMPLETE, total.completeness());
        assertTrue(total.upperBound().isEmpty());
    }

    @Test
    void cookedYieldControlsPortionConcentration() {
        EnumMap<NutrientCode, NutrientObservation> nutrients = new EnumMap<>(NutrientCode.class);
        nutrients.put(NutrientCode.ENERGY, NutrientObservation.measured(new BigDecimal("100")));
        FoodComposition food = food("yield", nutrients);

        var result = service.calculateRecipe(
                List.of(new RecipeIngredient(food, new BigDecimal("100"), "synthetic")),
                new BigDecimal("80"),
                new BigDecimal("40")
        );

        assertEquals(0, result.portion().totals().get(NutrientCode.ENERGY)
                .knownAmount().compareTo(new BigDecimal("50.0")));
    }

    private FoodComposition food(
            String id,
            EnumMap<NutrientCode, NutrientObservation> nutrients
    ) {
        return new FoodComposition(
                id,
                "1",
                "Synthetic fixture",
                new BigDecimal("100"),
                nutrients,
                new SourceReference("synthetic-test", "1", id, "TEST-ONLY")
        );
    }
}
