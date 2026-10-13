package org.nutricoach.target.domain;

import java.math.BigDecimal;
import java.util.List;

public record TargetEstimate(
        BigDecimal estimatedEnergyKcal,
        BigDecimal planningEnergyKcal,
        BigDecimal proteinTargetGrams,
        BigDecimal proteinLowerGrams,
        BigDecimal proteinUpperGrams,
        BigDecimal fiberTargetGrams,
        String energyMethodVersion,
        String proteinMethodVersion,
        String fiberMethodVersion,
        List<String> evidenceReferences,
        List<String> limitations
) {
    public TargetEstimate {
        evidenceReferences = List.copyOf(evidenceReferences);
        limitations = List.copyOf(limitations);
    }
}
