package org.nutricoach.nutrition.domain;

public enum NutrientUnit {
    KILOCALORIE("kcal"),
    GRAM("g"),
    MILLIGRAM("mg"),
    MICROGRAM("ug");

    private final String symbol;

    NutrientUnit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
