package org.nutricoach.nutrition.api;

import org.nutricoach.nutrition.api.dto.FoodSnapshotRequest;
import org.nutricoach.nutrition.api.dto.NutrientObservationRequest;
import org.nutricoach.nutrition.api.dto.RecipeIngredientRequest;
import org.nutricoach.nutrition.api.dto.SourceReferenceRequest;
import org.nutricoach.nutrition.domain.FoodComposition;
import org.nutricoach.nutrition.domain.NutrientCode;
import org.nutricoach.nutrition.domain.NutrientObservation;
import org.nutricoach.nutrition.domain.RecipeIngredient;
import org.nutricoach.nutrition.domain.SourceReference;

import java.util.EnumMap;

final class NutritionRequestMapper {
    private NutritionRequestMapper() {
    }

    static FoodComposition toDomain(FoodSnapshotRequest request) {
        EnumMap<NutrientCode, NutrientObservation> observations = new EnumMap<>(NutrientCode.class);
        request.nutrients().forEach((code, observation) ->
                observations.put(code, toDomain(observation)));

        return new FoodComposition(
                request.foodId(),
                request.version(),
                request.displayName(),
                request.referenceMassGrams(),
                observations,
                toDomain(request.source())
        );
    }

    static RecipeIngredient toDomain(RecipeIngredientRequest request) {
        return new RecipeIngredient(
                toDomain(request.food()),
                request.massGrams(),
                request.note()
        );
    }

    private static NutrientObservation toDomain(NutrientObservationRequest request) {
        return new NutrientObservation(
                request.status(),
                request.amount(),
                request.reportingLimit()
        );
    }

    private static SourceReference toDomain(SourceReferenceRequest request) {
        return new SourceReference(
                request.sourceCode(),
                request.sourceVersion(),
                request.sourceRecordId(),
                request.licenseExpression()
        );
    }
}
