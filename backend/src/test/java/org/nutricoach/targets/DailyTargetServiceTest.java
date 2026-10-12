package org.nutricoach.targets;

import org.junit.jupiter.api.Test;
import org.nutricoach.targets.application.DailyTargetService;
import org.nutricoach.targets.domain.ActivityLevel;
import org.nutricoach.targets.domain.AdultTargetRequest;
import org.nutricoach.targets.domain.EnergyEquationSet;
import org.nutricoach.targets.domain.Goal;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DailyTargetServiceTest {
    private final DailyTargetService service = new DailyTargetService();

    @Test
    void calculatesPublishedAdultMaleActiveEquation() {
        var estimate = service.estimate(new AdultTargetRequest(
                30,
                new BigDecimal("180"),
                new BigDecimal("80"),
                EnergyEquationSet.MALE,
                ActivityLevel.ACTIVE,
                Goal.MUSCLE_GAIN,
                true,
                4,
                null
        ));

        assertThat(estimate.maintenanceEnergyKilocalories()).isEqualByComparingTo("3126.3");
        assertThat(estimate.planningEnergyKilocalories()).isEqualByComparingTo("3282.6");
        assertThat(estimate.proteinGrams()).isEqualByComparingTo("128.0");
        assertThat(estimate.fiberGrams()).isEqualByComparingTo("46.0");
        assertThat(estimate.micronutrients()).containsKeys(
                "CALCIUM",
                "IRON",
                "POTASSIUM",
                "VITAMIN_C",
                "VITAMIN_D",
                "FOLATE_DFE",
                "VITAMIN_B12"
        );
    }

    @Test
    void rejectsUnsupportedPopulationAndAggressiveOverride() {
        assertThatThrownBy(() -> new AdultTargetRequest(
                18,
                new BigDecimal("170"),
                new BigDecimal("65"),
                EnergyEquationSet.MALE,
                ActivityLevel.INACTIVE,
                Goal.GENERAL_HEALTH,
                false,
                0,
                null
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new AdultTargetRequest(
                30,
                new BigDecimal("170"),
                new BigDecimal("65"),
                EnergyEquationSet.MALE,
                ActivityLevel.INACTIVE,
                Goal.FAT_LOSS,
                false,
                0,
                new BigDecimal("-20")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
