package org.nutricoach.targets;

import org.nutricoach.targets.application.DailyTargetService;
import org.nutricoach.targets.domain.ActivityLevel;
import org.nutricoach.targets.domain.AdultTargetRequest;
import org.nutricoach.targets.domain.DailyTargetEstimate;
import org.nutricoach.targets.domain.EnergyEquationSet;
import org.nutricoach.targets.domain.Goal;

import java.math.BigDecimal;

public final class DailyTargetServiceSmokeTest {
    private int assertions;

    public static void main(String[] args) {
        DailyTargetServiceSmokeTest test = new DailyTargetServiceSmokeTest();
        test.run();
        System.out.println("PASS: " + test.assertions + " target assertions");
    }

    private void run() {
        DailyTargetService service = new DailyTargetService();

        AdultTargetRequest activeMale = new AdultTargetRequest(
                30,
                new BigDecimal("180"),
                new BigDecimal("80"),
                EnergyEquationSet.MALE,
                ActivityLevel.ACTIVE,
                Goal.MUSCLE_GAIN,
                true,
                4,
                null
        );
        DailyTargetEstimate muscleGain = service.estimate(activeMale);
        assertDecimal("3126.3", muscleGain.maintenanceEnergyKilocalories(), "male active EER");
        assertDecimal("3282.6", muscleGain.planningEnergyKilocalories(), "muscle-gain planning target");
        assertDecimal("5.0", muscleGain.goalAdjustmentPercent(), "muscle-gain heuristic");
        assertDecimal("128.0", muscleGain.proteinGrams(), "resistance-training protein target");
        assertDecimal("1.6", muscleGain.proteinFactorGramsPerKilogram(), "resistance protein factor");
        assertDecimal("46.0", muscleGain.fiberGrams(), "fiber target");
        assertDecimal("1000", muscleGain.micronutrients().get("CALCIUM").amount(), "adult male calcium");
        assertDecimal("11", muscleGain.micronutrients().get("ZINC").amount(), "adult male zinc");
        assertEquals("PRODUCT_HEURISTIC", muscleGain.goalAdjustmentClassification(), "heuristic classification");
        assertTrue(muscleGain.evidence().size() >= 6, "evidence references are attached");

        AdultTargetRequest inactiveFemale = new AdultTargetRequest(
                35,
                new BigDecimal("165"),
                new BigDecimal("60"),
                EnergyEquationSet.FEMALE,
                ActivityLevel.LOW_ACTIVE,
                Goal.FAT_LOSS,
                false,
                0,
                null
        );
        DailyTargetEstimate fatLoss = service.estimate(inactiveFemale);
        assertDecimal("2147.8", fatLoss.maintenanceEnergyKilocalories(), "female low-active EER");
        assertDecimal("1933.0", fatLoss.planningEnergyKilocalories(), "fat-loss planning target");
        assertDecimal("48.0", fatLoss.proteinGrams(), "non-training protein RDA");
        assertDecimal("18", fatLoss.micronutrients().get("IRON").amount(), "adult female iron");
        assertDecimal("320", fatLoss.micronutrients().get("MAGNESIUM").amount(), "female age 31-50 magnesium");
        assertDecimal("310", service.estimate(new AdultTargetRequest(
                25,
                new BigDecimal("165"),
                new BigDecimal("60"),
                EnergyEquationSet.FEMALE,
                ActivityLevel.LOW_ACTIVE,
                Goal.WEIGHT_MAINTENANCE,
                false,
                0,
                BigDecimal.ZERO
        )).micronutrients().get("MAGNESIUM").amount(), "female age 19-30 magnesium");

        DailyTargetEstimate overridden = service.estimate(new AdultTargetRequest(
                30,
                new BigDecimal("180"),
                new BigDecimal("80"),
                EnergyEquationSet.MALE,
                ActivityLevel.ACTIVE,
                Goal.MUSCLE_GAIN,
                true,
                4,
                new BigDecimal("2.5")
        ));
        assertDecimal("2.5", overridden.goalAdjustmentPercent(), "explicit goal adjustment override");
        assertDecimal("3204.5", overridden.planningEnergyKilocalories(), "override planning energy");

        DailyTargetEstimate repeated = service.estimate(activeMale);
        assertEquals(muscleGain, repeated, "target calculation is deterministic");

        assertThrows(
                () -> new AdultTargetRequest(
                        18,
                        new BigDecimal("170"),
                        new BigDecimal("65"),
                        EnergyEquationSet.MALE,
                        ActivityLevel.INACTIVE,
                        Goal.GENERAL_HEALTH,
                        false,
                        0,
                        null
                ),
                "under-19 population is blocked until an age-specific method is implemented"
        );
        assertThrows(
                () -> new AdultTargetRequest(
                        25,
                        new BigDecimal("170"),
                        new BigDecimal("65"),
                        EnergyEquationSet.MALE,
                        ActivityLevel.INACTIVE,
                        Goal.GENERAL_HEALTH,
                        false,
                        0,
                        new BigDecimal("-20")
                ),
                "aggressive override is rejected"
        );
    }

    private void assertDecimal(String expected, BigDecimal actual, String message) {
        assertions++;
        if (new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }

    private void assertEquals(Object expected, Object actual, String message) {
        assertions++;
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }

    private void assertTrue(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private void assertThrows(Runnable action, String message) {
        assertions++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message + ": expected IllegalArgumentException");
    }
}
