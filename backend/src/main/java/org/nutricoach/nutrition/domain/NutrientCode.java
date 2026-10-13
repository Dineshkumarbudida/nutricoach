package org.nutricoach.nutrition.domain;

public enum NutrientCode {
    ENERGY(NutrientUnit.KILOCALORIE), PROTEIN(NutrientUnit.GRAM), CARBOHYDRATE(NutrientUnit.GRAM), FAT(NutrientUnit.GRAM),
    FIBER(NutrientUnit.GRAM), SUGARS(NutrientUnit.GRAM), SATURATED_FAT(NutrientUnit.GRAM), SODIUM(NutrientUnit.MILLIGRAM),
    POTASSIUM(NutrientUnit.MILLIGRAM), CALCIUM(NutrientUnit.MILLIGRAM), IRON(NutrientUnit.MILLIGRAM), MAGNESIUM(NutrientUnit.MILLIGRAM),
    PHOSPHORUS(NutrientUnit.MILLIGRAM), ZINC(NutrientUnit.MILLIGRAM), COPPER(NutrientUnit.MILLIGRAM), MANGANESE(NutrientUnit.MILLIGRAM),
    SELENIUM(NutrientUnit.MICROGRAM), VITAMIN_A_RAE(NutrientUnit.MICROGRAM), VITAMIN_C(NutrientUnit.MILLIGRAM),
    VITAMIN_D(NutrientUnit.MICROGRAM), VITAMIN_E(NutrientUnit.MILLIGRAM), VITAMIN_K(NutrientUnit.MICROGRAM),
    FOLATE(NutrientUnit.MICROGRAM), VITAMIN_B12(NutrientUnit.MICROGRAM), THIAMIN(NutrientUnit.MILLIGRAM),
    RIBOFLAVIN(NutrientUnit.MILLIGRAM), NIACIN(NutrientUnit.MILLIGRAM), VITAMIN_B6(NutrientUnit.MILLIGRAM),
    CHOLINE(NutrientUnit.MILLIGRAM);

    private final NutrientUnit canonicalUnit;

    NutrientCode(NutrientUnit canonicalUnit) { this.canonicalUnit = canonicalUnit; }

    public NutrientUnit canonicalUnit() { return canonicalUnit; }
}
