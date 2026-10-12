package org.nutricoach.nutrition.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SourceReferenceRequest(
        @NotBlank String sourceCode,
        @NotBlank String sourceVersion,
        @NotBlank String sourceRecordId,
        @NotBlank String licenseExpression
) {
}
