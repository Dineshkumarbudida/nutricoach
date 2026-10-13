package org.nutricoach.target;

import org.junit.jupiter.api.Test;
import org.nutricoach.target.application.AdultTargetCalculator;
import org.nutricoach.target.domain.AdultTargetProfile;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdultTargetCalculatorTest {
    @Test
    void adultMaleActiveEnergyMatchesPublishedDriEquation() {
        AdultTargetProfile profile = new AdultTargetProfile(
                30, new BigDecimal("175"), new BigDecimal("70"),
                AdultTargetProfile.Sex.MALE,
                AdultTargetProfile.ActivityCategory.ACTIVE,
                AdultTargetProfile.Goal.MAINTENANCE,
                false
        );

        var result = new AdultTargetCalculator().estimate(profile);

        assertEquals(new BigDecimal("2740"), result.estimatedEnergyKcal());
        assertEquals(new BigDecimal("980"), result.fiberTargetGrams());
    }

    @Test
    void resistanceTrainingProteinUsesEvidenceRange() {
        AdultTargetProfile profile = new AdultTargetProfile(
                30, new BigDecimal("175"), new BigDecimal("75"),
                AdultTargetProfile.Sex.MALE,
                AdultTargetProfile.ActivityCategory.ACTIVE,
                AdultTargetProfile.Goal.MUSCLE_GAIN,
                true
        );

        var result = new AdultTargetCalculator().estimate(profile);

        assertEquals(new BigDecimal("120.0"), result.proteinTargetGrams());
        assertEquals(new BigDecimal("105.0"), result.proteinLowerGrams());
        assertEquals(new BigDecimal("150.0"), result.proteinUpperGrams());
    }
}
