package com.academictracker.prediction.result;

import com.academictracker.prediction.model.PassRiskLevel;
import com.academictracker.prediction.model.PredictionMode;
import com.academictracker.prediction.model.PriorityLevel;

import java.util.List;
import java.util.Map;

public record CoursePredictionResult(
        String courseId,
        String courseName,
        String category,
        int credits,
        Boolean majorCourse,
        PredictionMode mode,
        boolean valid,
        List<String> warnings,
        List<String> errors,
        double totalAssignedWeight,
        double completedWeight,
        double missingWeight,
        double unassignedWeight,
        double currentWeightedScore,
        double currentPaceScore,
        double projectedFinalScore,
        GpaResult projectedGrade,
        double requiredScoreForPass,
        double requiredScoreForTarget,
        Map<String, Double> requiredScoreForEachGradeBoundary,
        List<AssessmentPlan> perAssessmentPlan,
        PassRiskLevel passRiskLevel,
        double recommendedTargetScore,
        GpaResult recommendedTargetGrade,
        WhatIfResult conservativeScenario,
        WhatIfResult expectedScenario,
        WhatIfResult ambitiousScenario,
        WhatIfResult customWhatIfScenario,
        PriorityLevel priorityLevel,
        String priorityReason,
        String feasibilityLabel,
        String riskExplanation,
        String profileConfidence,
        double profileExpectedScore,
        String profileExplanation,
        String explanationVi
) {
    public CoursePredictionResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        errors = errors == null ? List.of() : List.copyOf(errors);
        requiredScoreForEachGradeBoundary = requiredScoreForEachGradeBoundary == null
                ? Map.of()
                : Map.copyOf(requiredScoreForEachGradeBoundary);
        perAssessmentPlan = perAssessmentPlan == null ? List.of() : List.copyOf(perAssessmentPlan);
    }

    public record AssessmentPlan(
            String assessmentName,
            double weight,
            double requiredScore,
            double recommendedTarget,
            String explanationVi
    ) {
    }
}
