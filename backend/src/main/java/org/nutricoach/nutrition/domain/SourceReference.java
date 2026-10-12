package org.nutricoach.nutrition.domain;

import java.util.Objects;

public record SourceReference(
        String sourceCode,
        String sourceVersion,
        String sourceRecordId,
        String licenseExpression
) {
    public SourceReference {
        sourceCode = requireText(sourceCode, "sourceCode");
        sourceVersion = requireText(sourceVersion, "sourceVersion");
        sourceRecordId = requireText(sourceRecordId, "sourceRecordId");
        licenseExpression = requireText(licenseExpression, "licenseExpression");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }
}
