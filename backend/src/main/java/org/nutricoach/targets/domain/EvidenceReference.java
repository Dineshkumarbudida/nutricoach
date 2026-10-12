package org.nutricoach.targets.domain;

import java.util.Objects;

public record EvidenceReference(
        String id,
        String title,
        String sourceType,
        String url,
        String applicability,
        String lastReviewed
) {
    public EvidenceReference {
        id = requireText(id, "id");
        title = requireText(title, "title");
        sourceType = requireText(sourceType, "sourceType");
        url = requireText(url, "url");
        applicability = requireText(applicability, "applicability");
        lastReviewed = requireText(lastReviewed, "lastReviewed");
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
