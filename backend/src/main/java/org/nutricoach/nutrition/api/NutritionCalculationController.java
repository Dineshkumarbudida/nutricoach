package org.nutricoach.nutrition.api;

import jakarta.validation.Valid;
import org.nutricoach.nutrition.api.dto.FoodPortionCalculationRequest;
import org.nutricoach.nutrition.api.dto.RecipeCalculationRequest;
import org.nutricoach.nutrition.application.NutritionCalculationService;
import org.nutricoach.nutrition.domain.NutritionCalculationResult;
import org.nutricoach.nutrition.domain.RecipeCalculationResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class NutritionCalculationController {
    private final NutritionCalculationService calculationService;

    public NutritionCalculationController(NutritionCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @GetMapping("/meta")
    Map<String, Object> metadata() {
        return Map.of(
                "application", "NutriCoach",
                "apiVersion", "v1",
                "milestone", "production-real-data-foundation",
                "authoritativeCalculations", "deterministic-code",
                "calculationContractVersion", NutritionCalculationService.CONTRACT_VERSION,
                "productionNutritionDataBundled", false,
                "nutrients", List.of("ENERGY","PROTEIN","CARBOHYDRATE","FAT","FIBER","SODIUM","POTASSIUM","CALCIUM","IRON","MAGNESIUM",
                        "PHOSPHORUS","ZINC","COPPER","MANGANESE","SELENIUM","VITAMIN_A_RAE","VITAMIN_C","VITAMIN_D","VITAMIN_E",
                        "VITAMIN_K","FOLATE","VITAMIN_B12","THIAMIN","RIBOFLAVIN","NIACIN","VITAMIN_B6","CHOLINE")
        );
    }

    @PostMapping("/calculations/food-portions")
    NutritionCalculationResult calculateFoodPortion(@Valid @RequestBody FoodPortionCalculationRequest request) {
        return calculationService.scaleFood(
                NutritionRequestMapper.toDomain(request.food()),
                request.portionMassGrams()
        );
    }

    @PostMapping("/calculations/recipes")
    RecipeCalculationResult calculateRecipe(@Valid @RequestBody RecipeCalculationRequest request) {
        return calculationService.calculateRecipe(
                request.ingredients().stream().map(NutritionRequestMapper::toDomain).toList(),
                request.cookedBatchMassGrams(),
                request.portionMassGrams()
        );
    }
}
