package org.nutricoach.targets.api;

import jakarta.validation.Valid;
import org.nutricoach.targets.api.dto.AdultTargetEstimateRequest;
import org.nutricoach.targets.application.DailyTargetService;
import org.nutricoach.targets.domain.AdultTargetRequest;
import org.nutricoach.targets.domain.DailyTargetEstimate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/targets")
public class DailyTargetController {
    private final DailyTargetService targetService;

    public DailyTargetController(DailyTargetService targetService) {
        this.targetService = targetService;
    }

    @PostMapping("/estimate-adult")
    DailyTargetEstimate estimateAdultTargets(
            @Valid @RequestBody AdultTargetEstimateRequest request
    ) {
        return targetService.estimate(new AdultTargetRequest(
                request.ageYears(),
                request.heightCentimeters(),
                request.weightKilograms(),
                request.equationSet(),
                request.activityLevel(),
                request.goal(),
                request.resistanceTraining(),
                request.trainingDaysPerWeek(),
                request.goalAdjustmentPercentOverride()
        ));
    }
}
