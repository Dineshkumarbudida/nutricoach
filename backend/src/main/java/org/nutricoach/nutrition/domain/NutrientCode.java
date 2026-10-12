package org.nutricoach.nutrition.domain;

public enum NutrientCode {
    ENERGY(NutrientUnit.KILOCALORIE),
    PROTEIN(NutrientUnit.GRAM),
    CARBOHYDRATE(NutrientUnit.GRAM),
    FAT(NutrientUnit.GRAM),
    FIBER(NutrientUnit.GRAM);

    private final NutrientUnit canonicalUnit;

    NutrientCode(NutrientUnit canonicalUnit) {
        this.canonicalUnit = canonicalUnit;
    }

    public NutrientUnit canonicalUnit() {
        return canonicalUnit;
    }
}
