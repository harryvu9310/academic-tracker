package com.tracker.academictracker.service;

import com.academictracker.prediction.result.GlobalPredictionResult;
import com.academictracker.prediction.result.TargetValidationResult;
import com.tracker.academictracker.model.Student;

import java.util.ArrayList;
import java.util.List;

public class DashboardTargetService {
    private final AcademicPredictionService predictionService;

    public DashboardTargetService() {
        this(new AcademicPredictionService());
    }

    public DashboardTargetService(AcademicPredictionService predictionService) {
        this.predictionService = predictionService;
    }

    public TargetCalculationResult calculate(Student student, String cumulativeRaw, String majorRaw) {
        boolean hasCumulative = cumulativeRaw != null && !cumulativeRaw.trim().isEmpty();
        boolean hasMajor = majorRaw != null && !majorRaw.trim().isEmpty();
        if (!hasCumulative && !hasMajor) {
            return new TargetCalculationResult(
                    "Please enter a Cumulative GPA target, a Major GPA target, or both.",
                    false,
                    false,
                    false,
                    null,
                    null
            );
        }

        List<String> sections = new ArrayList<>();
        boolean allValid = true;
        boolean requiresConfirmation = false;
        GlobalPredictionResult cumulativeResult = null;
        GlobalPredictionResult majorResult = null;

        if (hasCumulative) {
            try {
                double target = InputValidator.parseGpaTarget(cumulativeRaw, "Cumulative GPA target");
                TargetValidationResult validation = predictionService.validateCumulativeTarget(student, target);
                cumulativeResult = predictionService.predictCumulative(student, target);
                sections.add("CUMULATIVE GPA TARGET\n" + predictionService.formatGlobalPrediction(cumulativeResult));
                allValid &= validation.valid();
                requiresConfirmation |= validation.requiresConfirmation();
            } catch (IllegalArgumentException e) {
                allValid = false;
                sections.add("CUMULATIVE GPA TARGET\n" + e.getMessage());
            }
        }

        if (hasMajor) {
            try {
                double target = InputValidator.parseGpaTarget(majorRaw, "Major GPA target");
                TargetValidationResult validation = predictionService.validateMajorTarget(student, target);
                majorResult = predictionService.predictMajor(student, target);
                sections.add("MAJOR GPA TARGET\n" + predictionService.formatGlobalPrediction(majorResult));
                allValid &= validation.valid();
                requiresConfirmation |= validation.requiresConfirmation();
            } catch (IllegalArgumentException e) {
                allValid = false;
                sections.add("MAJOR GPA TARGET\n" + e.getMessage());
            }
        }

        return new TargetCalculationResult(String.join("\n\n", sections), allValid, requiresConfirmation, true, cumulativeResult, majorResult);
    }

    public record TargetCalculationResult(
            String output,
            boolean valid,
            boolean requiresConfirmation,
            boolean hasAnyTarget,
            GlobalPredictionResult cumulativeResult,
            GlobalPredictionResult majorResult
    ) {
    }
}
