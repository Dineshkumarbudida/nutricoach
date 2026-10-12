package org.nutricoach.nutrition.domain;

public final class CalculationException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final String code;

    public CalculationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
